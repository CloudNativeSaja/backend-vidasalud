package cl.duoc.vidasalud.bff.controller;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.vidasalud.bff.client.MqAdminClient;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Expone la API del administrador de RabbitMQ a traves del BFF.
 *
 * Flujo: Angular -> API Gateway (JWT) -> BFF (rol ADMIN) -> ms-vidasalud-mq-admin
 *
 * /api/bff/mq/queues     -> /api/mq/queues
 * /api/bff/mq/exchanges  -> /api/mq/exchanges
 * /api/bff/mq/bindings   -> /api/mq/bindings
 * /api/bff/mq/messages   -> /api/mq/messages
 * /api/bff/mq/cluster/nodes -> /api/mq/cluster/nodes
 */
@RestController
@RequestMapping("/api/bff/mq")
@PreAuthorize("hasRole('ADMIN')")
public class BffMqAdminController {

    private static final String BFF_PREFIX = "/api/bff/mq";
    private static final String MQ_ADMIN_PREFIX = "/api/mq";

    private final MqAdminClient client;

    public BffMqAdminController(MqAdminClient client) {
        this.client = client;
    }

    @RequestMapping("/**")
    public ResponseEntity<String> proxy(
            HttpServletRequest request,
            @RequestBody(required = false) String body) {

        String path = request.getRequestURI()
                .substring(request.getContextPath().length() + BFF_PREFIX.length());
        String query = request.getQueryString();
        String target = MQ_ADMIN_PREFIX + path + (query != null ? "?" + query : "");

        return client.forward(HttpMethod.valueOf(request.getMethod()), target, body);
    }
}
