package cl.duoc.vidasalud.notify.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cl.duoc.vidasalud.notify.messaging.CommandEnvelope;
import cl.duoc.vidasalud.notify.messaging.InvalidCommandException;
import cl.duoc.vidasalud.notify.simulation.FailureSimulator;

/**
 * Logica de negocio de notificaciones al paciente (email / push).
 *
 * No conoce RabbitMQ: recibe un comando ya validado. El envio real a un
 * proveedor (SES, SendGrid, Firebase) se simula con un log.
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final FailureSimulator failureSimulator;

    public EmailNotificationService(FailureSimulator failureSimulator) {
        this.failureSimulator = failureSimulator;
    }

    public void send(CommandEnvelope command) {
        String patientId = command.payloadValue("patientId");
        if (patientId.isBlank()) {
            throw new InvalidCommandException("El comando de email no incluye patientId");
        }

        failureSimulator.check(command);

        String subject = switch (command.type()) {
            case "EMAIL_SOLICITUD_RECIBIDA" -> "Recibimos tu solicitud de atencion";
            case "EMAIL_CITA_CONFIRMADA" -> "Tu atencion fue confirmada";
            case "EMAIL_LLAMADO_BOX" -> "Es tu turno: dirigete al box";
            case "EMAIL_ATENCION_CERRADA" -> "Gracias por atenderte en VidaSalud";
            case "EMAIL_CITA_CANCELADA" -> "Tu atencion fue cancelada";
            default -> "Notificacion VidaSalud";
        };

        log.info("[EMAIL] Enviado a paciente={} | asunto=\"{}\" | atencion={} | fecha={}",
                patientId, subject,
                command.payloadValue("appointmentId"),
                command.payloadValue("appointmentDate"));
    }
}
