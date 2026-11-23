package cl.duoc.vidasalud.kafkaadmin.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Configuracion del administrador leida desde application.yaml
 * (prefijo "vidasalud.kafka-admin").
 */
@Validated
@ConfigurationProperties(prefix = "vidasalud.kafka-admin")
public record KafkaAdminProperties(
        @NotNull Duration timeout,
        @Valid @NotNull Defaults defaults,
        List<String> protectedNames,
        @NotEmpty List<String> editableConfigs
) {

    public KafkaAdminProperties {
        protectedNames = protectedNames == null ? List.of() : List.copyOf(protectedNames);
        editableConfigs = editableConfigs == null ? List.of() : List.copyOf(editableConfigs);
    }

    public record Defaults(
            @Min(1) int partitions,
            @Min(1) short replicationFactor
    ) {
    }
}
