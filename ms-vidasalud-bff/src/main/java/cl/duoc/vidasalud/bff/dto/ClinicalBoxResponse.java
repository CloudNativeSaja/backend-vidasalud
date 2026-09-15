package cl.duoc.vidasalud.bff.dto;

public record ClinicalBoxResponse(
    Long id,
    String name,
    String centerName,
    Boolean active
) {
}