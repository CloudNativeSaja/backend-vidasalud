package cl.duoc.vidasalud.mqadmin.dto;

public record PublishResponse(String eventId, String exchange, String routingKey, String type) {
}
