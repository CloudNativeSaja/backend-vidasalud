# Kafka en VidaSalud

Kafka complementa a RabbitMQ sin reemplazarlo:

| | RabbitMQ | Kafka |
|---|---|---|
| Qué transporta | **Comandos** de trabajo ("manda este correo") | **Eventos** de dominio ("la cita 42 pasó a CONFIRMADA") |
| Consumo | Un consumidor por mensaje; el mensaje se borra al procesarse | Varios grupos leen el mismo evento; el log se conserva 7 a 30 días |
| Quién lo usa | `notify` | `report` y `audit` |

Si Kafka cae, la cita igual se guarda y RabbitMQ sigue funcionando.

## Arquitectura

```
                                    ┌──► ms-vidasalud-report  (grupo "reportes")  ──► KPIs en memoria ──► /api/report/*
ms-vidasalud-appointments ─publica─►│
   (clave = id de la cita)          │   vidasalud.appointments.events (3 particiones x 3 réplicas, 7 días)
                                    └──► ms-vidasalud-audit   (grupo "auditoria") ─publica─┐
                                                                                           ▼
                                        vidasalud.audit.events (3 particiones x 3 réplicas, 30 días)
                                                                                           │
                                    ms-vidasalud-audit (grupo "auditoria-timeline") ◄─────┘──► línea de tiempo ──► /api/audit/*

ms-vidasalud-kafka-admin ──AdminClient──► tópicos, configuraciones, consumer groups, brokers
BFF (/api/bff/report | audit | kafka) ──► los tres servicios, con JWT y roles
```

## Tópicos y su configuración

Se crean con `infra/kafka/compose.yml` (contenedor `kafka-init`); `audit` declara además su tópico y las DLT al arrancar.

| Tópico | Particiones | Réplicas | min.insync | Retención | cleanup.policy | Productor | Consumidores (grupo) |
|---|---|---|---|---|---|---|---|
| `vidasalud.appointments.events` | 3 | 3 | 2 | 7 días | delete | appointments | report (`reportes`), audit (`auditoria`) |
| `vidasalud.audit.events` | 3 | 3 | 2 | 30 días | delete | audit | audit (`auditoria-timeline`) |
| `vidasalud.appointments.events.reportes.DLT` | 3 | 3 | – | 14 días | delete | report | (inspección manual) |
| `vidasalud.appointments.events.auditoria.DLT` | 3 | 3 | – | 14 días | delete | audit | (inspección manual) |
| `vidasalud.audit.events.auditoria-timeline.DLT` | 3 | 3 | – | 14 días | delete | audit | (inspección manual) |

- **Clave del mensaje = id de la cita**: todos los eventos de una cita caen en la misma partición y se procesan en orden.
- Retención y política se pueden cambiar en caliente con `PUT /api/kafka/topics/{name}/config`.

## Consumer groups

- **`reportes`** y **`auditoria`** leen el mismo tópico de citas de forma independiente: cada uno tiene sus propios offsets.
- Dentro de un grupo, Kafka reparte las particiones entre las instancias (`concurrency: 3` = un hilo por partición). Si se levanta una segunda instancia del servicio con el mismo grupo, se reparten la carga; si una cae, la otra toma sus particiones (`CooperativeStickyAssignor`, rebalanceo incremental).
- **El estado vive en memoria y se reconstruye releyendo el log.** `report` (grupo `reportes`) y la línea de tiempo de `audit` (grupo `auditoria-timeline`) hacen *seek al inicio* cada vez que se les asignan particiones (`AbstractConsumerSeekAware`): tras reiniciar el servicio, o si Kafka reasigna particiones, releen el tópico y los KPIs y la auditoría vuelven a estar completos. Por eso no hace falta base de datos. Repetir eventos es seguro: ambos descartan duplicados por `eventId`.
- El grupo `auditoria` (que **publica** en Kafka) no relee al reiniciar: continúa desde su último offset, para no duplicar eventos de auditoría.
- Límite conocido: con varias instancias del mismo servicio, cada una conoce solo las particiones que le tocaron. Para esta entrega corre una instancia de cada uno.

## Manejo de errores

Se configura en `KafkaConsumerConfig` / `KafkaConfig` (separado de la lógica de negocio):

1. **Validación**: el listener valida el DTO con Jakarta Validation. Un evento inválido lanza `InvalidEventException`, que **no se reintenta** y va directo a la DLT.
2. **JSON corrupto**: `ErrorHandlingDeserializer` evita que el consumidor se caiga; el mensaje original (bytes) va a la DLT.
3. **Errores transitorios**: hasta 3 reintentos con espera exponencial (1 s, 2 s, 4 s).
4. **DLT**: el mensaje se publica con cabeceras que explican la causa (`kafka_dlt-exception-message`, tópico, partición y offset de origen) y el consumidor sigue con el siguiente evento.
5. **Duplicados**: `KpiService` y `AuditTimelineService` descartan por `eventId`; el id de auditoría se deriva del evento de origen.
6. **Productor** (`appointments`): `acks=all`, idempotencia, `max.block.ms=1000`; el envío es asíncrono y nunca propaga errores al servicio de citas.

## API (a través del BFF)

| Endpoint | Roles | Descripción |
|---|---|---|
| `GET /api/bff/report/kpis?range=last24h\|last7d\|last30d` | ADMIN | Total, por estado, tasa de cancelación, espera promedio |
| `GET /api/bff/report/top-services?range=...` | ADMIN | Prestaciones más solicitadas |
| `GET /api/bff/audit/events?categoria=CITAS&limit=100` | ADMIN, AUDITOR | Línea de tiempo de auditoría |
| `GET /api/bff/kafka/cluster` | ADMIN | Brokers y controlador |
| `GET/POST /api/bff/kafka/topics` | ADMIN | Listar y crear tópicos |
| `GET/DELETE /api/bff/kafka/topics/{name}` | ADMIN | Detalle (particiones, líder, ISR, configuración) y eliminación |
| `PUT /api/bff/kafka/topics/{name}/config` | ADMIN | Cambiar retención, cleanup.policy, min.insync.replicas, etc. |
| `PATCH /api/bff/kafka/topics/{name}/partitions` | ADMIN | Aumentar particiones (nunca reducir) |
| `GET /api/bff/kafka/consumer-groups` | ADMIN | Listar grupos |
| `GET/DELETE /api/bff/kafka/consumer-groups/{id}` | ADMIN | Miembros, offsets y **lag**; eliminar solo si está vacío |

Reglas del administrador: solo se pueden modificar las configuraciones listadas en `vidasalud.kafka-admin.editable-configs`; los tópicos `vidasalud.appointments.events` y `vidasalud.audit.events` no se pueden eliminar (409).

## Levantar todo en local

```powershell
# 1. Kafka (3 ZooKeeper + 3 brokers + Kafka UI). KAFKA_HOST hace que los brokers se anuncien
#    de forma alcanzable desde los contenedores del backend.
cd infra\kafka
$env:KAFKA_HOST = "host.docker.internal"
docker compose up -d
docker compose ps          # todo en estado running; kafka-init termina solo (Exited 0)

# 2. Backend con Kafka activado. Los microservicios son repos aparte: se clonan en .\servicios
cd ..\..
.\deploy\servicios.ps1
#    en .env: STREAMING_ENABLED=true  (KAFKA_BOOTSTRAP_SERVERS ya apunta a host.docker.internal)
docker compose up -d --build
docker compose ps          # todo healthy
```

- Kafka UI: <http://localhost:8090> (puerto configurable con `KAFKA_UI_PORT`; el 8080 es del BFF).
- Verificación rápida: `deploy\verificar-kafka.ps1` (cluster, tópicos, grupos con lag, KPIs y línea de tiempo).

## Guion de demostración

1. **Infraestructura**: `docker compose ps` en `infra/kafka` y en el backend; abrir Kafka UI y mostrar 3 brokers y los tópicos con 3 particiones x 3 réplicas.
2. **Productor**: crear una cita en el front y cambiar su estado; en Kafka UI → *Topics → vidasalud.appointments.events → Messages* ver el evento con clave `id de la cita`.
3. **Consumer groups**: en Kafka UI → *Consumers* ver `reportes`, `auditoria` y `auditoria-timeline` con lag 0; mostrar el reparto de particiones.
4. **Resultado**: en el front, Reportes (KPIs) y Auditoría (línea de tiempo) alimentados por Kafka.
5. **Errores**: publicar un mensaje inválido desde Kafka UI (*Produce Message* con `{"eventId":"x"}`) y mostrarlo en `vidasalud.appointments.events.reportes.DLT` con la causa en las cabeceras.
6. **Administración**: por la API del BFF (o `curl` directo a `:8088`) crear un tópico, cambiar su retención, aumentar particiones, ver el lag de un grupo e intentar borrar un tópico protegido (409).
7. **Resiliencia** (opcional): `docker stop kafka-2` y crear otra cita: sigue funcionando (3 réplicas, `min.insync.replicas=2`).
