package cl.duoc.vidasalud.kafkaadmin.dto;

import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para crear un topico. Si no se indican particiones o factor de
 * replicacion se usan los valores por defecto de application.yaml.
 */
public record CreateTopicRequest(

        @NotBlank(message = "El nombre del topico es obligatorio")
        @Size(max = 249, message = "El nombre del topico no puede superar 249 caracteres")
        @Pattern(regexp = "[a-zA-Z0-9._-]+",
                message = "El nombre solo admite letras, numeros, punto, guion y guion bajo")
        String name,

        @Min(value = 1, message = "Debe tener al menos 1 particion")
        @Max(value = 100, message = "No puede superar 100 particiones")
        Integer partitions,

        @Min(value = 1, message = "El factor de replicacion debe ser al menos 1")
        @Max(value = 10, message = "El factor de replicacion no puede superar 10")
        Short replicationFactor,

        /** Ej.: retention.ms, cleanup.policy, min.insync.replicas. */
        Map<String, String> configs
) {
}
