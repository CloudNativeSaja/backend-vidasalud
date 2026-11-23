package cl.duoc.vidasalud.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import cl.duoc.vidasalud.audit.event.AuditEvent;

class AuditTimelineServiceTest {

    private static AuditEvent event(String id, AuditEvent.Categoria categoria, Instant at) {
        return new AuditEvent(id, "t-" + id, "cita-1", categoria, "CITA_CREADA", "sistema", at, Map.of());
    }

    @Test
    void ordenaDelMasRecienteAlMasAntiguoYFiltraPorCategoria() {
        AuditTimelineService timeline = new AuditTimelineService();
        Instant t = Instant.parse("2026-11-22T15:00:00Z");
        timeline.add(event("a", AuditEvent.Categoria.CITAS, t));
        timeline.add(event("b", AuditEvent.Categoria.CATALOGO, t.plusSeconds(10)));
        timeline.add(event("c", AuditEvent.Categoria.CITAS, t.plusSeconds(20)));

        List<AuditEvent> todos = timeline.list(null, 10);
        List<AuditEvent> citas = timeline.list(AuditEvent.Categoria.CITAS, 10);

        assertEquals(List.of("c", "b", "a"), todos.stream().map(AuditEvent::eventId).toList());
        assertEquals(List.of("c", "a"), citas.stream().map(AuditEvent::eventId).toList());
        assertEquals(1, timeline.list(null, 1).size());
    }

    @Test
    void descartaDuplicados() {
        AuditTimelineService timeline = new AuditTimelineService();
        AuditEvent e = event("a", AuditEvent.Categoria.CITAS, Instant.now());

        assertTrue(timeline.add(e));
        assertFalse(timeline.add(e));
        assertEquals(1, timeline.list(null, 10).size());
    }

    @Test
    void conservaSoloLosUltimosEventos() {
        AuditTimelineService timeline = new AuditTimelineService();
        Instant t = Instant.parse("2026-11-22T15:00:00Z");
        for (int i = 0; i < AuditTimelineService.MAX_EVENTS + 5; i++) {
            timeline.add(event("e" + i, AuditEvent.Categoria.CITAS, t.plusSeconds(i)));
        }

        assertEquals(AuditTimelineService.MAX_EVENTS, timeline.list(null, 5_000).size());
    }
}
