package cl.duoc.vidasalud.appointments.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Nombres de exchanges y routing keys leidos desde application.yaml
 * (prefijo "vidasalud.messaging"). Unica fuente de nombres del productor.
 */
@Validated
@ConfigurationProperties(prefix = "vidasalud.messaging")
public record MessagingProperties(

        boolean enabled,

        @NotBlank String source,

        @Valid @NotNull Exchanges exchanges,

        @Valid @NotNull Publishing publishing

) {

    public record Exchanges(
            @NotBlank String direct,
            @NotBlank String topic
    ) {
    }

    /** Routing key de cada comando que publica el servicio de atenciones. */
    public record Publishing(
            @NotBlank String emailSend,
            @NotBlank String emailRequested,
            @NotBlank String emailCalled,
            @NotBlank String emailClosed,
            @NotBlank String emailCancelled,
            @NotBlank String admissionTicket,
            @NotBlank String admissionCall,
            @NotBlank String recordGen,
            @NotBlank String recordSummary
    ) {
    }
}
