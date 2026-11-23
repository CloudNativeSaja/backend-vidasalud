package cl.duoc.vidasalud.audit.event;

import java.time.Instant;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Evento de auditoria publicado en el topico {@code vidasalud.audit.events}.
 *
 * Los nombres de campo son los que espera la pantalla de Auditoria del
 * frontend (EventoAuditoria).
 *
 * @param eventId       identificador unico del evento de auditoria
 * @param traceId       eventId del evento de origen (trazabilidad)
 * @param correlationId agrupa los eventos de una misma entidad (ej. "cita-42")
 * @param categoria     CITAS, CATALOGO, MENSAJERIA o ACCESO
 * @param accion        que ocurrio (ej. CITA_CONFIRMADA)
 * @param usuario       quien o que origino la accion
 * @param ocurridoEn    momento en que ocurrio el hecho de origen
 * @param payload       datos relevantes del hecho
 */
public record AuditEvent(
        @NotBlank String eventId,
        @NotBlank String traceId,
        @NotBlank String correlationId,
        @NotNull Categoria categoria,
        @NotBlank String accion,
        @NotBlank String usuario,
        @NotNull Instant ocurridoEn,
        Map<String, Object> payload
) {

    public enum Categoria {
        CITAS,
        CATALOGO,
        MENSAJERIA,
        ACCESO
    }
}
