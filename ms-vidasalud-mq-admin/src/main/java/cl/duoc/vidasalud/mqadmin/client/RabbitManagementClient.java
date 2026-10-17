package cl.duoc.vidasalud.mqadmin.client;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Lecturas sobre la API HTTP de RabbitMQ Management.
 * Devuelve el JSON crudo; {@code RabbitAdminService} lo transforma en DTOs.
 */
@Component
public class RabbitManagementClient {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> JSON_LIST =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public RabbitManagementClient(@Qualifier("rabbitManagementRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<Map<String, Object>> getQueues() {
        return get("/api/queues");
    }

    public List<Map<String, Object>> getExchanges() {
        return get("/api/exchanges");
    }

    public List<Map<String, Object>> getBindings() {
        return get("/api/bindings");
    }

    public List<Map<String, Object>> getNodes() {
        return get("/api/nodes");
    }

    private List<Map<String, Object>> get(String path) {
        List<Map<String, Object>> body = restClient.get()
                .uri(path)
                .retrieve()
                .body(JSON_LIST);
        return body == null ? List.of() : body;
    }
}
