package cl.duoc.vidasalud.notify.simulation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Marcadores para provocar fallos controlados durante la demostracion
 * (prefijo "vidasalud.notify.simulation").
 */
@ConfigurationProperties(prefix = "vidasalud.notify.simulation")
public record FailureSimulationProperties(
        boolean enabled,
        String transientFailureMarker,
        String permanentFailureMarker
) {
}
