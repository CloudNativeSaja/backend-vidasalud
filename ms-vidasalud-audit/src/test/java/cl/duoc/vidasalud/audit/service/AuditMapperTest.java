package cl.duoc.vidasalud.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import cl.duoc.vidasalud.audit.event.AppointmentEvent;
import cl.duoc.vidasalud.audit.event.AppointmentStatus;
import cl.duoc.vidasalud.audit.event.AuditEvent;

class AuditMapperTest {

    private final AuditMapper mapper = new AuditMapper();

    @Test
    void traduceCambioDeEstadoConTrazabilidad() {
        Instant at = Instant.parse("2026-11-22T15:00:00Z");
        AppointmentEvent event = new AppointmentEvent("e-1", AppointmentEvent.Type.APPOINTMENT_STATUS_CHANGED,
                at, 42L, "12345678-9", 3L, 7L, AppointmentStatus.SOLICITADA, AppointmentStatus.CONFIRMADA);

        AuditEvent audit = mapper.fromAppointment(event);

        assertEquals("e-1", audit.traceId());
        assertEquals("cita-42", audit.correlationId());
        assertEquals(AuditEvent.Categoria.CITAS, audit.categoria());
        assertEquals("CITA_CONFIRMADA", audit.accion());
        assertEquals("paciente:12345678-9", audit.usuario());
        assertEquals(at, audit.ocurridoEn());
        assertEquals("SOLICITADA", audit.payload().get("estadoAnterior"));
        assertEquals("CONFIRMADA", audit.payload().get("estadoNuevo"));
    }

    @Test
    void creacionSinPacienteQuedaComoSistema() {
        AppointmentEvent event = new AppointmentEvent("e-2", AppointmentEvent.Type.APPOINTMENT_CREATED,
                Instant.now(), 1L, null, 3L, null, null, AppointmentStatus.SOLICITADA);

        AuditEvent audit = mapper.fromAppointment(event);

        assertEquals("CITA_CREADA", audit.accion());
        assertEquals("sistema:ms-vidasalud-appointments", audit.usuario());
        assertNull(audit.payload().get("estadoAnterior"));
    }

    @Test
    void elMismoEventoDeOrigenGeneraElMismoIdDeAuditoria() {
        AppointmentEvent event = new AppointmentEvent("e-3", AppointmentEvent.Type.APPOINTMENT_CREATED,
                Instant.now(), 1L, "p", 3L, null, null, AppointmentStatus.SOLICITADA);

        assertEquals(mapper.fromAppointment(event).eventId(), mapper.fromAppointment(event).eventId());
    }
}
