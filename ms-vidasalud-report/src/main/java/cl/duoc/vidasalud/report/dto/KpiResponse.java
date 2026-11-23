package cl.duoc.vidasalud.report.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Indicadores agregados a partir de los eventos de citas.
 *
 * @param cancellationRate fraccion entre 0 y 1
 * @param lastEventAt      ultimo evento Kafka procesado (frescura del dato)
 */
public record KpiResponse(
        String range,
        long totalAppointments,
        Map<String, Long> byStatus,
        double cancellationRate,
        double averageWaitMinutes,
        Instant lastEventAt
) {
}
