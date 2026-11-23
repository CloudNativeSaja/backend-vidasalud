package cl.duoc.vidasalud.report;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
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

import cl.duoc.vidasalud.report.dto.ReportRange;
import cl.duoc.vidasalud.report.service.KpiService;

/**
 * Flujo completo contra un broker Kafka embebido:
 * - un evento valido actualiza los KPIs;
 * - un evento invalido y un JSON corrupto terminan en la DLT sin detener al consumidor.
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "vidasalud.kafka.dead-letter.replication-factor=1",
        "vidasalud.kafka.retry.initial-interval=50ms",
        "vidasalud.kafka.retry.max-interval=100ms",
        "vidasalud.catalog.url=http://127.0.0.1:1"
})
@EmbeddedKafka(partitions = 3, topics = {
        "vidasalud.appointments.events",
        "vidasalud.appointments.events.reportes.DLT"
})
class ReportKafkaIntegrationTest {

    private static final String TOPIC = "vidasalud.appointments.events";
    private static final String DLT = "vidasalud.appointments.events.reportes.DLT";

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private KpiService kpiService;

    private KafkaTemplate<String, String> rawTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(Map.of(
                "bootstrap.servers", broker.getBrokersAsString(),
                "key.serializer", StringSerializer.class,
                "value.serializer", StringSerializer.class)));
    }

    @Test
    void eventoValidoActualizaKpisEInvalidosVanALaDlt() {
        KafkaTemplate<String, String> template = rawTemplate();
        String now = Instant.now().toString();

        template.send(new ProducerRecord<>(TOPIC, "42", """
                {"eventId":"it-1","type":"APPOINTMENT_CREATED","occurredAt":"%s",
                 "appointmentId":42,"patientId":"12345678-9","serviceId":3,"boxId":7,
                 "previousStatus":null,"newStatus":"SOLICITADA"}
                """.formatted(now)));
        // Falta serviceId y newStatus: no pasa la validacion
        template.send(new ProducerRecord<>(TOPIC, "43", """
                {"eventId":"it-2","type":"APPOINTMENT_CREATED","occurredAt":"%s","appointmentId":43}
                """.formatted(now)));
        // No es JSON: falla la deserializacion
        template.send(new ProducerRecord<>(TOPIC, "44", "esto no es json"));
        template.flush();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertEquals(1, kpiService.kpis(ReportRange.LAST_24H).totalAppointments()));

        Map<String, Object> props = KafkaTestUtils.consumerProps(broker, "it-dlt", false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (Consumer<String, String> consumer =
                     new DefaultKafkaConsumerFactory<String, String>(props).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, DLT);
            var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(30), 2);

            assertEquals(2, records.count());
            for (ConsumerRecord<String, String> record : records) {
                assertTrue(record.headers().lastHeader("kafka_dlt-exception-message") != null,
                        "La DLT debe indicar la causa del error");
            }
        }
    }
}
