package cl.duoc.vidasalud.catalog.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record SlotRequest(

    @NotNull(message = "El box es obligatorio")
    Long boxId,

    @NotNull(message = "La prestación es obligatoria")
    Long serviceId,

    @NotNull(message = "La fecha y hora son obligatorias")
    @Future(message = "El cupo debe corresponder a una fecha futura")
    LocalDateTime startTime,

    Boolean available

) {
}