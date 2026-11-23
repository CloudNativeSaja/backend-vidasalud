package cl.duoc.vidasalud.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Nuevo total de particiones. Kafka solo permite aumentarlas, nunca reducirlas. */
public record IncreasePartitionsRequest(

        @NotNull(message = "Debe indicar el nuevo total de particiones")
        @Min(value = 2, message = "El nuevo total debe ser al menos 2")
        @Max(value = 100, message = "No puede superar 100 particiones")
        Integer partitions
) {
}
