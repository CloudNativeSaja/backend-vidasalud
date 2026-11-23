package cl.duoc.vidasalud.appointments.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cl.duoc.vidasalud.appointments.entity.Appointment;
import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;

class CompositeAppointmentNotifierTest {

    @Test
    void avisaATodosLosCanales() {
        AppointmentNotifier rabbit = mock(AppointmentNotifier.class);
        AppointmentNotifier kafka = mock(AppointmentNotifier.class);
        Appointment appointment = new Appointment();
        CompositeAppointmentNotifier composite = new CompositeAppointmentNotifier(List.of(rabbit, kafka));

        composite.appointmentCreated(appointment);
        composite.statusChanged(appointment, AppointmentStatus.SOLICITADA);

        verify(rabbit).appointmentCreated(appointment);
        verify(kafka).appointmentCreated(appointment);
        verify(rabbit).statusChanged(appointment, AppointmentStatus.SOLICITADA);
        verify(kafka).statusChanged(appointment, AppointmentStatus.SOLICITADA);
    }

    @Test
    void siUnCanalFallaLosDemasIgualRecibenElAviso() {
        AppointmentNotifier rabbit = mock(AppointmentNotifier.class);
        AppointmentNotifier kafka = mock(AppointmentNotifier.class);
        Appointment appointment = new Appointment();
        doThrow(new IllegalStateException("RabbitMQ caido")).when(rabbit).appointmentCreated(any());
        CompositeAppointmentNotifier composite = new CompositeAppointmentNotifier(List.of(rabbit, kafka));

        assertDoesNotThrow(() -> composite.appointmentCreated(appointment));

        verify(kafka).appointmentCreated(appointment);
    }

    @Configuration
    static class Canales {
        @Bean
        AppointmentNotifier canalA() {
            return mock(AppointmentNotifier.class);
        }

        @Bean
        AppointmentNotifier canalB() {
            return mock(AppointmentNotifier.class);
        }
    }

    /**
     * En Spring el servicio recibe un unico AppointmentNotifier: debe ser el
     * compuesto (@Primary) y este no debe incluirse a si mismo como canal.
     */
    @Test
    void elCompuestoEsElPrimarioYNoSeIncluyeASiMismo() {
        new ApplicationContextRunner()
                .withUserConfiguration(Canales.class, CompositeAppointmentNotifier.class)
                .run(context -> {
                    assertEquals(3, context.getBeansOfType(AppointmentNotifier.class).size());
                    AppointmentNotifier primario = context.getBean(AppointmentNotifier.class);
                    assertSame(context.getBean(CompositeAppointmentNotifier.class), primario);
                });
    }
}
