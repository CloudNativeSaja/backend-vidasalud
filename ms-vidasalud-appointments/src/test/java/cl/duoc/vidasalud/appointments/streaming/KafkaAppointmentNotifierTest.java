package cl.duoc.vidasalud.appointments.streaming;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import cl.duoc.vidasalud.appointments.entity.Appointment;
import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;
import cl.duoc.vidasalud.appointments.streaming.config.StreamingProperties;

/**
 * Verifica que cada hecho de una cita se publique en el topico configurado,
 * con el id de la cita como clave, y que un fallo de Kafka nunca llegue al
 * servicio de negocio.
 */
class KafkaAppointmentNotifierTest {

    private static final String TOPIC = "vidasalud.appointments.events";

    private KafkaTemplate<String, AppointmentEvent> kafkaTemplate;
    private KafkaAppointmentNotifier notifier;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        StreamingProperties properties = new StreamingProperties(
                true, new StreamingProperties.Topics(TOPIC));
        notifier = new KafkaAppointmentNotifier(kafkaTemplate, properties);
    }

    private Appointment appointment(AppointmentStatus status) {
        Appointment a = new Appointment();
        ReflectionTestUtils.setField(a, "id", 42L);
        a.setPatientId("12345678-9");
        a.setServiceId(3L);
        a.setBoxId(7L);
        a.setStatus(status);
        return a;
    }

    @Test
    void creacionPublicaEventoCreadoConClaveDeLaCita() {
        when(kafkaTemplate.send(anyString(), anyString(), any(AppointmentEvent.class)))
                .thenReturn(new CompletableFuture<SendResult<String, AppointmentEvent>>());

        notifier.appointmentCreated(appointment(AppointmentStatus.SOLICITADA));

        ArgumentCaptor<AppointmentEvent> captor = ArgumentCaptor.forClass(AppointmentEvent.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq("42"), captor.capture());
        AppointmentEvent event = captor.getValue();
        assertEquals(AppointmentEvent.Type.APPOINTMENT_CREATED, event.type());
        assertEquals(AppointmentStatus.SOLICITADA, event.newStatus());
        assertNull(event.previousStatus());
    }

    @Test
    void cambioDeEstadoPublicaEstadoAnteriorYNuevo() {
        when(kafkaTemplate.send(anyString(), anyString(), any(AppointmentEvent.class)))
                .thenReturn(new CompletableFuture<SendResult<String, AppointmentEvent>>());

        notifier.statusChanged(appointment(AppointmentStatus.CONFIRMADA), AppointmentStatus.SOLICITADA);

        ArgumentCaptor<AppointmentEvent> captor = ArgumentCaptor.forClass(AppointmentEvent.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq("42"), captor.capture());
        AppointmentEvent event = captor.getValue();
        assertEquals(AppointmentEvent.Type.APPOINTMENT_STATUS_CHANGED, event.type());
        assertEquals(AppointmentStatus.SOLICITADA, event.previousStatus());
        assertEquals(AppointmentStatus.CONFIRMADA, event.newStatus());
    }

    @Test
    void siKafkaLanzaExcepcionNoSePropaga() {
        when(kafkaTemplate.send(anyString(), anyString(), any(AppointmentEvent.class)))
                .thenThrow(new IllegalStateException("Kafka caido"));

        assertDoesNotThrow(() -> notifier.appointmentCreated(appointment(AppointmentStatus.SOLICITADA)));
    }

    @Test
    void siElEnvioFallaDeFormaAsincronaNoSePropaga() {
        CompletableFuture<SendResult<String, AppointmentEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(anyString(), anyString(), any(AppointmentEvent.class))).thenReturn(future);

        assertDoesNotThrow(() -> {
            notifier.statusChanged(appointment(AppointmentStatus.EN_ESPERA), AppointmentStatus.CONFIRMADA);
            future.completeExceptionally(new RuntimeException("sin brokers"));
        });
    }
}
