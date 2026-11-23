package cl.duoc.vidasalud.bff.controller;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.vidasalud.bff.client.ReportClient;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Indicadores de reporteria calculados desde Kafka.
 *
 * Flujo: Angular -> API Gateway (JWT) -> BFF (rol ADMIN) -> ms-vidasalud-report
 *
 * /api/bff/report/kpis          -> /api/report/kpis
 * /api/bff/report/top-services  -> /api/report/top-services
 */
@RestController
@RequestMapping("/api/bff/report")
@PreAuthorize("hasRole('ADMIN')")
public class BffReportController {

    private static final String BFF_PREFIX = "/api/bff/report";
    private static final String TARGET_PREFIX = "/api/report";

    private final ReportClient client;

    public BffReportController(ReportClient client) {
        this.client = client;
    }

    /** Solo lectura: el servicio de reportes unicamente expone GET. */
    @GetMapping("/**")
    public ResponseEntity<String> proxy(HttpServletRequest request) {
        return client.forward(HttpMethod.GET, ProxyPaths.target(request, BFF_PREFIX, TARGET_PREFIX), null);
    }
}
