package cl.duoc.vidasalud.bff.dto;

public record ClinicalBoxRequest(
    String name,
    String centerName,
    Boolean active
) {
}