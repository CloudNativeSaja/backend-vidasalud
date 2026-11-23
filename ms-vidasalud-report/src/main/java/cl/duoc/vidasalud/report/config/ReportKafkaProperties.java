package cl.duoc.vidasalud.report.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Nombres de topicos y politica de errores del consumidor, leidos desde
 * application.yaml (prefijo "vidasalud.kafka"). Unica fuente de nombres.
 */
@Validated
@ConfigurationProperties(prefix = "vidasalud.kafka")
public record ReportKafkaProperties(
        @Valid @NotNull Topics topics,
        @Valid @NotNull DeadLetter deadLetter,
        @Valid @NotNull Retry retry
) {

    public record Topics(
            /** Topico de entrada: eventos de citas. */
            @NotBlank String appointmentsEvents
    ) {
    }

    /** Topico de mensajes muertos propio de este consumer group. */
    public record DeadLetter(
            @NotBlank String topic,
            @Min(1) int partitions,
            @Min(1) short replicationFactor,
            @NotNull Duration retention
    ) {
    }

    /** Reintentos con espera exponencial antes de enviar a la DLT. */
    public record Retry(
            @Min(0) int maxRetries,
            @NotNull Duration initialInterval,
            double multiplier,
            @NotNull Duration maxInterval
    ) {
    }
}
