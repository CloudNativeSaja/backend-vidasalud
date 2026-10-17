package cl.duoc.vidasalud.appointments.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cl.duoc.vidasalud.appointments.entity.Appointment;
import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;
import cl.duoc.vidasalud.appointments.messaging.config.MessagingProperties;

/**
 * Verifica que cada cambio de estado publique en el exchange y routing key correctos.
 */
class RabbitAppointmentNotifierTest {

    private CommandPublisher publisher;
    private RabbitAppointmentNotifier notifier;

    @BeforeEach
    void setUp() {
        publisher = mock(CommandPublisher.class);
        MessagingProperties properties = new MessagingProperties(
                true,
                "ms-vidasalud-appointments",
                new MessagingProperties.Exchanges("cmd.direct", "cmd.topic"),
                new MessagingProperties.Publishing(
                        "email.send", "email.requested", "email.called", "email.closed", "email.cancelled",
                        "admission.ticket", "admission.call.box",
                        "record.gen", "record.summary"));
        notifier = new RabbitAppointmentNotifier(publisher, properties);
    }

    @Test
    void creacionPublicaEmailPorTopic() {
        notifier.appointmentCreated(appointment(AppointmentStatus.SOLICITADA));

        verify(publisher).publish(eq("cmd.topic"), eq("email.requested"),
                eq(CommandType.EMAIL_SOLICITUD_RECIBIDA), anyString(), anyString(), anyMap());
    }

    @Test
    void confirmadaPublicaEmailYComprobantePorDirect() {
        notifier.statusChanged(appointment(AppointmentStatus.CONFIRMADA), AppointmentStatus.SOLICITADA);

        verify(publisher).publish(eq("cmd.direct"), eq("email.send"),
                eq(CommandType.EMAIL_CITA_CONFIRMADA), anyString(), anyString(), anyMap());
        verify(publisher).publish(eq("cmd.direct"), eq("record.gen"),
                eq(CommandType.RECORD_COMPROBANTE), anyString(), anyString(), anyMap());
    }

    @Test
    void enAtencionPublicaLlamadoABoxPorTopic() {
        notifier.statusChanged(appointment(AppointmentStatus.EN_ATENCION), AppointmentStatus.EN_ESPERA);

        verify(publisher).publish(eq("cmd.topic"), eq("admission.call.box"),
                eq(CommandType.ADMISSION_LLAMADO_BOX), anyString(), anyString(), anyMap());
    }

    @Test
    void solicitadaNoPublicaComandosDeCambioDeEstado() {
        notifier.statusChanged(appointment(AppointmentStatus.SOLICITADA), null);

        verify(publisher, never()).publish(anyString(), anyString(), any(), anyString(), anyString(), anyMap());
    }

    private static Appointment appointment(AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setPatientId("paciente-001");
        appointment.setServiceId(1L);
        appointment.setBoxId(2L);
        appointment.setAppointmentDate(LocalDateTime.now().plusDays(1));
        appointment.setStatus(status);
        return appointment;
    }
}
