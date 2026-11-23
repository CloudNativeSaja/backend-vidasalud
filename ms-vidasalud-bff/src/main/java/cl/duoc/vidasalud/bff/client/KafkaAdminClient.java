package cl.duoc.vidasalud.bff.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Cliente de ms-vidasalud-kafka-admin (administracion de Kafka). */
@Component
public class KafkaAdminClient extends ForwardingClient {

    public KafkaAdminClient(
            RestClient.Builder builder,
            @Value("${services.kafka-admin.url}") String baseUrl) {

        super(builder, baseUrl, "ms-vidasalud-kafka-admin");
    }
}
