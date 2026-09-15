package cl.duoc.vidasalud.bff.dto;

import java.time.LocalDateTime;

public record SlotRequest(
    Long boxId,
    Long serviceId,
    LocalDateTime startTime,
    Boolean available
) {
}