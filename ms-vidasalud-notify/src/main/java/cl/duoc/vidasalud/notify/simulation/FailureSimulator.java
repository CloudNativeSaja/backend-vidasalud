package cl.duoc.vidasalud.notify.simulation;

import org.springframework.stereotype.Component;

import cl.duoc.vidasalud.notify.messaging.CommandEnvelope;
import cl.duoc.vidasalud.notify.messaging.InvalidCommandException;
import cl.duoc.vidasalud.notify.messaging.RecoverableProcessingException;

/**
 * Simula fallos para demostrar reintentos y DLQ sin tocar la logica real.
 *
 * - patientId contiene FALLA-TEMPORAL  -> error recuperable (se reintenta y termina en DLQ)
 * - patientId contiene FALLA-PERMANENTE -> error no recuperable (DLQ inmediato)
 *
 * Se desactiva con NOTIFY_SIMULATION_ENABLED=false.
 */
@Component
public class FailureSimulator {

    private final FailureSimulationProperties properties;

    public FailureSimulator(FailureSimulationProperties properties) {
        this.properties = properties;
    }

    public void check(CommandEnvelope command) {
        if (!properties.enabled()) {
            return;
        }
        String patientId = command.payloadValue("patientId");

        if (contains(patientId, properties.permanentFailureMarker())) {
            throw new InvalidCommandException(
                    "Simulacion: datos del paciente invalidos (" + patientId + ")");
        }
        if (contains(patientId, properties.transientFailureMarker())) {
            throw new RecoverableProcessingException(
                    "Simulacion: servicio externo no disponible (" + patientId + ")");
        }
    }

    private static boolean contains(String value, String marker) {
        return marker != null && !marker.isBlank() && value.contains(marker);
    }
}
