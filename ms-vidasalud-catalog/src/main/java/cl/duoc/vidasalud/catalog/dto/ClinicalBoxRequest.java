package cl.duoc.vidasalud.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClinicalBoxRequest(

    @NotBlank(message = "El nombre del box es obligatorio")
    @Size(max = 100)
    String name,

    @NotBlank(message = "El centro es obligatorio")
    @Size(max = 150)
    String centerName,

    Boolean active

) {
}