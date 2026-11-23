package cl.duoc.vidasalud.bff.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Cliente de ms-vidasalud-mq-admin (administracion de RabbitMQ). */
@Component
public class MqAdminClient extends ForwardingClient {

    public MqAdminClient(
            RestClient.Builder builder,
            @Value("${services.mq-admin.url}") String baseUrl) {

        super(builder, baseUrl, "ms-vidasalud-mq-admin");
    }
}
