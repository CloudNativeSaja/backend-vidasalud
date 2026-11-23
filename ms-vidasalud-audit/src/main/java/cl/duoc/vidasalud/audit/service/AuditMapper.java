package cl.duoc.vidasalud.audit.service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import cl.duoc.vidasalud.audit.event.AppointmentEvent;
import cl.duoc.vidasalud.audit.event.AuditEvent;

/**
 * Traduce un evento de cita en un evento de auditoria. Logica pura, sin Kafka.
 *
 * El eventId de auditoria se deriva del evento de origen: si el mismo evento
 * de cita se procesa dos veces (reintento o relectura), genera el mismo
 * evento de auditoria y la linea de tiempo lo descarta como duplicado.
 */
@Component
public class AuditMapper {

    static final String SOURCE = "ms-vidasalud-appointments";

    public AuditEvent fromAppointment(AppointmentEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appointmentId", event.appointmentId());
        payload.put("serviceId", event.serviceId());
        payload.put("boxId", event.boxId());
        payload.put("estadoAnterior", event.previousStatus() != null ? event.previousStatus().name() : null);
        payload.put("estadoNuevo", event.newStatus().name());
        payload.put("origen", SOURCE);

        return new AuditEvent(
                UUID.nameUUIDFromBytes(("audit:" + event.eventId()).getBytes(StandardCharsets.UTF_8)).toString(),
                event.eventId(),
                "cita-" + event.appointmentId(),
                AuditEvent.Categoria.CITAS,
                accion(event),
                event.patientId() != null && !event.patientId().isBlank()
                        ? "paciente:" + event.patientId()
                        : "sistema:" + SOURCE,
                event.occurredAt(),
                payload);
    }

    private static String accion(AppointmentEvent event) {
        if (event.type() == AppointmentEvent.Type.APPOINTMENT_CREATED) {
            return "CITA_CREADA";
        }
        return "CITA_" + event.newStatus().name();
    }
}
