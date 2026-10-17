package cl.duoc.vidasalud.mqadmin.dto;

public record ExchangeResponse(
        String name,
        String type,
        boolean durable,
        boolean builtIn,
        boolean protectedResource
) {
}
