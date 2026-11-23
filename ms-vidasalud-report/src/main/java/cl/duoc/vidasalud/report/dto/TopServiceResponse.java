package cl.duoc.vidasalud.report.dto;

/** Prestacion mas solicitada en el rango. */
public record TopServiceResponse(
        Long serviceId,
        String serviceName,
        long total
) {
}
