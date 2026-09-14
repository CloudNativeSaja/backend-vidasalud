package cl.duoc.vidasalud.appointments.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AppointmentRequest(

    @NotBlank(message = "El paciente es obligatorio")
    String patientId,

    @NotNull(message = "La prestación es obligatoria")
    Long serviceId,

    Long boxId,

    @NotNull(message = "La fecha es obligatoria")
    @Future(message = "La fecha de atención debe ser futura")
    LocalDateTime appointmentDate

) {
}