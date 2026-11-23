package cl.duoc.vidasalud.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cl.duoc.vidasalud.report.dto.KpiResponse;
import cl.duoc.vidasalud.report.dto.ReportRange;
import cl.duoc.vidasalud.report.dto.TopServiceResponse;
import cl.duoc.vidasalud.report.event.AppointmentEvent;
import cl.duoc.vidasalud.report.event.AppointmentStatus;

class KpiServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-22T15:00:00Z");

    private KpiService service;

    @BeforeEach
    void setUp() {
        ServiceNameResolver names = mock(ServiceNameResolver.class);
        when(names.nameOf(anyLong())).thenAnswer(inv -> "Prestacion " + inv.getArgument(0));
        service = new KpiService(Clock.fixed(NOW, ZoneOffset.UTC), names);
    }

    private static AppointmentEvent created(long id, long serviceId, Instant at) {
        return new AppointmentEvent(UUID.randomUUID().toString(), AppointmentEvent.Type.APPOINTMENT_CREATED,
                at, id, "p-" + id, serviceId, 1L, null, AppointmentStatus.SOLICITADA);
    }

    private static AppointmentEvent changed(long id, long serviceId, AppointmentStatus from,
                                            AppointmentStatus to, Instant at) {
        return new AppointmentEvent(UUID.randomUUID().toString(), AppointmentEvent.Type.APPOINTMENT_STATUS_CHANGED,
                at, id, "p-" + id, serviceId, 1L, from, to);
    }

    @Test
    void calculaTotalesEstadosCancelacionYEspera() {
        Instant t = NOW.minusSeconds(3600);
        service.apply(created(1, 10, t));
        service.apply(changed(1, 10, AppointmentStatus.SOLICITADA, AppointmentStatus.CONFIRMADA, t.plusSeconds(60)));
        service.apply(changed(1, 10, AppointmentStatus.CONFIRMADA, AppointmentStatus.EN_ESPERA, t.plusSeconds(120)));
        service.apply(changed(1, 10, AppointmentStatus.EN_ESPERA, AppointmentStatus.EN_ATENCION, t.plusSeconds(720)));
        service.apply(created(2, 10, t));
        service.apply(changed(2, 10, AppointmentStatus.SOLICITADA, AppointmentStatus.CANCELADA, t.plusSeconds(30)));
        service.apply(created(3, 20, t));
        service.apply(created(4, 30, t));

        KpiResponse kpis = service.kpis(ReportRange.LAST_24H);

        assertEquals("last24h", kpis.range());
        assertEquals(4, kpis.totalAppointments());
        assertEquals(1, kpis.byStatus().get("EN_ATENCION"));
        assertEquals(1, kpis.byStatus().get("CANCELADA"));
        assertEquals(2, kpis.byStatus().get("SOLICITADA"));
        assertEquals(0.25, kpis.cancellationRate());
        // EN_ESPERA -> EN_ATENCION: 600 segundos = 10 minutos
        assertEquals(10.0, kpis.averageWaitMinutes());
        assertEquals(t.plusSeconds(720), kpis.lastEventAt());
    }

    @Test
    void descartaEventosDuplicados() {
        AppointmentEvent event = created(1, 10, NOW.minusSeconds(60));

        assertTrue(service.apply(event));
        assertFalse(service.apply(event));
        assertEquals(1, service.kpis(ReportRange.LAST_24H).totalAppointments());
    }

    @Test
    void respetaElRangoDeTiempo() {
        service.apply(created(1, 10, NOW.minusSeconds(3600)));
        service.apply(created(2, 10, NOW.minusSeconds(3 * 24 * 3600)));

        assertEquals(1, service.kpis(ReportRange.LAST_24H).totalAppointments());
        assertEquals(2, service.kpis(ReportRange.LAST_7D).totalAppointments());
    }

    @Test
    void rankingDePrestacionesOrdenadoConNombre() {
        Instant t = NOW.minusSeconds(60);
        service.apply(created(1, 10, t));
        service.apply(created(2, 20, t));
        service.apply(created(3, 20, t));

        List<TopServiceResponse> top = service.topServices(ReportRange.LAST_7D);

        assertEquals(2, top.size());
        assertEquals(new TopServiceResponse(20L, "Prestacion 20", 2), top.get(0));
        assertEquals(new TopServiceResponse(10L, "Prestacion 10", 1), top.get(1));
    }

    @Test
    void sinEventosTodoEnCero() {
        KpiResponse kpis = service.kpis(ReportRange.LAST_30D);

        assertEquals(0, kpis.totalAppointments());
        assertEquals(0.0, kpis.cancellationRate());
        assertEquals(0.0, kpis.averageWaitMinutes());
        assertEquals(6, kpis.byStatus().size());
    }
}
