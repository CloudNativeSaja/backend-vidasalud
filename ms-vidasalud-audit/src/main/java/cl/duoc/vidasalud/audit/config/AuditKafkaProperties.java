package cl.duoc.vidasalud.audit.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Topicos, consumer groups y politica de errores, leidos desde
 * application.yaml (prefijo "vidasalud.kafka"). Unica fuente de nombres.
 */
@Validated
@ConfigurationProperties(prefix = "vidasalud.kafka")
public record AuditKafkaProperties(
        @Valid @NotNull Topics topics,
        @Valid @NotNull Groups groups,
        @Valid @NotNull AuditTopic auditTopic,
        @Valid @NotNull DeadLetter deadLetter,
        @Valid @NotNull Retry retry
) {

    public record Topics(
            /** Entrada: eventos de citas que publica ms-vidasalud-appointments. */
            @NotBlank String appointmentsEvents,
            /** Salida y entrada: eventos de auditoria. */
            @NotBlank String auditEvents
    ) {
    }

    /** Un consumer group por responsabilidad. */
    public record Groups(
            /** Transforma eventos de citas en eventos de auditoria. */
            @NotBlank String auditoria,
            /** Arma la linea de tiempo a partir de los eventos de auditoria. */
            @NotBlank String timeline
    ) {
    }

    /** Parametros del topico de auditoria (lo declara su productor). */
    public record AuditTopic(
            @Min(1) int partitions,
            @Min(1) short replicationFactor,
            @NotNull Duration retention,
            @NotBlank String minInsyncReplicas
    ) {
    }

    /** Una DLT por topico de entrada. */
    public record DeadLetter(
            @NotBlank String appointmentsTopic,
            @NotBlank String auditTopic,
            @Min(1) int partitions,
            @Min(1) short replicationFactor,
            @NotNull Duration retention
    ) {
    }

    public record Retry(
            @Min(0) int maxRetries,
            @NotNull Duration initialInterval,
            double multiplier,
            @NotNull Duration maxInterval
    ) {
    }
}
