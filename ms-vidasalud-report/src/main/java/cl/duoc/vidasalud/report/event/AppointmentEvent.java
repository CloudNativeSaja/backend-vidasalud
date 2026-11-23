package cl.duoc.vidasalud.report.event;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Evento de dominio leido desde el topico {@code vidasalud.appointments.events}.
 *
 * Es el mismo contrato JSON que publica ms-vidasalud-appointments. Cada
 * microservicio mantiene su propia copia del DTO para no acoplarse al codigo
 * del productor; solo comparten el formato del mensaje.
 */
public record AppointmentEvent(
        @NotBlank String eventId,
        @NotNull Type type,
        @NotNull Instant occurredAt,
        @NotNull @Positive Long appointmentId,
        String patientId,
        @NotNull @Positive Long serviceId,
        Long boxId,
        AppointmentStatus previousStatus,
        @NotNull AppointmentStatus newStatus
) {

    public enum Type {
        APPOINTMENT_CREATED,
        APPOINTMENT_STATUS_CHANGED
    }
}
