# Mensajería asíncrona VidaSalud (RabbitMQ)

Evaluación Parcial 2 — DSY1107 Desarrollo Cloud Native I.

La capa de mensajería desacopla las tareas lentas o externas (emails, tickets de admisión, generación de PDF) del flujo principal de las atenciones. Cuando una atención cambia de estado, `ms-vidasalud-appointments` publica un comando y responde de inmediato al usuario. `ms-vidasalud-notify` lo procesa en segundo plano. Si RabbitMQ no está disponible, la atención igual se guarda: la mensajería no afecta la lógica existente.

## 1. Componentes

| Componente | Puerto | Rol |
|---|---|---|
| `ms-vidasalud-appointments` | 8081 | **Productor**: publica un comando por cada evento de la atención |
| `ms-vidasalud-notify` | 8084 | **Consumidor**: listeners de email, admisión y documentos, con ACK manual, reintentos y DLQ |
| `ms-vidasalud-mq-admin` | 8085 | **Administrador**: API REST para crear y eliminar colas, exchanges y bindings |
| `ms-vidasalud-bff` | 8080 | Expone el administrador en `/api/bff/mq/**`, solo para el rol `ADMIN` |
| `rabbitmq-1` / `rabbitmq-2` | 5672/15672 y 5673/15673 | Clúster RabbitMQ de 2 nodos (`infra/mq/compose.yml`) |

```
Angular ─► API Gateway (JWT) ─► BFF ─► appointments ──publica──► RabbitMQ (cluster 2 nodos) ──► notify
                                  └──(ADMIN)──► mq-admin ──AMQP / HTTP──┘
```

## 2. Topología (3 colas + 3 DLQ)

Todos los nombres están centralizados en `application.yaml`, bajo el prefijo `vidasalud.messaging`, que es el mismo en todos los microservicios. Se leen con `@ConfigurationProperties` (`MessagingProperties`); ninguna clase de negocio contiene nombres escritos a mano.

**Exchanges**

| Exchange | Tipo | Uso |
|---|---|---|
| `cmd.direct` | direct | Comandos con routing key exacta |
| `cmd.topic` | topic | Comandos por patrón (`*` = 1 palabra, `#` = 0 o más) |
| `cmd.dead.dlx` | direct | Dead Letter Exchange: recibe los mensajes rechazados |

**Colas y bindings**

| Cola | Propósito | Binding direct | Binding topic | DLQ |
|---|---|---|---|---|
| `q.cmd.email` | Email/push al paciente | `email.send` | `email.*` | `q.cmd.email.dlq` |
| `q.cmd.admission` | Ticket de admisión / llamado a box | `admission.ticket` | `admission.#` | `q.cmd.admission.dlq` |
| `q.cmd.record` | Comprobante / resumen PDF | `record.gen` | `record.*` | `q.cmd.record.dlq` |

- Cada cola principal declara `x-dead-letter-exchange = cmd.dead.dlx` y `x-dead-letter-routing-key = <nombre de su DLQ>`.
- Cada DLQ está enlazada a `cmd.dead.dlx` con su propio nombre como routing key.
- **Política de retención:** las DLQ conservan los mensajes 7 días (`x-message-ttl = 604800000`).
- **Durabilidad:** exchanges y colas durables, y mensajes persistentes (delivery mode 2).
- **Dónde se declaran:** en `ms-vidasalud-notify`, clase `config/RabbitTopologyConfig`, con un bean `Queue`, uno `Binding` direct, uno `Binding` topic y uno `Binding` DLQ por cada caso de uso. El productor declara solo los exchanges donde publica.

## 3. Comandos por evento de la atención

| Evento | Comando (`type`) | Exchange | Routing key | Cola |
|---|---|---|---|---|
| Atención creada | `EMAIL_SOLICITUD_RECIBIDA` | cmd.topic | `email.requested` | q.cmd.email |
| → CONFIRMADA | `EMAIL_CITA_CONFIRMADA` | cmd.direct | `email.send` | q.cmd.email |
| | `RECORD_COMPROBANTE` | cmd.direct | `record.gen` | q.cmd.record |
| → EN_ESPERA | `ADMISSION_TICKET` | cmd.direct | `admission.ticket` | q.cmd.admission |
| → EN_ATENCION | `ADMISSION_LLAMADO_BOX` | cmd.topic | `admission.call.box` | q.cmd.admission |
| | `EMAIL_LLAMADO_BOX` | cmd.topic | `email.called` | q.cmd.email |
| → CERRADA | `EMAIL_ATENCION_CERRADA` | cmd.topic | `email.closed` | q.cmd.email |
| | `RECORD_RESUMEN` | cmd.topic | `record.summary` | q.cmd.record |
| → CANCELADA | `EMAIL_CITA_CANCELADA` | cmd.topic | `email.cancelled` | q.cmd.email |

**Envelope común** (JSON):

```json
{
  "eventId": "uuid unico (idempotencia)",
  "type": "EMAIL_CITA_CONFIRMADA",
  "timestamp": "2026-10-17T15:00:00Z",
  "traceId": "uuid de la operacion",
  "correlationId": "appointment-15",
  "source": "ms-vidasalud-appointments",
  "payload": { "appointmentId": "15", "patientId": "...", "serviceId": "1", "boxId": "2",
               "appointmentDate": "...", "status": "CONFIRMADA", "previousStatus": "SOLICITADA" }
}
```

**Garantías del productor:** publisher confirms (`publisher-confirm-type: correlated`) y returns (`mandatory=true`). El log `[PUBLISHER]` muestra si el broker confirmó cada mensaje o si algún mensaje no encontró cola.

## 4. Consumidores: ACK y manejo de errores

Los consumidores usan ACK manual (`acknowledge-mode: manual`) y `prefetch: 1` para un reparto justo entre consumidores. Hay un listener `@RabbitListener` por dominio funcional:

| Paquete | Listener | Servicio de negocio |
|---|---|---|
| `notify.email` | `EmailCommandListener` | `EmailNotificationService` |
| `notify.admission` | `AdmissionCommandListener` | `AdmissionTicketService` |
| `notify.record` | `RecordCommandListener` | `RecordGenerationService` |

La decisión de ACK, reintento o NACK está centralizada en `messaging/MessageAckHandler`:

| Situación | Decisión | Resultado |
|---|---|---|
| Procesado OK | `basicAck` | Se elimina de la cola |
| `eventId` ya procesado | `basicAck` sin reprocesar | Idempotencia |
| JSON inválido o sin `eventId`/`type` | `basicNack(requeue=false)` | DLQ inmediato |
| `InvalidCommandException` (dato inválido) | `basicNack(requeue=false)` | DLQ inmediato |
| `RecoverableProcessingException` con intento < 3 | Espera de 1 s × intento, republica en la misma cola con `x-retry-count + 1` y hace `basicAck` del original | Reintento |
| `RecoverableProcessingException` en el intento 3 | `basicNack(requeue=false)` | DLQ |
| Error inesperado | `basicNack(requeue=false)` | DLQ |

Nunca se usa `requeue=true`, para que un mensaje defectuoso no quede en un loop infinito. Cada NACK queda en el log con su `eventId`, tipo, motivo y número de intentos, y RabbitMQ agrega el header `x-death` al mensaje que llega a la DLQ.

### Fallos simulados para la demo

Los activa `vidasalud.notify.simulation.enabled`. Se usan creando una atención cuyo `patientId` contenga el marcador:

- `FALLA-TEMPORAL`: error recuperable. Se reintenta 3 veces y termina en la DLQ.
- `FALLA-PERMANENTE`: error no recuperable. Va directo a la DLQ.

## 5. API del administrador (`ms-vidasalud-mq-admin`)

La URL base es `http://<host>:8085/api/mq`. Por el BFF, con un JWT de rol ADMIN, es `https://<api-gateway>/api/bff/mq`.

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/queues` | Lista las colas con mensajes, consumidores y nodo | 200 |
| GET | `/queues/{name}` | Detalle de una cola | 200, 404 |
| POST | `/queues` | Crea una cola (opcional: DLQ, TTL, largo máximo, quorum) | 201, 400, 404, 409 |
| DELETE | `/queues/{name}?ifEmpty=true` | Elimina una cola | 204, 404, 409 (protegida o con mensajes) |
| POST | `/queues/{name}/purge` | Vacía una cola | 200, 404 |
| GET | `/exchanges` | Lista los exchanges | 200 |
| POST | `/exchanges` | Crea un exchange direct, topic, fanout o headers | 201, 400, 409 |
| DELETE | `/exchanges/{name}` | Elimina un exchange | 204, 404, 409 |
| GET | `/bindings` | Lista los bindings exchange → cola | 200 |
| POST | `/bindings` | Crea un binding | 201, 400, 404, 409 |
| DELETE | `/bindings?exchange=&queue=&routingKey=` | Elimina un binding | 204, 404, 409 |
| POST | `/messages` | Publica un comando de prueba | 202, 400, 404 |
| GET | `/cluster/nodes` | Nodos del clúster y su estado | 200 |

- **Validaciones:** los DTO usan Jakarta Validation. Los nombres deben tener 3 a 120 caracteres, solo `[A-Za-z0-9._-]` y sin el prefijo reservado `amq.`. El tipo de cola es `classic` o `quorum`; el TTL y el largo máximo deben ser mayores que 0. Una DLQ exige un DLX, y una cola quorum debe ser durable. Los errores responden JSON con `message` y `details`.
- **Protección:** la topología base (`cmd.*` y `q.cmd.*`) no se puede eliminar desde la API y responde 409.
- **Diseño:** toda la lógica vive en `service/RabbitAdminService`. Las escrituras usan `AmqpAdmin` y las lecturas, la API HTTP de RabbitMQ Management. Los controladores no importan clases de RabbitMQ.

Ejemplos (PowerShell usa `curl.exe`):

```bash
# Crear una cola con su DLQ
curl -X POST http://localhost:8085/api/mq/queues -H "Content-Type: application/json" \
  -d '{"name":"q.cmd.sms","deadLetterExchange":"cmd.dead.dlx","withDeadLetterQueue":true}'

# Enlazarla al exchange topic
curl -X POST http://localhost:8085/api/mq/bindings -H "Content-Type: application/json" \
  -d '{"exchange":"cmd.topic","queue":"q.cmd.sms","routingKey":"sms.*"}'

# Publicar un comando de prueba que falla y termina en la DLQ
curl -X POST http://localhost:8085/api/mq/messages -H "Content-Type: application/json" \
  -d '{"exchange":"cmd.topic","routingKey":"email.prueba","type":"EMAIL_PRUEBA","payload":{"appointmentId":"99","patientId":"FALLA-TEMPORAL"}}'

# Validacion: nombre vacio -> 400
curl -X POST http://localhost:8085/api/mq/queues -H "Content-Type: application/json" -d '{"name":""}'

# Eliminar la cola de prueba y su DLQ
curl -X DELETE http://localhost:8085/api/mq/queues/q.cmd.sms
curl -X DELETE http://localhost:8085/api/mq/queues/q.cmd.sms.dlq
```

## 6. Ejecución

### 6.1 Local (Windows con Docker Desktop)

```powershell
cd infra\mq
copy .env.example .env
docker compose up -d
docker compose ps
docker exec rabbitmq-1 rabbitmqctl cluster_status
```

La UI queda en http://localhost:15672 (usuario y clave del `.env`). Después se levantan los servicios, cada uno en su terminal:

```powershell
$env:RABBITMQ_USER="vidasalud"; $env:RABBITMQ_PASSWORD="<clave del .env>"
cd ms-vidasalud-notify;       .\mvnw.cmd spring-boot:run
cd ms-vidasalud-mq-admin;     .\mvnw.cmd spring-boot:run
cd ms-vidasalud-appointments; .\mvnw.cmd spring-boot:run
```

### 6.2 AWS EC2 (`ec2-mq`)

1. Instalar Docker en la instancia (Amazon Linux 2023):

   ```bash
   sudo dnf install -y docker git
   sudo systemctl enable --now docker
   sudo usermod -aG docker ec2-user
   sudo mkdir -p /usr/local/lib/docker/cli-plugins
   sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 -o /usr/local/lib/docker/cli-plugins/docker-compose
   sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
   ```

   Cierra la sesión SSH y vuelve a entrar para que aplique el grupo `docker`.

2. Clonar el repo, entrar a `infra/mq`, crear el `.env` y ejecutar `docker compose up -d`.

3. Security Group de la instancia:

   | Puerto | Origen | Uso |
   |---|---|---|
   | 5672, 5673 | IP o SG del backend | AMQP |
   | 15672, 15673 | Tu IP | Management UI |
   | 4369, 25672 | No se abren | Los nodos se comunican por la red interna de Docker |

4. En la EC2 del backend, antes de `java -jar`:

   ```bash
   export RABBITMQ_ADDRESSES=<IP-privada-ec2-mq>:5672,<IP-privada-ec2-mq>:5673
   export RABBITMQ_USER=vidasalud RABBITMQ_PASSWORD=<clave>
   export RABBITMQ_MANAGEMENT_URL=http://<IP-privada-ec2-mq>:15672   # solo para mq-admin
   ```

Si RabbitMQ corre en la misma EC2 que el backend, los valores por defecto (`localhost:5672,localhost:5673`) funcionan sin configurar nada.

### 6.3 Si el nodo 2 no se une al clúster

```bash
docker exec rabbitmq-2 rabbitmqctl stop_app
docker exec rabbitmq-2 rabbitmqctl reset
docker exec rabbitmq-2 rabbitmqctl join_cluster rabbit@rabbitmq-1
docker exec rabbitmq-2 rabbitmqctl start_app
docker exec rabbitmq-1 rabbitmqctl cluster_status
```

## 7. Guion sugerido para el video (EP4)

1. `docker compose ps`: los 2 contenedores `healthy`. `rabbitmqctl cluster_status`: 2 nodos en *Running Nodes*.
2. En la UI de RabbitMQ:
   - *Exchanges*: `cmd.direct` (direct), `cmd.topic` (topic) y `cmd.dead.dlx`.
   - *Queues*: las 6 colas.
   - En `q.cmd.email`, los argumentos DLX y sus bindings.
3. Logs en vivo de notify y appointments:
   - Crear una atención y luego confirmarla, poner en espera, llamar a box y cerrar. En el log aparecen `[PUBLISHER] Publicado` y `[EMAIL] ... ACK`.
4. Fallos controlados:
   - Una atención con paciente `FALLA-TEMPORAL` muestra 3 intentos y luego `NACK -> DLQ`. En la UI, `q.cmd.email.dlq` tiene el mensaje con su header `x-death`.
   - Con `FALLA-PERMANENTE`, el mensaje va directo a la DLQ.
5. Administrador:
   - `GET /cluster/nodes`.
   - Crear una cola con DLQ y un binding, publicar un mensaje de prueba y eliminar la cola.
   - Validación: nombre vacío devuelve 400. Intentar borrar `q.cmd.email` devuelve 409.
6. Mostrar el código:
   - `application.yaml` (nombres centralizados).
   - `RabbitTopologyConfig` (beans).
   - Un listener y `MessageAckHandler` (tabla de decisiones).
   - `RabbitAdminService`.
