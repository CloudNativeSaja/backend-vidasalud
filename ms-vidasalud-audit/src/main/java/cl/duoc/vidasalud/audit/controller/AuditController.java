package cl.duoc.vidasalud.audit.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.vidasalud.audit.event.AuditEvent;
import cl.duoc.vidasalud.audit.exception.InvalidRequestException;
import cl.duoc.vidasalud.audit.service.AuditTimelineService;

/**
 * Linea de tiempo de auditoria. Solo es accesible a traves del BFF
 * (/api/bff/audit/** con rol ADMIN o AUDITOR).
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private static final int MAX_LIMIT = 500;

    private final AuditTimelineService timeline;

    public AuditController(AuditTimelineService timeline) {
        this.timeline = timeline;
    }

    /** GET /api/audit/events?categoria=CITAS&limit=100 */
    @GetMapping("/events")
    public List<AuditEvent> events(
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "100") int limit) {

        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidRequestException("limit debe estar entre 1 y " + MAX_LIMIT);
        }
        return timeline.list(parse(categoria), limit);
    }

    private static AuditEvent.Categoria parse(String categoria) {
        if (categoria == null || categoria.isBlank() || "TODOS".equalsIgnoreCase(categoria)) {
            return null;
        }
        return Arrays.stream(AuditEvent.Categoria.values())
                .filter(c -> c.name().equalsIgnoreCase(categoria))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Categoria invalida '" + categoria + "'. Use CITAS, CATALOGO, MENSAJERIA o ACCESO"));
    }
}
