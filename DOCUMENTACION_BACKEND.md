# Documentacion del backend VidaSalud

## 1. Resumen

El backend de VidaSalud esta compuesto por tres aplicaciones Spring Boot independientes:

| Aplicacion | Responsabilidad | Puerto local | Base de datos |
|---|---|---:|---|
| `ms-vidasalud-bff` | Punto de entrada para el frontend, autenticacion JWT y enrutamiento | `8080` | No usa base de datos |
| `ms-vidasalud-appointments` | Gestion de citas y sus estados | `8081` | `vidasalud_appointments` |
| `ms-vidasalud-catalog` | Gestion de prestaciones, boxes clinicos y cupos | `8082` | `vidasalud_catalog` |

Los tres modulos usan Java 17 y Spring Boot 4.0.8. El BFF se comunica con los servicios internos mediante `RestClient` y URLs configurables.

## 2. Arquitectura

```text
Frontend Angular (localhost:4200)
              |
              | HTTP + Bearer JWT
              v
BFF VidaSalud (8080)
  |-- /api/bff/appointments --> ms-vidasalud-appointments (8081) --> MySQL vidasalud_appointments
  `-- /api/bff/catalog      --> ms-vidasalud-catalog (8082)      --> MySQL vidasalud_catalog
```

El BFF transforma las rutas publicas `/api/bff/...` en llamadas internas:

- `/api/bff/appointments` -> `/api/appointments`
- `/api/bff/catalog/services` -> `/api/catalog/services`
- `/api/bff/catalog/boxes` -> `/api/catalog/boxes`
- `/api/bff/catalog/slots` -> `/api/catalog/slots`

## 3. Requisitos

- Java 17 o superior.
- Maven Wrapper incluido en cada servicio (`mvnw.cmd` para Windows).
- MySQL disponible para los servicios de citas y catalogo.
- Bases de datos creadas:
  - `vidasalud_appointments`
  - `vidasalud_catalog`
- Un tenant y una aplicacion configurada en Azure Entra ID para emitir JWT compatibles con el BFF.

## 4. Puesta en marcha local

### 4.1 Configurar MySQL

Los servicios usan `spring.jpa.hibernate.ddl-auto=update`, por lo que Hibernate actualiza las tablas al iniciar. Se recomienda crear previamente las bases de datos y utilizar un usuario con permisos sobre ellas.

```sql
CREATE DATABASE vidasalud_appointments;
CREATE DATABASE vidasalud_catalog;
```

### 4.2 Iniciar los servicios

Ejecutar cada comando desde la carpeta del modulo correspondiente:

```powershell
cd vidaSaludBack\ms-vidasalud-appointments
.\mvnw.cmd spring-boot:run
```

```powershell
cd vidaSaludBack\ms-vidasalud-catalog
.\mvnw.cmd spring-boot:run
```

```powershell
cd vidaSaludBack\ms-vidasalud-bff
.\mvnw.cmd spring-boot:run
```

Orden recomendado: bases de datos, `appointments` y `catalog`, y finalmente el BFF.

### 4.3 Compilar y probar

```powershell
.\mvnw.cmd clean verify
```

El comando debe ejecutarse dentro de cada microservicio. Cada modulo contiene una prueba de contexto Spring (`...ApplicationTests`).

## 5. Variables de entorno

### BFF

| Variable | Valor por defecto | Uso |
|---|---|---|
| `BFF_PORT` | `8080` | Puerto del BFF |
| `AZURE_ISSUER` | `https://login.microsoftonline.com/9856cc68-d10d-4037-8c16-eef696ff0bbe/v2.0` | Emisor esperado del JWT |
| `AZURE_AUDIENCE` | `87836a5b-acbe-48cb-8154-37e25612d06b` | Audience esperado del JWT |
| `APPOINTMENTS_URL` | `http://127.0.0.1:8081` | URL del servicio de citas |
| `CATALOG_URL` | `http://127.0.0.1:8082` | URL del servicio de catalogo |

### Servicios de dominio

| Variable | Valor por defecto | Uso |
|---|---|---|
| `APPOINTMENTS_PORT` | `8081` | Puerto de citas |
| `CATALOG_PORT` | `8082` | Puerto de catalogo |
| `APPOINTMENTS_DB_URL` | `jdbc:mysql://127.0.0.1:3306/vidasalud_appointments?...` | Conexion de citas |
| `CATALOG_DB_URL` | `jdbc:mysql://127.0.0.1:3306/vidasalud_catalog?...` | Conexion de catalogo |
| `DB_USERNAME` | `root` | Usuario MySQL compartido por defecto |
| `DB_PASSWORD` | `123456` | Password MySQL compartido por defecto |

En ambientes reales se deben reemplazar los valores por defecto, especialmente las credenciales de base de datos.

## 6. API publica del BFF

Todas las rutas del BFF requieren autenticacion, excepto `GET /actuator/health` y las solicitudes `OPTIONS`. El token se envia de la siguiente forma:

```http
Authorization: Bearer <JWT>
```

Los roles se leen desde el claim `roles` del token y se convierten a autoridades Spring con el prefijo `ROLE_`.

### 6.1 Citas: `/api/bff/appointments`

| Metodo | Ruta | Roles | Resultado |
|---|---|---|---|
| `GET` | `/api/bff/appointments` | `ADMIN`, `RECEPCIONISTA`, `AUDITOR` | Lista de citas |
| `GET` | `/api/bff/appointments/{id}` | `ADMIN`, `PACIENTE`, `RECEPCIONISTA`, `AUDITOR` | Cita por ID |
| `POST` | `/api/bff/appointments` | `ADMIN`, `PACIENTE`, `RECEPCIONISTA` | Crea una cita |
| `PUT` | `/api/bff/appointments/{id}` | `ADMIN`, `RECEPCIONISTA` | Actualiza datos |
| `PUT` | `/api/bff/appointments/{id}/status` | `ADMIN`, `RECEPCIONISTA` | Cambia estado |
| `DELETE` | `/api/bff/appointments/{id}` | `ADMIN`, `RECEPCIONISTA` | Elimina una cita |

El listado del BFF no expone filtros; la API interna de citas si los soporta.

### 6.2 Catalogo: `/api/bff/catalog`

Las rutas de catalogo requieren un JWT valido, pero actualmente no tienen restricciones de rol con `@PreAuthorize`.

#### Prestaciones

| Metodo | Ruta | Resultado |
|---|---|---|
| `GET` | `/api/bff/catalog/services` | Lista prestaciones |
| `GET` | `/api/bff/catalog/services/{id}` | Prestacion por ID |
| `POST` | `/api/bff/catalog/services` | Crea prestacion |
| `PUT` | `/api/bff/catalog/services/{id}` | Actualiza prestacion |
| `DELETE` | `/api/bff/catalog/services/{id}` | Elimina prestacion |

#### Boxes clinicos

| Metodo | Ruta | Resultado |
|---|---|---|
| `GET` | `/api/bff/catalog/boxes` | Lista boxes |
| `GET` | `/api/bff/catalog/boxes/{id}` | Box por ID |
| `POST` | `/api/bff/catalog/boxes` | Crea box |
| `PUT` | `/api/bff/catalog/boxes/{id}` | Actualiza box |
| `DELETE` | `/api/bff/catalog/boxes/{id}` | Elimina box |

#### Cupos

| Metodo | Ruta | Resultado |
|---|---|---|
| `GET` | `/api/bff/catalog/slots` | Lista cupos |
| `GET` | `/api/bff/catalog/slots/{id}` | Cupo por ID |
| `POST` | `/api/bff/catalog/slots` | Crea cupo |
| `PUT` | `/api/bff/catalog/slots/{id}` | Actualiza cupo |
| `PUT` | `/api/bff/catalog/slots/{id}/reserve` | Reserva cupo |
| `PUT` | `/api/bff/catalog/slots/{id}/release` | Libera cupo |
| `DELETE` | `/api/bff/catalog/slots/{id}` | Elimina cupo |

## 7. API interna de appointments

Base URL local: `http://localhost:8081`.

### Endpoints

| Metodo | Ruta | Descripcion |
|---|---|---|
| `POST` | `/api/appointments` | Crea una cita con estado inicial `SOLICITADA` |
| `GET` | `/api/appointments` | Lista todas las citas |
| `GET` | `/api/appointments/{id}` | Busca una cita |
| `PUT` | `/api/appointments/{id}` | Modifica prestacion, box y fecha |
| `PUT` | `/api/appointments/{id}/status` | Cambia el estado |
| `DELETE` | `/api/appointments/{id}` | Elimina la cita |

Filtros disponibles en `GET /api/appointments`:

- `status`: `SOLICITADA`, `CONFIRMADA`, `EN_ESPERA`, `EN_ATENCION`, `CERRADA` o `CANCELADA`.
- `from`: fecha/hora ISO-8601, por ejemplo `2026-09-20T08:00:00`.
- `to`: fecha/hora ISO-8601.

Cuando se envian varios filtros, la implementacion prioriza estas combinaciones: `status + from + to`, solo `status`, solo `from + to`, o todos los registros. No combina filtros parciales de fecha.

### Modelo de cita

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | `Long` | Generado por la base de datos |
| `patientId` | `String` | Obligatorio y no vacio |
| `serviceId` | `Long` | Obligatorio |
| `boxId` | `Long` | Opcional |
| `appointmentDate` | `LocalDateTime` | Obligatorio y futuro |
| `status` | Enum | Se inicia en `SOLICITADA` |
| `createdAt` | `LocalDateTime` | Se genera al crear |
| `updatedAt` | `LocalDateTime` | Se actualiza al modificar |

Tabla JPA: `VS_APPOINTMENTS`.

Ejemplo de creacion:

```json
{
  "patientId": "paciente-001",
  "serviceId": 1,
  "boxId": 2,
  "appointmentDate": "2026-09-20T10:30:00"
}
```

### Transiciones de estado

```text
SOLICITADA -> CONFIRMADA -> EN_ESPERA -> EN_ATENCION -> CERRADA
      |             |             |             |
      +-------------+-------------+-------------+
                    CANCELADA
```

Transiciones validas:

- `SOLICITADA` -> `CONFIRMADA` o `CANCELADA`.
- `CONFIRMADA` -> `EN_ESPERA` o `CANCELADA`.
- `EN_ESPERA` -> `EN_ATENCION` o `CANCELADA`.
- `EN_ATENCION` -> `CERRADA`.
- `CERRADA` y `CANCELADA` son estados finales.

No se permite modificar los datos de una cita cerrada o cancelada. Tampoco se permite repetir el mismo estado.

## 8. API interna de catalog

Base URL local: `http://localhost:8082`.

### Prestaciones: `/api/catalog/services`

Campos de entrada:

```json
{
  "name": "Consulta medica general",
  "description": "Evaluacion medica general",
  "price": 25000.00,
  "durationMinutes": 30,
  "active": true
}
```

Reglas:

- `name` es obligatorio y admite hasta 120 caracteres.
- `description` admite hasta 500 caracteres.
- `price` es obligatorio y no puede ser negativo.
- `durationMinutes` es obligatorio y debe ser mayor que cero.
- `active` toma `true` por defecto al crear.
- `GET` admite `?active=true` o `?active=false`.

Respuesta: `id`, `name`, `description`, `price`, `durationMinutes` y `active`.

### Boxes clinicos: `/api/catalog/boxes`

Campos de entrada:

```json
{
  "name": "Box 1",
  "centerName": "Centro VidaSalud Santiago",
  "active": true
}
```

Reglas:

- `name` es obligatorio y admite hasta 100 caracteres.
- `centerName` es obligatorio y admite hasta 150 caracteres.
- `active` toma `true` por defecto al crear.
- `GET` admite `?active=true` o `?active=false`.

Respuesta: `id`, `name`, `centerName` y `active`.

### Cupos: `/api/catalog/slots`

Campos de entrada:

```json
{
  "boxId": 1,
  "serviceId": 1,
  "startTime": "2026-09-20T10:30:00",
  "available": true
}
```

Reglas:

- `boxId`, `serviceId` y `startTime` son obligatorios.
- `startTime` debe estar en el futuro.
- El box y la prestacion referenciados deben existir y estar activos al crear.
- No puede existir otro cupo para el mismo box en la misma fecha y hora.
- `available` toma `true` por defecto al crear.
- Al actualizar un cupo se validan las referencias, pero la implementacion no repite la comprobacion de actividad ni duplicidad que se hace al crear.

Filtros disponibles en `GET /api/catalog/slots`:

- `available=true|false`.
- `boxId`.
- `serviceId`.
- `from` y `to` como fecha/hora ISO-8601.

La implementacion aplica un solo criterio de busqueda por llamada, con prioridad `available`, `boxId`, `serviceId`, rango de fechas y finalmente todos los registros.

Operaciones de disponibilidad:

- `PUT /api/catalog/slots/{id}/reserve`: cambia `available` a `false`; falla si ya estaba ocupado.
- `PUT /api/catalog/slots/{id}/release`: cambia `available` a `true`; falla si ya estaba disponible.

La respuesta de un cupo incluye informacion enriquecida del box y la prestacion: `id`, `boxId`, `boxName`, `serviceId`, `serviceName`, `startTime` y `available`.

## 9. Persistencia

Los servicios de dominio usan Spring Data JPA y crean/actualizan el esquema con Hibernate:

- Citas: entidad `Appointment`, tabla `VS_APPOINTMENTS`.
- Catalogo: entidades `HealthService`, `ClinicalBox` y `Slot`.
- `Slot` mantiene referencias JPA a `ClinicalBox` y `HealthService`.
- Los repositorios ofrecen consultas por estado, disponibilidad, referencias y rangos de fecha.

No existe una base de datos compartida entre los servicios. El BFF no persiste informacion.

## 10. Seguridad y CORS

### BFF

- Actua como OAuth2 Resource Server.
- Valida issuer y audience del JWT.
- Lee roles desde el claim `roles`.
- Usa sesiones sin estado basadas en bearer token.
- Desactiva CSRF para la API.
- Permite CORS para:
  - `http://localhost:4200`
  - `https://100.30.46.47`
- Metodos permitidos: `GET`, `POST`, `PUT`, `DELETE` y `OPTIONS`.

### Servicios internos

Los servicios `appointments` y `catalog` tienen Spring Security, pero su configuracion actual desactiva CSRF y permite todas las solicitudes (`permitAll`). Por lo tanto, la frontera efectiva de autenticacion y autorizacion es el BFF. En despliegues donde los puertos internos sean accesibles desde fuera de la red privada, se debe restringir su exposicion o agregar validacion de token en estos servicios.

## 11. Manejo de errores

`appointments` y `catalog` usan una respuesta comun:

```json
{
  "timestamp": "2026-09-16T12:00:00",
  "status": 409,
  "error": "Business Rule Violation",
  "message": "Descripcion del error",
  "path": "uri=/api/..."
}
```

Respuestas implementadas:

| HTTP | Caso |
|---:|---|
| `400` | Error de validacion de campos del request |
| `404` | Recurso no encontrado |
| `409` | Regla de negocio incumplida, como una transicion invalida o reservar un cupo ocupado |
| `401` | JWT ausente o invalido en el BFF |
| `403` | JWT valido sin el rol requerido en una ruta de citas del BFF |

El BFF usa `RestClient.retrieve()`, por lo que los errores HTTP de los servicios internos se propagan como errores del cliente HTTP si no se agrega un manejador especifico.

## 12. Estructura de paquetes

Cada microservicio sigue una separacion por responsabilidad:

```text
src/main/java/cl/duoc/vidasalud/<modulo>/
  config/       Configuracion de seguridad, JWT o CORS
  controller/   Endpoints REST
  dto/          Records de entrada y salida
  entity/       Entidades JPA en los servicios de dominio
  exception/    Excepciones y respuestas de error
  repository/  Interfaces Spring Data JPA
  service/      Reglas de negocio
  client/       Clientes HTTP del BFF
```

## 13. Consideraciones de mantenimiento

- Mantener secretos fuera de `application.yaml`; usar variables de entorno o un gestor de secretos.
- Restringir la exposicion directa de los puertos `8081` y `8082`.
- Definir autorizacion por rol para las operaciones de catalogo si el BFF debe diferenciar lectura y administracion.
- Agregar validacion `@Valid` en los controladores del BFF para fallar antes de llamar a los servicios internos.
- Agregar pruebas de endpoints, reglas de transicion y reserva/liberacion de cupos; las pruebas actuales son principalmente de carga del contexto.
- Evaluar manejo centralizado de errores y timeouts para las llamadas del BFF a los servicios internos.

## 14. Archivos de configuracion principales

- `ms-vidasalud-bff/src/main/resources/application.yaml`
- `ms-vidasalud-appointments/src/main/resources/application.yaml`
- `ms-vidasalud-catalog/src/main/resources/application.yaml`
- `ms-vidasalud-bff/pom.xml`
- `ms-vidasalud-appointments/pom.xml`
- `ms-vidasalud-catalog/pom.xml`
