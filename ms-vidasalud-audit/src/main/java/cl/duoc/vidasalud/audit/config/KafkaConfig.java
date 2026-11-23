package cl.duoc.vidasalud.audit.config;

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
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import cl.duoc.vidasalud.audit.exception.InvalidEventException;

/**
 * Infraestructura Kafka del servicio, separada de la logica de negocio.
 *
 * - Declara el topico de auditoria (este servicio es su productor) y las DLT.
 * - Errores transitorios: reintentos con espera exponencial.
 * - Eventos invalidos o JSON ilegible: directo a la DLT del topico de origen.
 */
@Configuration
@EnableConfigurationProperties(AuditKafkaProperties.class)
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    /** Topico de auditoria: retencion larga (30 dias) para trazabilidad. */
    @Bean
    public KafkaAdmin.NewTopics auditTopics(AuditKafkaProperties properties) {
        AuditKafkaProperties.AuditTopic audit = properties.auditTopic();
        AuditKafkaProperties.DeadLetter dlt = properties.deadLetter();

        NewTopic auditEvents = TopicBuilder.name(properties.topics().auditEvents())
                .partitions(audit.partitions())
                .replicas(audit.replicationFactor())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(audit.retention().toMillis()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, audit.minInsyncReplicas())
                .build();

        return new KafkaAdmin.NewTopics(
                auditEvents,
                deadLetterTopic(dlt.appointmentsTopic(), dlt),
                deadLetterTopic(dlt.auditTopic(), dlt));
    }

    private static NewTopic deadLetterTopic(String name, AuditKafkaProperties.DeadLetter dlt) {
        return TopicBuilder.name(name)
                .partitions(dlt.partitions())
                .replicas(dlt.replicationFactor())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(dlt.retention().toMillis()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            DefaultKafkaProducerFactory<Object, Object> producerFactory,
            KafkaTemplate<Object, Object> jsonTemplate,
            AuditKafkaProperties properties) {

        KafkaTemplate<Object, Object> bytesTemplate = new KafkaTemplate<>(
                producerFactory.copyWithConfigurationOverride(
                        Map.of(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class)));

        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, bytesTemplate);
        templates.put(Object.class, jsonTemplate);

        String appointmentsTopic = properties.topics().appointmentsEvents();
        AuditKafkaProperties.DeadLetter dlt = properties.deadLetter();

        return new DeadLetterPublishingRecoverer(templates,
                (record, exception) -> {
                    String target = record.topic().equals(appointmentsTopic)
                            ? dlt.appointmentsTopic()
                            : dlt.auditTopic();
                    log.error("Evento enviado a la DLT {}: topico={} particion={} offset={} causa={}",
                            target, record.topic(), record.partition(), record.offset(),
                            exception.getMessage());
                    return new TopicPartition(target, record.partition());
                });
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            DeadLetterPublishingRecoverer recoverer,
            AuditKafkaProperties properties) {

        AuditKafkaProperties.Retry retry = properties.retry();
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(retry.maxRetries());
        backOff.setInitialInterval(retry.initialInterval().toMillis());
        backOff.setMultiplier(retry.multiplier());
        backOff.setMaxInterval(retry.maxInterval().toMillis());

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(InvalidEventException.class);
        handler.setRetryListeners((record, exception, attempt) ->
                log.warn("Reintento {} del evento topico={} particion={} offset={}: {}",
                        attempt, record.topic(), record.partition(), record.offset(),
                        exception.getMessage()));
        return handler;
    }
}
