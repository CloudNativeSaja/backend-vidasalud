package cl.duoc.vidasalud.audit.service;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import cl.duoc.vidasalud.audit.config.AuditKafkaProperties;
import cl.duoc.vidasalud.audit.event.AuditEvent;

/**
 * Productor del topico de auditoria.
 *
 * Espera la confirmacion del broker (acks=all): si la publicacion falla se
 * lanza una excepcion, el consumidor que llamo reintenta y, agotados los
 * reintentos, el evento de origen va a la DLT. Asi no se pierde auditoria.
 */
@Component
public class AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuditPublisher.class);
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);

    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;
    private final String topic;

    public AuditPublisher(KafkaTemplate<String, AuditEvent> kafkaTemplate, AuditKafkaProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = properties.topics().auditEvents();
    }

    public void publish(AuditEvent event) {
        try {
            SendResult<String, AuditEvent> result = kafkaTemplate
                    .send(topic, event.correlationId(), event)
                    .get(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            log.info("Auditoria publicada: {} {} en {} particion {} offset {}",
                    event.accion(), event.correlationId(), topic,
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Publicacion de auditoria interrumpida", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("No se pudo publicar la auditoria " + event.eventId()
                    + " en " + topic + ": " + e.getMessage(), e);
        }
    }
}
