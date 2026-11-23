package cl.duoc.vidasalud.kafkaadmin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.web.client.RestClient;

/**
 * Recorre la API HTTP real contra un broker Kafka embebido: el mismo camino
 * que hace el BFF en produccion.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "vidasalud.kafka-admin.timeout=5s",
                "vidasalud.kafka-admin.defaults.replication-factor=1",
                "vidasalud.kafka-admin.protected-names=protegido.base"
        })
@EmbeddedKafka(partitions = 1, topics = {"protegido.base", "con-grupo"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KafkaAdminApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private EmbeddedKafkaBroker broker;

    private RestClient http() {
        return RestClient.builder().baseUrl("http://localhost:" + port + "/api/kafka")
                .defaultStatusHandler(status -> true, (req, res) -> { })
                .build();
    }

    private ResponseEntity<Map<String, Object>> send(String method, String path, Object body) {
        RestClient.RequestBodySpec spec = http().method(org.springframework.http.HttpMethod.valueOf(method)).uri(path);
        if (body != null) {
            spec = spec.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return spec.retrieve().toEntity(new ParameterizedTypeReference<>() { });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @Test
    @Order(1)
    void describeElClusterConSusBrokers() {
        ResponseEntity<Map<String, Object>> response = send("GET", "/cluster", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().get("clusterId"));
        assertEquals(1, ((List<?>) response.getBody().get("brokers")).size());
    }

    @Test
    @Order(2)
    void creaUnTopicoConParticionesYConfiguracion() {
        ResponseEntity<Map<String, Object>> response = send("POST", "/topics", Map.of(
                "name", "demo.topico",
                "partitions", 3,
                "replicationFactor", 1,
                "configs", Map.of("retention.ms", "604800000", "cleanup.policy", "delete")));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertEquals(3, body.get("partitions"));
        assertEquals("604800000", map(map(body.get("configs")).get("retention.ms")).get("value"));
        assertEquals("delete", map(map(body.get("configs")).get("cleanup.policy")).get("value"));
        assertEquals(3, ((List<?>) body.get("partitionDetails")).size());
    }

    @Test
    @Order(3)
    void unTopicoRepetidoDevuelve409() {
        ResponseEntity<Map<String, Object>> response = send("POST", "/topics", Map.of("name", "demo.topico"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(String.valueOf(response.getBody().get("message")).contains("ya existe"));
    }

    @Test
    @Order(4)
    void datosInvalidosDevuelven400ConElDetalle() {
        ResponseEntity<Map<String, Object>> badName = send("POST", "/topics", Map.of("name", "nombre con espacios"));
        ResponseEntity<Map<String, Object>> badConfig = send("POST", "/topics",
                Map.of("name", "otro.topico", "configs", Map.of("unclean.leader.election.enable", "true")));

        assertEquals(HttpStatus.BAD_REQUEST, badName.getStatusCode());
        assertTrue(((List<?>) badName.getBody().get("details")).toString().contains("solo admite"));
        assertEquals(HttpStatus.BAD_REQUEST, badConfig.getStatusCode());
        assertTrue(String.valueOf(badConfig.getBody().get("message")).contains("no se puede modificar"));
    }

    @Test
    @Order(5)
    void modificaLaRetencionYLaPolitica() {
        ResponseEntity<Map<String, Object>> response = send("PUT", "/topics/demo.topico/config",
                Map.of("configs", Map.of("retention.ms", "86400000", "cleanup.policy", "compact")));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> configs = map(response.getBody().get("configs"));
        assertEquals("86400000", map(configs.get("retention.ms")).get("value"));
        assertEquals("compact", map(configs.get("cleanup.policy")).get("value"));
        assertEquals("DYNAMIC_TOPIC_CONFIG", map(configs.get("retention.ms")).get("source"));
    }

    @Test
    @Order(6)
    void rechazaValoresInvalidosDeConfiguracion() {
        ResponseEntity<Map<String, Object>> policy = send("PUT", "/topics/demo.topico/config",
                Map.of("configs", Map.of("cleanup.policy", "borrar")));
        ResponseEntity<Map<String, Object>> number = send("PUT", "/topics/demo.topico/config",
                Map.of("configs", Map.of("retention.ms", "mucho")));

        assertEquals(HttpStatus.BAD_REQUEST, policy.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, number.getStatusCode());
    }

    @Test
    @Order(7)
    void aumentaParticionesPeroNuncaLasReduce() {
        ResponseEntity<Map<String, Object>> up = send("PATCH", "/topics/demo.topico/partitions",
                Map.of("partitions", 5));
        ResponseEntity<Map<String, Object>> down = send("PATCH", "/topics/demo.topico/partitions",
                Map.of("partitions", 2));

        assertEquals(HttpStatus.OK, up.getStatusCode());
        assertEquals(5, up.getBody().get("partitions"));
        assertEquals(HttpStatus.BAD_REQUEST, down.getStatusCode());
        assertTrue(String.valueOf(down.getBody().get("message")).contains("solo permite aumentar"));
    }

    @Test
    @Order(8)
    void listaLosTopicosYMarcaLosProtegidos() {
        ResponseEntity<List<Map<String, Object>>> response = http().get().uri("/topics").retrieve()
                .toEntity(new ParameterizedTypeReference<>() { });

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map<String, Object>> topics = response.getBody();
        assertTrue(topics.stream().anyMatch(t -> "demo.topico".equals(t.get("name"))
                && Boolean.FALSE.equals(t.get("protectedTopic"))));
        assertTrue(topics.stream().anyMatch(t -> "protegido.base".equals(t.get("name"))
                && Boolean.TRUE.equals(t.get("protectedTopic"))));
    }

    @Test
    @Order(9)
    void noEliminaUnTopicoProtegidoNiUnoInexistente() {
        assertEquals(HttpStatus.CONFLICT, send("DELETE", "/topics/protegido.base", null).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, send("DELETE", "/topics/no.existe", null).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, send("GET", "/topics/no.existe", null).getStatusCode());
    }

    // Va al final: en Windows el borrado de un topico deja fuera de servicio el
    // directorio de logs del broker embebido (problema conocido de Kafka en Windows).
    @Test
    @Order(11)
    void eliminaUnTopico() {
        assertEquals(HttpStatus.NO_CONTENT, send("DELETE", "/topics/demo.topico", null).getStatusCode());
        await(() -> send("GET", "/topics/demo.topico", null).getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(10)
    void muestraElConsumerGroupConSuLag() {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(Map.of(
                "bootstrap.servers", broker.getBrokersAsString(),
                "key.serializer", StringSerializer.class,
                "value.serializer", StringSerializer.class)));
        for (int i = 0; i < 5; i++) {
            template.send(new ProducerRecord<>("con-grupo", "k" + i, "v" + i));
        }
        template.flush();

        Map<String, Object> props = KafkaTestUtils.consumerProps(broker, "grupo-de-prueba", false);
        props.put("auto.offset.reset", "earliest");
        props.put("key.deserializer", StringDeserializer.class);
        props.put("value.deserializer", StringDeserializer.class);
        props.put("max.poll.records", 2);

        try (Consumer<String, String> consumer =
                     new DefaultKafkaConsumerFactory<String, String>(props).createConsumer()) {
            consumer.subscribe(List.of("con-grupo"));
            // Lee algunos de los 5 mensajes y confirma: el resto queda pendiente (lag)
            int read = 0;
            long deadline = System.currentTimeMillis() + 60_000;
            while (read < 2 && System.currentTimeMillis() < deadline) {
                read += consumer.poll(Duration.ofMillis(500)).count();
            }
            assertTrue(read >= 2, "El consumidor debio leer al menos 2 mensajes");
            consumer.commitSync();

            ResponseEntity<List<Map<String, Object>>> list = http().get().uri("/consumer-groups").retrieve()
                    .toEntity(new ParameterizedTypeReference<>() { });
            assertTrue(list.getBody().stream().anyMatch(g -> "grupo-de-prueba".equals(g.get("groupId"))));

            ResponseEntity<Map<String, Object>> detail = send("GET", "/consumer-groups/grupo-de-prueba", null);
            assertEquals(HttpStatus.OK, detail.getStatusCode());
            assertEquals(1, ((List<?>) detail.getBody().get("members")).size());
            long totalLag = ((Number) detail.getBody().get("totalLag")).longValue();
            assertEquals(5 - read, totalLag);

            // Con un consumidor activo no se puede eliminar el grupo
            assertEquals(HttpStatus.CONFLICT, send("DELETE", "/consumer-groups/grupo-de-prueba", null).getStatusCode());
        }
        assertEquals(HttpStatus.NOT_FOUND, send("GET", "/consumer-groups/no-existe", null).getStatusCode());
    }

    private static void await(java.util.function.BooleanSupplier condition) {
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(15)).until(condition::getAsBoolean);
    }
}
