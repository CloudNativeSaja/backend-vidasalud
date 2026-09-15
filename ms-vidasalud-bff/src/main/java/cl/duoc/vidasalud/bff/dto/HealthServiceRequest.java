package cl.duoc.vidasalud.bff.dto;

import java.math.BigDecimal;

public record HealthServiceRequest(
    String name,
    String description,
    BigDecimal price,
    Integer durationMinutes,
    Boolean active
) {
}