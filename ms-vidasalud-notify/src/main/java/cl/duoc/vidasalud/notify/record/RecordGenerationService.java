package cl.duoc.vidasalud.notify.record;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cl.duoc.vidasalud.notify.messaging.CommandEnvelope;
import cl.duoc.vidasalud.notify.messaging.InvalidCommandException;
import cl.duoc.vidasalud.notify.simulation.FailureSimulator;

/**
 * Logica de negocio de documentos: comprobante de cita confirmada y
 * resumen de atencion cerrada (PDF). La generacion se simula con un log.
 */
@Service
public class RecordGenerationService {

    private static final Logger log = LoggerFactory.getLogger(RecordGenerationService.class);

    private final FailureSimulator failureSimulator;

    public RecordGenerationService(FailureSimulator failureSimulator) {
        this.failureSimulator = failureSimulator;
    }

    public void generate(CommandEnvelope command) {
        String appointmentId = command.payloadValue("appointmentId");
        if (appointmentId.isBlank()) {
            throw new InvalidCommandException("El comando de documento no incluye appointmentId");
        }

        failureSimulator.check(command);

        String document = "RECORD_RESUMEN".equals(command.type())
                ? "resumen-atencion-" + appointmentId + ".pdf"
                : "comprobante-cita-" + appointmentId + ".pdf";

        log.info("[RECORD] Documento generado {} | paciente={} estado={}",
                document, command.payloadValue("patientId"), command.payloadValue("status"));
    }
}
