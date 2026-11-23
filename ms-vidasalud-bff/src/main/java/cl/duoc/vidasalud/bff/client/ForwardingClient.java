package cl.duoc.vidasalud.bff.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Reenvia una solicitud a un microservicio interno y devuelve su respuesta
 * tal cual (codigo HTTP y JSON), incluidos los errores de validacion (400),
 * 404 y 409. Si el microservicio no responde, contesta 503.
 */
public abstract class ForwardingClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final String serviceName;

    protected ForwardingClient(RestClient.Builder builder, String baseUrl, String serviceName) {
        this.restClient = builder.build();
        this.baseUrl = baseUrl;
        this.serviceName = serviceName;
    }

    /**
     * @param method       metodo HTTP original
     * @param pathAndQuery ruta interna, ej. "/api/report/kpis?range=last7d"
     * @param body         cuerpo JSON (puede ser null)
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
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\":503,\"message\":\"" + serviceName + " no esta disponible\"}");
        }
    }
}
