package cl.duoc.vidasalud.catalog.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HealthServiceRequest(

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120)
    String name,

    @Size(max = 500)
    String description,

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(
        value = "0.0",
        inclusive = true,
        message = "El precio no puede ser negativo"
    )
    BigDecimal price,

    @NotNull(message = "La duración es obligatoria")
    @Min(
        value = 1,
        message = "La duración debe ser mayor a 0"
    )
    Integer durationMinutes,

    Boolean active

) {
}