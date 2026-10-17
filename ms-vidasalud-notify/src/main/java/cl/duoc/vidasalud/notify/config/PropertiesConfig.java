package cl.duoc.vidasalud.notify.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import cl.duoc.vidasalud.notify.simulation.FailureSimulationProperties;

/**
 * Registra las propiedades tipadas leidas desde application.yaml.
 */
@Configuration
@EnableConfigurationProperties({
        MessagingProperties.class,
        FailureSimulationProperties.class
})
public class PropertiesConfig {
}
