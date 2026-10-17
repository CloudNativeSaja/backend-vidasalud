package cl.duoc.vidasalud.mqadmin.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Configuracion del administrador (prefijo "vidasalud.mq-admin").
 *
 * @param vhost          virtual host administrado
 * @param management     acceso a la API HTTP de RabbitMQ Management
 * @param protectedNames colas/exchanges de la topologia base que no se pueden borrar
 */
@Validated
@ConfigurationProperties(prefix = "vidasalud.mq-admin")
public record MqAdminProperties(
        @NotBlank String vhost,
        @Valid @NotNull Management management,
        List<String> protectedNames
) {

    public MqAdminProperties {
        protectedNames = protectedNames == null ? List.of() : List.copyOf(protectedNames);
    }

    public boolean isProtected(String name) {
        return protectedNames.contains(name);
    }

    public record Management(
            @NotBlank String url,
            @NotBlank String username,
            @NotBlank String password
    ) {
    }
}
