package cl.duoc.vidasalud.audit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import cl.duoc.vidasalud.audit.event.AuditEvent;
import cl.duoc.vidasalud.audit.service.AuditTimelineService;

/**
 * Flujo completo por los dos topicos contra un broker Kafka embebido:
 * evento de cita -> (grupo auditoria) -> vidasalud.audit.events
 * -> (grupo auditoria-timeline) -> linea de tiempo.
 * Un evento invalido termina en la DLT del topico de citas.
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "vidasalud.kafka.audit-topic.replication-factor=1",
        "vidasalud.kafka.audit-topic.min-insync-replicas=1",
        "vidasalud.kafka.dead-letter.replication-factor=1",
        "vidasalud.kafka.retry.initial-interval=50ms",
        "vidasalud.kafka.retry.max-interval=100ms"
})
@EmbeddedKafka(partitions = 3, topics = {
        "vidasalud.appointments.events",
        "vidasalud.audit.events",
        "vidasalud.appointments.events.auditoria.DLT"
})
class AuditKafkaIntegrationTest {

    private static final String APPOINTMENTS = "vidasalud.appointments.events";
    private static final String DLT = "vidasalud.appointments.events.auditoria.DLT";

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private AuditTimelineService timeline;

    @Test
    void eventoDeCitaLlegaALaLineaDeTiempoPorElTopicoDeAuditoria() {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(Map.of(
                "bootstrap.servers", broker.getBrokersAsString(),
                "key.serializer", StringSerializer.class,
                "value.serializer", StringSerializer.class)));

        template.send(new ProducerRecord<>(APPOINTMENTS, "42", """
                {"eventId":"it-1","type":"APPOINTMENT_STATUS_CHANGED","occurredAt":"%s",
                 "appointmentId":42,"patientId":"12345678-9","serviceId":3,"boxId":7,
                 "previousStatus":"SOLICITADA","newStatus":"CONFIRMADA"}
                """.formatted(Instant.now())));
        template.send(new ProducerRecord<>(APPOINTMENTS, "43", "{\"eventId\":\"it-2\"}"));
        template.flush();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<AuditEvent> events = timeline.list(null, 10);
            assertEquals(1, events.size());
            assertEquals("CITA_CONFIRMADA", events.get(0).accion());
            assertEquals("it-1", events.get(0).traceId());
            assertEquals("cita-42", events.get(0).correlationId());
        });

        Map<String, Object> props = KafkaTestUtils.consumerProps(broker, "it-dlt", false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (Consumer<String, String> consumer =
                     new DefaultKafkaConsumerFactory<String, String>(props).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, DLT);
            ConsumerRecord<String, String> dead = KafkaTestUtils.getSingleRecord(consumer, DLT, Duration.ofSeconds(30));

            assertEquals("43", dead.key());
            assertTrue(dead.headers().lastHeader("kafka_dlt-exception-message") != null);
        }
    }
}
