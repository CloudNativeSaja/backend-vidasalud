package cl.duoc.vidasalud.appointments.messaging;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cl.duoc.vidasalud.appointments.entity.Appointment;
import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;
import cl.duoc.vidasalud.appointments.messaging.config.MessagingProperties;
import cl.duoc.vidasalud.appointments.notification.AppointmentNotifier;

/**
 * Traduce los eventos de una atencion en comandos RabbitMQ.
 *
 * <pre>
 * Evento                 Comando                    Exchange    Routing key          Cola destino
 * ---------------------- -------------------------- ----------- -------------------- ---------------
 * Atencion creada        EMAIL_SOLICITUD_RECIBIDA   cmd.topic   email.requested      q.cmd.email
 * -> CONFIRMADA          EMAIL_CITA_CONFIRMADA      cmd.direct  email.send           q.cmd.email
 *                        RECORD_COMPROBANTE         cmd.direct  record.gen           q.cmd.record
 * -> EN_ESPERA           ADMISSION_TICKET           cmd.direct  admission.ticket     q.cmd.admission
 * -> EN_ATENCION         ADMISSION_LLAMADO_BOX      cmd.topic   admission.call.box   q.cmd.admission
 *                        EMAIL_LLAMADO_BOX          cmd.topic   email.called         q.cmd.email
 * -> CERRADA             EMAIL_ATENCION_CERRADA     cmd.topic   email.closed         q.cmd.email
 *                        RECORD_RESUMEN             cmd.topic   record.summary       q.cmd.record
 * -> CANCELADA           EMAIL_CITA_CANCELADA       cmd.topic   email.cancelled      q.cmd.email
 * </pre>
 *
 * Las routing keys se leen de application.yaml (vidasalud.messaging.publishing).
 */
@Component
@ConditionalOnProperty(prefix = "vidasalud.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitAppointmentNotifier implements AppointmentNotifier {

    private final CommandPublisher publisher;
    private final MessagingProperties.Exchanges exchanges;
    private final MessagingProperties.Publishing keys;

    public RabbitAppointmentNotifier(CommandPublisher publisher, MessagingProperties properties) {
        this.publisher = publisher;
        this.exchanges = properties.exchanges();
        this.keys = properties.publishing();
    }

    @Override
    public void appointmentCreated(Appointment appointment) {
        publishAll(appointment, null, List.of(
                topic(CommandType.EMAIL_SOLICITUD_RECIBIDA, keys.emailRequested())));
    }

    @Override
    public void statusChanged(Appointment appointment, AppointmentStatus previousStatus) {
        List<Route> routes = switch (appointment.getStatus()) {
            case CONFIRMADA -> List.of(
                    direct(CommandType.EMAIL_CITA_CONFIRMADA, keys.emailSend()),
                    direct(CommandType.RECORD_COMPROBANTE, keys.recordGen()));
            case EN_ESPERA -> List.of(
                    direct(CommandType.ADMISSION_TICKET, keys.admissionTicket()));
            case EN_ATENCION -> List.of(
                    topic(CommandType.ADMISSION_LLAMADO_BOX, keys.admissionCall()),
                    topic(CommandType.EMAIL_LLAMADO_BOX, keys.emailCalled()));
            case CERRADA -> List.of(
                    topic(CommandType.EMAIL_ATENCION_CERRADA, keys.emailClosed()),
                    topic(CommandType.RECORD_RESUMEN, keys.recordSummary()));
            case CANCELADA -> List.of(
                    topic(CommandType.EMAIL_CITA_CANCELADA, keys.emailCancelled()));
            case SOLICITADA -> List.of();
        };
        publishAll(appointment, previousStatus, routes);
    }

    // -----------------------------------------------------------------

    /** Todos los comandos de un mismo evento comparten traceId y correlationId. */
    private void publishAll(Appointment appointment, AppointmentStatus previousStatus, List<Route> routes) {
        String traceId = UUID.randomUUID().toString();
        String correlationId = "appointment-" + appointment.getId();
        Map<String, Object> payload = payload(appointment, previousStatus);

        for (Route route : routes) {
            publisher.publish(route.exchange(), route.routingKey(), route.type(), traceId, correlationId, payload);
        }
    }

    private Map<String, Object> payload(Appointment appointment, AppointmentStatus previousStatus) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appointmentId", String.valueOf(appointment.getId()));
        payload.put("patientId", appointment.getPatientId());
        payload.put("serviceId", String.valueOf(appointment.getServiceId()));
        payload.put("boxId", appointment.getBoxId() != null ? String.valueOf(appointment.getBoxId()) : "");
        payload.put("appointmentDate", String.valueOf(appointment.getAppointmentDate()));
        payload.put("status", appointment.getStatus().name());
        payload.put("previousStatus", previousStatus != null ? previousStatus.name() : "");
        return payload;
    }

    private Route direct(CommandType type, String routingKey) {
        return new Route(type, exchanges.direct(), routingKey);
    }

    private Route topic(CommandType type, String routingKey) {
        return new Route(type, exchanges.topic(), routingKey);
    }

    private record Route(CommandType type, String exchange, String routingKey) {
    }
}
