package cl.duoc.vidasalud.catalog.dto;

import java.math.BigDecimal;

public record HealthServiceResponse(

    Long id,
    String name,
    String description,
    BigDecimal price,
    Integer durationMinutes,
    Boolean active

) {
}