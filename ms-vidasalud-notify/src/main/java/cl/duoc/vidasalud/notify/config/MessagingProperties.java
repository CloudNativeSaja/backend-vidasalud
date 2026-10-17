package cl.duoc.vidasalud.notify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Nombres de exchanges, colas, DLQ y bindings leidos desde application.yaml
 * (prefijo "vidasalud.messaging").
 *
 * Es la UNICA fuente de nombres de mensajeria del microservicio:
 * ninguna clase de negocio ni listener escribe nombres "a mano".
 */
@Validated
@ConfigurationProperties(prefix = "vidasalud.messaging")
public record MessagingProperties(

        @Valid @NotNull Exchanges exchanges,

        @NotBlank @Pattern(regexp = "classic|quorum") String queueType,

        @Positive long dlqRetentionMs,

        @Valid @NotNull Routes routes,

        @Valid @NotNull Consumer consumer

) {

    /** Exchanges de la topologia: direct, topic y dead-letter (DLX). */
    public record Exchanges(
            @NotBlank String direct,
            @NotBlank String topic,
            @NotBlank String deadLetter
    ) {
    }

    /** Las 3 rutas de mensajeria del caso: email, admision y documentos. */
    public record Routes(
            @Valid @NotNull Route email,
            @Valid @NotNull Route admission,
            @Valid @NotNull Route record
    ) {
    }

    /**
     * Una ruta = cola principal + su DLQ + sus bindings.
     *
     * @param queue         cola principal (consumida por el listener del dominio)
     * @param dlq           cola de mensajes muertos asociada
     * @param directKey     routing key exacta en el exchange direct
     * @param topicPattern  patron en el exchange topic (* = 1 palabra, # = 0..n)
     * @param deadLetterKey routing key con que el DLX entrega a la DLQ
     */
    public record Route(
            @NotBlank String queue,
            @NotBlank String dlq,
            @NotBlank String directKey,
            @NotBlank String topicPattern,
            @NotBlank String deadLetterKey
    ) {
    }

    /** Politica de reintentos e idempotencia de los consumidores. */
    public record Consumer(
            @Min(1) int maxAttempts,
            @Min(0) long retryDelayMs,
            @Min(100) int idempotencyCacheSize
    ) {
    }
}
