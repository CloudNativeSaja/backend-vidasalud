package cl.duoc.vidasalud.appointments.streaming;

import java.time.Instant;
import java.util.UUID;

import cl.duoc.vidasalud.appointments.entity.Appointment;
import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;

/**
 * Evento de dominio que se publicara en el topico Kafka
 * {@code vidasalud.appointments.events} (3 particiones x 3 replicas).
 *
 * A diferencia de los comandos de RabbitMQ (CommandEnvelope), un evento
 * describe algo que YA ocurrio y queda guardado en el log de Kafka para que
 * reportes (KPIs) y auditoria (timeline) lo lean cada uno con su consumer group.
 *
 * - eventId: UUID para que los consumidores descarten duplicados (idempotencia).
 * - key(): appointmentId, asi todos los eventos de una misma cita caen en la
 *   misma particion y se procesan en orden.
 * - previousStatus / newStatus: permiten calcular cancelaciones y tiempos de
 *   espera sin consultar la base de datos de citas.
 */
public record AppointmentEvent(
        String eventId,
        Type type,
        Instant occurredAt,
        Long appointmentId,
        String patientId,
        Long serviceId,
        Long boxId,
        AppointmentStatus previousStatus,
        AppointmentStatus newStatus
) {

    public enum Type {
        APPOINTMENT_CREATED,
        APPOINTMENT_STATUS_CHANGED
    }

    /** Evento al registrar una nueva cita (estado SOLICITADA). */
    public static AppointmentEvent created(Appointment appointment) {
        return of(Type.APPOINTMENT_CREATED, appointment, null);
    }

    /** Evento al cambiar el estado de una cita. */
    public static AppointmentEvent statusChanged(Appointment appointment, AppointmentStatus previousStatus) {
        return of(Type.APPOINTMENT_STATUS_CHANGED, appointment, previousStatus);
    }

    /** Key del mensaje Kafka: garantiza el orden de los eventos por cita. */
    public String key() {
        return String.valueOf(appointmentId);
    }

    private static AppointmentEvent of(Type type, Appointment appointment, AppointmentStatus previousStatus) {
        return new AppointmentEvent(
                UUID.randomUUID().toString(),
                type,
                Instant.now(),
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getServiceId(),
                appointment.getBoxId(),
                previousStatus,
                appointment.getStatus());
    }
}
