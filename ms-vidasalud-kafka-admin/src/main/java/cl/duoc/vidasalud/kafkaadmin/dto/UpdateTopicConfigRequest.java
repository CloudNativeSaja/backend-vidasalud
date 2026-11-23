package cl.duoc.vidasalud.kafkaadmin.dto;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

/** Configuraciones a modificar en un topico existente. */
public record UpdateTopicConfigRequest(

        @NotEmpty(message = "Debe indicar al menos una configuracion")
        Map<String, String> configs
) {
}
