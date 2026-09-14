package cl.duoc.vidasalud.bff.dto;

import java.time.LocalDateTime;

public record AppointmentResponse(

    Long id,
    String patientId,
    Long serviceId,
    Long boxId,
    LocalDateTime appointmentDate,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt

) {
}