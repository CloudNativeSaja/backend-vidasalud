package cl.duoc.vidasalud.appointments.dto;

import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(

    @NotNull(message = "El estado es obligatorio")
    AppointmentStatus status

) {
}