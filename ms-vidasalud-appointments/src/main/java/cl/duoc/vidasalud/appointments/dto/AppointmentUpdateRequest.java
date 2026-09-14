package cl.duoc.vidasalud.appointments.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record AppointmentUpdateRequest(

    @NotNull(message = "La prestación es obligatoria")
    Long serviceId,

    Long boxId,

    @NotNull(message = "La fecha es obligatoria")
    @Future(message = "La fecha debe ser futura")
    LocalDateTime appointmentDate

) {
}