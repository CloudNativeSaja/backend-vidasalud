package cl.duoc.vidasalud.notify.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.rabbitmq.client.Channel;

import cl.duoc.vidasalud.notify.config.MessagingProperties;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pruebas de la politica ACK / reintento / NACK -> DLQ.
 */
class MessageAckHandlerTest {

    private static final long TAG = 7L;
    private static final String QUEUE = "q.cmd.email";
    private static final String VALID_JSON = """
            {"eventId":"evt-1","type":"EMAIL_CITA_CONFIRMADA","timestamp":"2026-10-16T12:00:00Z",
             "traceId":"t-1","correlationId":"appointment-1","source":"test",
             "payload":{"appointmentId":"1","patientId":"paciente-001"}}
            """;

    private Channel channel;
    private RabbitTemplate rabbitTemplate;
    private MessageAckHandler handler;

    @BeforeEach
    void setUp() {
        channel = mock(Channel.class);
        rabbitTemplate = mock(RabbitTemplate.class);

        MessagingProperties properties = new MessagingProperties(
                new MessagingProperties.Exchanges("cmd.direct", "cmd.topic", "cmd.dead.dlx"),
                "classic",
                604800000L,
                null,
                new MessagingProperties.Consumer(3, 0, 1000));

        handler = new MessageAckHandler(
                JsonMapper.builder().build(),
                rabbitTemplate,
                new ProcessedEventRegistry(properties),
                properties);
    }

    @Test
    void procesamientoExitosoHaceAck() throws Exception {
        handler.process("EMAIL", message(VALID_JSON, null), channel, command -> { });

        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(TAG, false, false);
    }

    @Test
    void jsonInvalidoVaDirectoADlq() throws Exception {
        handler.process("EMAIL", message("esto-no-es-json", null), channel, command -> { });

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(TAG, false);
    }

    @Test
    void errorNoRecuperableVaDirectoADlq() throws Exception {
        handler.process("EMAIL", message(VALID_JSON, null), channel, command -> {
            throw new InvalidCommandException("dato invalido");
        });

        verify(channel).basicNack(TAG, false, false);
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    void errorRecuperableSeReintentaConHeaderIncrementado() throws Exception {
        handler.process("EMAIL", message(VALID_JSON, null), channel, command -> {
            throw new RecoverableProcessingException("proveedor caido");
        });

        ArgumentCaptor<Message> retried = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(""), eq(QUEUE), retried.capture());
        assertThat((Object) retried.getValue().getMessageProperties()
                .getHeaders().get(MessageAckHandler.RETRY_COUNT_HEADER)).isEqualTo(1);
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(TAG, false, false);
    }

    @Test
    void errorRecuperableEnUltimoIntentoVaADlq() throws Exception {
        // x-retry-count = 2 -> este es el intento 3 de 3
        handler.process("EMAIL", message(VALID_JSON, 2), channel, command -> {
            throw new RecoverableProcessingException("proveedor caido");
        });

        verify(channel).basicNack(TAG, false, false);
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    void mensajeDuplicadoSeConfirmaSinReprocesar() throws Exception {
        AtomicInteger executions = new AtomicInteger();

        handler.process("EMAIL", message(VALID_JSON, null), channel, command -> executions.incrementAndGet());
        handler.process("EMAIL", message(VALID_JSON, null), channel, command -> executions.incrementAndGet());

        assertThat(executions.get()).isEqualTo(1);
    }

    private static Message message(String body, Integer retryCount) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(TAG);
        props.setConsumerQueue(QUEUE);
        if (retryCount != null) {
            props.setHeader(MessageAckHandler.RETRY_COUNT_HEADER, retryCount);
        }
        return new Message(body.getBytes(StandardCharsets.UTF_8), props);
    }
}
