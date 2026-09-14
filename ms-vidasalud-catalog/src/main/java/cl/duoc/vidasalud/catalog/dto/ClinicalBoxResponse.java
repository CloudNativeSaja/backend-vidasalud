package cl.duoc.vidasalud.catalog.dto;

public record ClinicalBoxResponse(

    Long id,
    String name,
    String centerName,
    Boolean active

) {
}