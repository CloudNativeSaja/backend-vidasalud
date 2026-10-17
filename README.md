# backend-vidasalud

Backend de VidaSalud (DSY1107 Desarrollo Cloud Native I): microservicios Spring Boot 4 / Java 17.

| Módulo | Puerto | Responsabilidad |
|---|---|---|
| `ms-vidasalud-bff` | 8080 | Backend for Frontend. Valida el JWT de Entra ID y autoriza por rol |
| `ms-vidasalud-appointments` | 8081 | Atenciones y sus estados. **Productor RabbitMQ** |
| `ms-vidasalud-catalog` | 8082 | Prestaciones, boxes y cupos |
| `ms-vidasalud-notify` | 8084 | **Consumidor RabbitMQ**: email, admisión y documentos, con ACK y DLQ |
| `ms-vidasalud-mq-admin` | 8085 | **Administrador RabbitMQ**: API REST de colas, exchanges y bindings |
| `infra/mq` | 5672/5673, 15672/15673 | Clúster RabbitMQ de 2 nodos (`docker compose`) |

- Documentación general: [DOCUMENTACION_BACKEND.md](DOCUMENTACION_BACKEND.md)
- Mensajería (topología, ACK/DLQ, API del administrador, despliegue y guion de demo): [docs/MENSAJERIA.md](docs/MENSAJERIA.md)
