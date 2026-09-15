package cl.duoc.vidasalud.bff.dto;

import java.time.LocalDateTime;

public record AppointmentUpdateRequest(
    Long serviceId,
    Long boxId,
    LocalDateTime appointmentDate
) {
}