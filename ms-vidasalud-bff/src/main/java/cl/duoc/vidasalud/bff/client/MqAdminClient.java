package cl.duoc.vidasalud.bff.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Reenvia las solicitudes de administracion de RabbitMQ al microservicio
 * ms-vidasalud-mq-admin y devuelve su respuesta tal cual (codigo HTTP y JSON),
 * incluidos los errores de validacion (400), 404 y 409.
 */
@Component
public class MqAdminClient {

    private final RestClient restClient;
    private final String baseUrl;

    public MqAdminClient(
            RestClient.Builder builder,
            @Value("${services.mq-admin.url}") String baseUrl) {

        this.restClient = builder.build();
        this.baseUrl = baseUrl;
    }

    /**
     * @param method   metodo HTTP original
     * @param pathAndQuery ruta interna, ej. "/api/mq/queues?x=1"
     * @param body     cuerpo JSON (puede ser null)
     */
    public ResponseEntity<String> forward(HttpMethod method, String pathAndQuery, String body) {
        URI target = URI.create(baseUrl + pathAndQuery);

        RestClient.RequestBodySpec request = restClient.method(method).uri(target);
        if (body != null && !body.isBlank()) {
            request = request.contentType(MediaType.APPLICATION_JSON).body(body);
        }

        try {
            return request.exchange((clientRequest, clientResponse) -> {
                String responseBody = new String(
                        clientResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
                MediaType contentType = clientResponse.getHeaders().getContentType();

                return ResponseEntity
                        .status(clientResponse.getStatusCode())
                        .contentType(contentType != null ? contentType : MediaType.APPLICATION_JSON)
                        .body(responseBody);
            });
        } catch (ResourceAccessException ex) {
            return unavailable();
        }
    }

    private static ResponseEntity<String> unavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":503,\"message\":\"ms-vidasalud-mq-admin no esta disponible\"}");
    }
}
