package cl.duoc.vidasalud.bff.dto;

import java.time.LocalDateTime;

public record AppointmentRequest(
    String patientId,
    Long serviceId,
    Long boxId,
    LocalDateTime appointmentDate
) {
}