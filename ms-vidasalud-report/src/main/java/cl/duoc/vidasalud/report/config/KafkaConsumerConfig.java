package cl.duoc.vidasalud.report.config;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import cl.duoc.vidasalud.report.exception.InvalidEventException;

/**
 * Infraestructura de mensajeria del consumidor, separada de la logica de
 * negocio (KpiService).
 *
 * Manejo de errores:
 * - Error transitorio: hasta N reintentos con espera exponencial.
 * - Evento invalido (validacion) o JSON ilegible (deserializacion): no se
 *   reintenta, va directo a la DLT.
 * - Agotados los reintentos: el mensaje se publica en la DLT con cabeceras
 *   que explican la causa, y el consumidor sigue con el siguiente offset.
 *
 * Spring Boot aplica automaticamente el DefaultErrorHandler a todos los
 * @KafkaListener de este servicio.
 */
@Configuration
@EnableConfigurationProperties(ReportKafkaProperties.class)
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** DLT de este consumer group (se crea al iniciar si no existe). */
    @Bean
    public NewTopic deadLetterTopic(ReportKafkaProperties properties) {
        ReportKafkaProperties.DeadLetter dlt = properties.deadLetter();
        return TopicBuilder.name(dlt.topic())
                .partitions(dlt.partitions())
                .replicas(dlt.replicationFactor())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(dlt.retention().toMillis()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    /**
     * Publica en la DLT. Si el mensaje no se pudo deserializar se reenvian los
     * bytes originales; si no, el evento como JSON.
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            DefaultKafkaProducerFactory<Object, Object> producerFactory,
            KafkaTemplate<Object, Object> jsonTemplate,
            ReportKafkaProperties properties) {

        KafkaTemplate<Object, Object> bytesTemplate = new KafkaTemplate<>(
                producerFactory.copyWithConfigurationOverride(
                        Map.of(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class)));

        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, bytesTemplate);
        templates.put(Object.class, jsonTemplate);

        String deadLetterTopic = properties.deadLetter().topic();
        return new DeadLetterPublishingRecoverer(templates,
                (record, exception) -> {
                    log.error("Evento enviado a la DLT {}: topico={} particion={} offset={} causa={}",
                            deadLetterTopic, record.topic(), record.partition(), record.offset(),
                            exception.getMessage());
                    return new TopicPartition(deadLetterTopic, record.partition());
                });
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            DeadLetterPublishingRecoverer recoverer,
            ReportKafkaProperties properties) {

        ReportKafkaProperties.Retry retry = properties.retry();
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(retry.maxRetries());
        backOff.setInitialInterval(retry.initialInterval().toMillis());
        backOff.setMultiplier(retry.multiplier());
        backOff.setMaxInterval(retry.maxInterval().toMillis());

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        // Un evento invalido no mejora al reintentarlo
        handler.addNotRetryableExceptions(InvalidEventException.class);
        handler.setRetryListeners((record, exception, attempt) ->
                log.warn("Reintento {} del evento topico={} particion={} offset={}: {}",
                        attempt, record.topic(), record.partition(), record.offset(),
                        exception.getMessage()));
        return handler;
    }
}
