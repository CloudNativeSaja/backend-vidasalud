package cl.duoc.vidasalud.bff.controller;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.vidasalud.bff.client.KafkaAdminClient;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Expone la API del administrador de Kafka a traves del BFF.
 *
 * Flujo: Angular -> API Gateway (JWT) -> BFF (rol ADMIN) -> ms-vidasalud-kafka-admin
 *
 * /api/bff/kafka/cluster          -> /api/kafka/cluster
 * /api/bff/kafka/topics           -> /api/kafka/topics
 * /api/bff/kafka/consumer-groups  -> /api/kafka/consumer-groups
 */
@RestController
@RequestMapping("/api/bff/kafka")
@PreAuthorize("hasRole('ADMIN')")
public class BffKafkaAdminController {

    private static final String BFF_PREFIX = "/api/bff/kafka";
    private static final String TARGET_PREFIX = "/api/kafka";

    private final KafkaAdminClient client;

    public BffKafkaAdminController(KafkaAdminClient client) {
        this.client = client;
    }

    @RequestMapping("/**")
    public ResponseEntity<String> proxy(
            HttpServletRequest request,
            @RequestBody(required = false) String body) {

        return client.forward(HttpMethod.valueOf(request.getMethod()),
                ProxyPaths.target(request, BFF_PREFIX, TARGET_PREFIX), body);
    }
}
