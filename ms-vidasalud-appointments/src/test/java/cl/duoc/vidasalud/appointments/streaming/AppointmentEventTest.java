package cl.duoc.vidasalud.appointments.streaming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import cl.duoc.vidasalud.appointments.entity.Appointment;
import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;

class AppointmentEventTest {

    private Appointment appointment(AppointmentStatus status) {
        Appointment a = new Appointment();
        ReflectionTestUtils.setField(a, "id", 42L); // el id lo asigna JPA
        a.setPatientId("12345678-9");
        a.setServiceId(3L);
        a.setBoxId(7L);
        a.setStatus(status);
        return a;
    }

    @Test
    void createdEventUsesAppointmentIdAsKey() {
        AppointmentEvent event = AppointmentEvent.created(appointment(AppointmentStatus.SOLICITADA));

        assertEquals(AppointmentEvent.Type.APPOINTMENT_CREATED, event.type());
        assertEquals("42", event.key());
        assertNull(event.previousStatus());
        assertEquals(AppointmentStatus.SOLICITADA, event.newStatus());
    }

    @Test
    void statusChangedEventKeepsBothStatesAndUniqueIds() {
        Appointment a = appointment(AppointmentStatus.CONFIRMADA);

        AppointmentEvent first = AppointmentEvent.statusChanged(a, AppointmentStatus.SOLICITADA);
        AppointmentEvent second = AppointmentEvent.statusChanged(a, AppointmentStatus.SOLICITADA);

        assertEquals(AppointmentStatus.SOLICITADA, first.previousStatus());
        assertEquals(AppointmentStatus.CONFIRMADA, first.newStatus());
        assertNotEquals(first.eventId(), second.eventId());
    }
}
