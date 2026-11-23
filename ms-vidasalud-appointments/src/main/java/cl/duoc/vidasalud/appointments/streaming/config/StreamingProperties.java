package cl.duoc.vidasalud.appointments.streaming.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Nombres de topicos Kafka leidos desde application.yaml
 * (prefijo "vidasalud.streaming"). Unica fuente de nombres del productor.
 *
 * Las propiedades del cliente (bootstrap servers, serializers, acks...) viven
 * en el bloque estandar "spring.kafka".
 */
@Validated
@ConfigurationProperties(prefix = "vidasalud.streaming")
public record StreamingProperties(

        /** false = no se publican eventos (la API funciona igual). */
        boolean enabled,

        @Valid @NotNull Topics topics

) {

    public record Topics(
            /** Eventos de citas (creada / cambio de estado). Clave: appointmentId. */
            @NotBlank String appointmentsEvents
    ) {
    }
}
