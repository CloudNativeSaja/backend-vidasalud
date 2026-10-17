package cl.duoc.vidasalud.notify.admission;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cl.duoc.vidasalud.notify.messaging.CommandEnvelope;
import cl.duoc.vidasalud.notify.messaging.InvalidCommandException;
import cl.duoc.vidasalud.notify.simulation.FailureSimulator;

/**
 * Logica de negocio de admision: emite el ticket de llegada del paciente
 * y el llamado al box clinico (pantalla de sala de espera).
 */
@Service
public class AdmissionTicketService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionTicketService.class);

    private final FailureSimulator failureSimulator;
    private final AtomicInteger ticketSequence = new AtomicInteger();

    public AdmissionTicketService(FailureSimulator failureSimulator) {
        this.failureSimulator = failureSimulator;
    }

    public void process(CommandEnvelope command) {
        String appointmentId = command.payloadValue("appointmentId");
        if (appointmentId.isBlank()) {
            throw new InvalidCommandException("El comando de admision no incluye appointmentId");
        }

        failureSimulator.check(command);

        String box = command.payloadValue("boxId").isBlank() ? "por asignar" : command.payloadValue("boxId");

        if ("ADMISSION_LLAMADO_BOX".equals(command.type())) {
            log.info("[ADMISSION] Llamado a box | atencion={} paciente={} box={}",
                    appointmentId, command.payloadValue("patientId"), box);
        } else {
            String ticket = String.format("T-%04d", ticketSequence.incrementAndGet());
            log.info("[ADMISSION] Ticket emitido {} | atencion={} paciente={} box={}",
                    ticket, appointmentId, command.payloadValue("patientId"), box);
        }
    }
}
