package cl.duoc.vidasalud.appointments.streaming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import cl.duoc.vidasalud.appointments.entity.Appointment;
import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;

/**
 * Prueba contra un broker Kafka real (embebido): el evento sale con los mismos
 * serializers y propiedades de application.yaml y llega integro al consumidor.
 */
@EmbeddedKafka(partitions = 3, topics = AppointmentEventKafkaTest.TOPIC)
class AppointmentEventKafkaTest {

    static final String TOPIC = "vidasalud.appointments.events.it";

    @Test
    void elEventoViajaComoJsonConLaCitaComoClave(EmbeddedKafkaBroker broker) {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        producerProps.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        KafkaTemplate<String, AppointmentEvent> template =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));

        Appointment appointment = new Appointment();
        ReflectionTestUtils.setField(appointment, "id", 42L);
        appointment.setPatientId("12345678-9");
        appointment.setServiceId(3L);
        appointment.setBoxId(7L);
        appointment.setStatus(AppointmentStatus.CONFIRMADA);
        AppointmentEvent enviado = AppointmentEvent.statusChanged(appointment, AppointmentStatus.SOLICITADA);

        template.send(TOPIC, enviado.key(), enviado);
        template.flush();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(broker, "it-grupo", true);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        consumerProps.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, AppointmentEvent.class.getName());
        consumerProps.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        consumerProps.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "cl.duoc.vidasalud.appointments.streaming");

        try (Consumer<String, AppointmentEvent> consumer =
                     new DefaultKafkaConsumerFactory<String, AppointmentEvent>(consumerProps).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, TOPIC);
            ConsumerRecord<String, AppointmentEvent> record =
                    KafkaTestUtils.getSingleRecord(consumer, TOPIC, Duration.ofSeconds(15));

            assertEquals("42", record.key());
            assertNotNull(record.value());
            assertEquals(enviado, record.value());
        }
    }
}
