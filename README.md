# backend-vidasalud

Repositorio de **infraestructura y orquestación** del backend de VidaSalud (DSY1107 Desarrollo Cloud Native I). Cada microservicio Spring Boot 4 / Java 17 vive en **su propio repositorio**; aquí están los `docker-compose`, los scripts de despliegue y la documentación.

## Microservicios (un repositorio cada uno)

| Repositorio | Puerto | Responsabilidad |
|---|---|---|
| [`ms-vidasalud-bff`](https://github.com/CloudNativeSaja/ms-vidasalud-bff) | 8080 | Backend for Frontend. Valida el JWT de Entra ID y autoriza por rol |
| [`ms-vidasalud-appointments`](https://github.com/CloudNativeSaja/ms-vidasalud-appointments) | 8081 | Atenciones y sus estados. **Productor** de RabbitMQ y de Kafka |
| [`ms-vidasalud-catalog`](https://github.com/CloudNativeSaja/ms-vidasalud-catalog) | 8082 | Prestaciones, boxes y cupos |
| [`ms-vidasalud-notify`](https://github.com/CloudNativeSaja/ms-vidasalud-notify) | 8084 | **Consumidor RabbitMQ**: email, admisión y documentos, con ACK y DLQ |
| [`ms-vidasalud-mq-admin`](https://github.com/CloudNativeSaja/ms-vidasalud-mq-admin) | 8085 | **Administrador RabbitMQ**: API REST de colas, exchanges y bindings |
| [`ms-vidasalud-report`](https://github.com/CloudNativeSaja/ms-vidasalud-report) | 8086 | **Consumidor Kafka** (grupo `reportes`): KPIs de las atenciones |
| [`ms-vidasalud-audit`](https://github.com/CloudNativeSaja/ms-vidasalud-audit) | 8087 | **Consumidor y productor Kafka**: línea de tiempo de auditoría |
| [`ms-vidasalud-kafka-admin`](https://github.com/CloudNativeSaja/ms-vidasalud-kafka-admin) | 8088 | **Administrador Kafka**: tópicos, configuraciones y consumer groups |

El frontend está en [`frontend-vidasalud`](https://github.com/CloudNativeSaja/frontend-vidasalud).

## Qué hay en este repositorio

| Ruta | Contenido |
|---|---|
| `docker-compose.yml` | BFF, appointments, catalog, notify, mq-admin, report, audit, kafka-admin y MariaDB |
| `infra/mq` | Clúster RabbitMQ de 2 nodos (`docker compose`) |
| `infra/kafka` | Clúster Kafka: 3 ZooKeeper, 3 brokers y Kafka UI (`docker compose`) |
| `deploy/` | `servicios.sh` / `servicios.ps1` (clonan los microservicios), `deploy.sh` (redespliegue en la EC2) y `verificar-kafka.ps1` |
| `docs/` | Mensajería (RabbitMQ) y Kafka |

## Levantar el backend

El compose construye cada imagen desde `./servicios/<microservicio>`, una carpeta que **no se versiona**: la crean los scripts clonando los repositorios.

```powershell
cp .env.example .env              # y editar las contraseñas
.\deploy\servicios.ps1            # clona o actualiza los 8 microservicios (en Linux: ./deploy/servicios.sh)
docker compose up -d --build
docker compose ps
```

- En la EC2 basta `./deploy/deploy.sh`: hace `git pull`, actualiza los servicios y reconstruye.
- Para usar otra rama de los servicios: `.\deploy\servicios.ps1 -Ref mi-rama` (o `SERVICIOS_REF=mi-rama ./deploy/servicios.sh`).
- Para desarrollar un servicio, se trabaja en **su repositorio** (`servicios/<nombre>` es un clon normal de git); este repositorio no contiene su código.

## Documentación

- General: [DOCUMENTACION_BACKEND.md](DOCUMENTACION_BACKEND.md)
- Mensajería con RabbitMQ (topología, ACK/DLQ, API del administrador, despliegue y guion de demo): [docs/MENSAJERIA.md](docs/MENSAJERIA.md)
- Streaming con Kafka (tópicos, consumer groups, errores, API y guion de demo): [docs/KAFKA.md](docs/KAFKA.md)
