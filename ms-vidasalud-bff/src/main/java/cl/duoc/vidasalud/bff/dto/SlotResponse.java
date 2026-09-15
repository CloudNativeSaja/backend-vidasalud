package cl.duoc.vidasalud.bff.dto;

import java.time.LocalDateTime;

public record SlotResponse(
    Long id,
    Long boxId,
    String boxName,
    Long serviceId,
    String serviceName,
    LocalDateTime startTime,
    Boolean available
) {
}