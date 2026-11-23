package cl.duoc.vidasalud.audit.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import cl.duoc.vidasalud.audit.event.AuditEvent;

/**
 * Linea de tiempo de auditoria en memoria (los mas recientes primero).
 *
 * Se reconstruye al reiniciar: el consumer group relee el topico de
 * auditoria, que conserva 30 dias de eventos.
 */
@Service
public class AuditTimelineService {

    static final int MAX_EVENTS = 1_000;

    private final Deque<AuditEvent> events = new ArrayDeque<>();
    private final Set<String> ids = new HashSet<>();

    /** @return false si el evento ya estaba registrado (duplicado) */
    public synchronized boolean add(AuditEvent event) {
        if (!ids.add(event.eventId())) {
            return false;
        }
        events.addFirst(event);
        if (events.size() > MAX_EVENTS) {
            AuditEvent oldest = events.removeLast();
            ids.remove(oldest.eventId());
        }
        return true;
    }

    public synchronized List<AuditEvent> list(AuditEvent.Categoria categoria, int limit) {
        return events.stream()
                .filter(e -> categoria == null || e.categoria() == categoria)
                .sorted((a, b) -> b.ocurridoEn().compareTo(a.ocurridoEn()))
                .limit(limit)
                .toList();
    }
}
