package cl.duoc.vidasalud.bff.controller;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.vidasalud.bff.client.AuditClient;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Linea de tiempo de auditoria armada desde Kafka.
 *
 * Flujo: Angular -> API Gateway (JWT) -> BFF (rol ADMIN o AUDITOR) -> ms-vidasalud-audit
 *
 * /api/bff/audit/events -> /api/audit/events
 */
@RestController
@RequestMapping("/api/bff/audit")
@PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
public class BffAuditController {

    private static final String BFF_PREFIX = "/api/bff/audit";
    private static final String TARGET_PREFIX = "/api/audit";

    private final AuditClient client;

    public BffAuditController(AuditClient client) {
        this.client = client;
    }

    /** Solo lectura: la auditoria no se modifica desde la API. */
    @GetMapping("/**")
    public ResponseEntity<String> proxy(HttpServletRequest request) {
        return client.forward(HttpMethod.GET, ProxyPaths.target(request, BFF_PREFIX, TARGET_PREFIX), null);
    }
}
