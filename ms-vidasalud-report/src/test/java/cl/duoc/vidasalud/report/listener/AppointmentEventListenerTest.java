package cl.duoc.vidasalud.report.listener;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cl.duoc.vidasalud.report.event.AppointmentEvent;
import cl.duoc.vidasalud.report.event.AppointmentStatus;
import cl.duoc.vidasalud.report.exception.InvalidEventException;
import cl.duoc.vidasalud.report.service.KpiService;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class AppointmentEventListenerTest {

    private ValidatorFactory validatorFactory;
    private KpiService kpiService;
    private AppointmentEventListener listener;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        kpiService = mock(KpiService.class);
        listener = new AppointmentEventListener(kpiService, validatorFactory.getValidator());
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    private static ConsumerRecord<String, AppointmentEvent> record(AppointmentEvent event) {
        return new ConsumerRecord<>("vidasalud.appointments.events", 0, 5L, "42", event);
    }

    @Test
    void eventoValidoSeDelegaAlServicio() {
        AppointmentEvent event = new AppointmentEvent("e-1", AppointmentEvent.Type.APPOINTMENT_CREATED,
                Instant.now(), 42L, "p", 3L, 7L, null, AppointmentStatus.SOLICITADA);
        when(kpiService.apply(any())).thenReturn(true);

        listener.onAppointmentEvent(record(event));

        verify(kpiService).apply(event);
    }

    @Test
    void eventoSinCamposObligatoriosEsInvalido() {
        AppointmentEvent event = new AppointmentEvent("", null, null, null, null, null, null, null, null);

        assertThrows(InvalidEventException.class, () -> listener.onAppointmentEvent(record(event)));
        verify(kpiService, never()).apply(any());
    }

    @Test
    void mensajeVacioEsInvalido() {
        assertThrows(InvalidEventException.class, () -> listener.onAppointmentEvent(record(null)));
    }
}
