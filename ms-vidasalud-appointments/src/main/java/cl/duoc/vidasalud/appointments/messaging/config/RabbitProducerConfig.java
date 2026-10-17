package cl.duoc.vidasalud.appointments.messaging.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion RabbitMQ del productor (ms-vidasalud-appointments).
 *
 * - Declara los exchanges donde publica (idempotente: si ya existen con los
 *   mismos argumentos, RabbitMQ no hace nada). Las colas y DLQ las declara
 *   ms-vidasalud-notify, que es su dueno.
 * - Configura el RabbitTemplate con publisher confirms y returns para saber
 *   si cada comando llego al broker y si tuvo una cola de destino.
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitProducerConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitProducerConfig.class);

    private final MessagingProperties properties;

    public RabbitProducerConfig(MessagingProperties properties) {
        this.properties = properties;
    }

    /** Exchange direct (cmd.direct): routing key exacta. */
    @Bean
    public DirectExchange commandDirectExchange() {
        return new DirectExchange(properties.exchanges().direct(), true, false);
    }

    /** Exchange topic (cmd.topic): routing por patron. */
    @Bean
    public TopicExchange commandTopicExchange() {
        return new TopicExchange(properties.exchanges().topic(), true, false);
    }

    /**
     * RabbitTemplate con:
     * - mandatory=true: si ningun binding coincide, el broker devuelve el mensaje
     * - ConfirmCallback: ack/nack del broker por cada publicacion
     * - ReturnsCallback: registra los mensajes sin cola de destino
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);

        template.setConfirmCallback((correlationData, ack, cause) -> {
            String eventId = correlationData != null ? correlationData.getId() : "desconocido";
            if (ack) {
                log.info("[PUBLISHER] Broker confirmo el mensaje | eventId={}", eventId);
            } else {
                log.error("[PUBLISHER] Broker rechazo el mensaje | eventId={} causa={}", eventId, cause);
            }
        });

        template.setReturnsCallback(returned -> log.error(
                "[PUBLISHER] Mensaje sin cola de destino | exchange={} routingKey={} motivo={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));

        return template;
    }
}
