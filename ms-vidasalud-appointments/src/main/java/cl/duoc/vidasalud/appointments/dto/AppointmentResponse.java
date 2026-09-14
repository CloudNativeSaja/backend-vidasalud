package cl.duoc.vidasalud.appointments.dto;

import java.time.LocalDateTime;

import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;

public record AppointmentResponse(

    Long id,
    String patientId,
    Long serviceId,
    Long boxId,
    LocalDateTime appointmentDate,
    AppointmentStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt

) {
}