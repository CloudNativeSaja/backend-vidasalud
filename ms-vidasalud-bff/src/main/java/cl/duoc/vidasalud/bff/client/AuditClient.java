package cl.duoc.vidasalud.bff.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Cliente de ms-vidasalud-audit (linea de tiempo de auditoria desde Kafka). */
@Component
public class AuditClient extends ForwardingClient {

    public AuditClient(
            RestClient.Builder builder,
            @Value("${services.audit.url}") String baseUrl) {

        super(builder, baseUrl, "ms-vidasalud-audit");
    }
}
