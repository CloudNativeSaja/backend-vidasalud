package cl.duoc.vidasalud.mqadmin.dto;

public record BindingResponse(
        String exchange,
        String queue,
        String routingKey,
        boolean protectedResource
) {
}
