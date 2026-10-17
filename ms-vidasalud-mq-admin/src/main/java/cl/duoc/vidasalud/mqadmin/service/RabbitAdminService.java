package cl.duoc.vidasalud.mqadmin.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import cl.duoc.vidasalud.mqadmin.client.RabbitManagementClient;
import cl.duoc.vidasalud.mqadmin.config.MqAdminProperties;
import cl.duoc.vidasalud.mqadmin.dto.BindingRequest;
import cl.duoc.vidasalud.mqadmin.dto.BindingResponse;
import cl.duoc.vidasalud.mqadmin.dto.ClusterNodeResponse;
import cl.duoc.vidasalud.mqadmin.dto.CreateExchangeRequest;
import cl.duoc.vidasalud.mqadmin.dto.CreateQueueRequest;
import cl.duoc.vidasalud.mqadmin.dto.ExchangeResponse;
import cl.duoc.vidasalud.mqadmin.dto.PublishMessageRequest;
import cl.duoc.vidasalud.mqadmin.dto.PublishResponse;
import cl.duoc.vidasalud.mqadmin.dto.PurgeResponse;
import cl.duoc.vidasalud.mqadmin.dto.QueueResponse;
import cl.duoc.vidasalud.mqadmin.dto.ValidationPatterns;
import cl.duoc.vidasalud.mqadmin.exception.ConflictException;
import cl.duoc.vidasalud.mqadmin.exception.InvalidRequestException;
import cl.duoc.vidasalud.mqadmin.exception.ResourceNotFoundException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Encapsula TODA la administracion de RabbitMQ.
 *
 * - Escrituras (crear/eliminar colas, exchanges y bindings, purgar, publicar)
 *   via AmqpAdmin / RabbitTemplate (protocolo AMQP).
 * - Lecturas (listados, estado del cluster) via RabbitMQ Management HTTP.
 *
 * Los controladores solo llaman a estos metodos de alto nivel y no conocen
 * ninguna clase de RabbitMQ.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private static final String DLQ_SUFFIX = ".dlq";
    private static final String QUEUE_TYPE_ARGUMENT = "x-queue-type";
    private static final String DESTINATION_TYPE_QUEUE = "queue";
    private static final String SOURCE = "ms-vidasalud-mq-admin";

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitManagementClient managementClient;
    private final MqAdminProperties properties;
    private final JsonMapper jsonMapper;

    public RabbitAdminService(
            AmqpAdmin amqpAdmin,
            RabbitTemplate rabbitTemplate,
            RabbitManagementClient managementClient,
            MqAdminProperties properties,
            JsonMapper jsonMapper) {

        this.amqpAdmin = amqpAdmin;
        this.rabbitTemplate = rabbitTemplate;
        this.managementClient = managementClient;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    // =================================================================
    // COLAS
    // =================================================================

    public List<QueueResponse> listQueues() {
        return managementClient.getQueues().stream()
                .filter(this::inVhost)
                .map(this::toQueueResponse)
                .sorted(Comparator.comparing(QueueResponse::name))
                .toList();
    }

    public QueueResponse getQueue(String name) {
        requireValidName(name, "cola");
        return findQueue(name)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cola '" + name + "'"));
    }

    /**
     * Crea una cola durable (o no) con argumentos opcionales: tipo, DLX, TTL y largo maximo.
     * Si withDeadLetterQueue=true crea ademas "<name>.dlq" enlazada al DLX.
     */
    public QueueResponse createQueue(CreateQueueRequest request) {
        String name = request.name();

        if (queueExists(name)) {
            throw new ConflictException("La cola '" + name + "' ya existe");
        }

        String deadLetterKey = request.deadLetterRoutingKey();

        if (request.withDeadLetterQueueOrDefault()) {
            String dlqName = name + DLQ_SUFFIX;
            if (queueExists(dlqName)) {
                throw new ConflictException("La DLQ '" + dlqName + "' ya existe");
            }
            requireExchangeExists(request.deadLetterExchange());
            deadLetterKey = deadLetterKey != null ? deadLetterKey : dlqName;

            Queue dlq = QueueBuilder.durable(dlqName)
                    .withArgument(QUEUE_TYPE_ARGUMENT, request.typeOrDefault())
                    .build();
            amqpAdmin.declareQueue(dlq);
            amqpAdmin.declareBinding(queueBinding(request.deadLetterExchange(), dlqName, deadLetterKey));
            log.info("[MQ-ADMIN] DLQ creada {} enlazada a {} con key {}", dlqName, request.deadLetterExchange(), deadLetterKey);
        }

        QueueBuilder builder = request.durableOrDefault()
                ? QueueBuilder.durable(name)
                : QueueBuilder.nonDurable(name);

        builder.withArgument(QUEUE_TYPE_ARGUMENT, request.typeOrDefault());

        if (request.deadLetterExchange() != null) {
            builder.deadLetterExchange(request.deadLetterExchange());
        }
        if (deadLetterKey != null) {
            builder.deadLetterRoutingKey(deadLetterKey);
        }
        if (request.messageTtlMs() != null) {
            builder.ttl(request.messageTtlMs().intValue());
        }
        if (request.maxLength() != null) {
            builder.maxLength(request.maxLength());
        }

        amqpAdmin.declareQueue(builder.build());
        log.info("[MQ-ADMIN] Cola creada {} (tipo={}, durable={})", name, request.typeOrDefault(), request.durableOrDefault());

                final String effectiveDeadLetterKey = deadLetterKey;
        return findQueue(name).orElseGet(() -> new QueueResponse(
                name, request.typeOrDefault(), request.durableOrDefault(), 0, 0, 0, 0, null,
                request.deadLetterExchange(), effectiveDeadLetterKey, false));
    }

    /**
     * Elimina una cola. Las colas de la topologia base estan protegidas.
     *
     * @param ifEmpty si es true, solo elimina la cola cuando no tiene mensajes
     */
    public void deleteQueue(String name, boolean ifEmpty) {
        requireValidName(name, "cola");
        requireNotProtected(name);

        QueueResponse queue = getQueue(name);
        if (ifEmpty && queue.messages() > 0) {
            throw new ConflictException("La cola '" + name + "' tiene " + queue.messages()
                    + " mensajes; use ifEmpty=false para eliminarla igualmente");
        }

        amqpAdmin.deleteQueue(name);
        log.info("[MQ-ADMIN] Cola eliminada {}", name);
    }

    /** Vacia una cola (util para limpiar DLQ despues de analizarlas). */
    public PurgeResponse purgeQueue(String name) {
        requireValidName(name, "cola");
        if (!queueExists(name)) {
            throw new ResourceNotFoundException("No existe la cola '" + name + "'");
        }
        int purged = amqpAdmin.purgeQueue(name);
        log.info("[MQ-ADMIN] Cola purgada {} ({} mensajes)", name, purged);
        return new PurgeResponse(name, purged);
    }

    // =================================================================
    // EXCHANGES
    // =================================================================

    public List<ExchangeResponse> listExchanges() {
        return managementClient.getExchanges().stream()
                .filter(this::inVhost)
                .filter(exchange -> !text(exchange, "name").isEmpty())
                .map(this::toExchangeResponse)
                .sorted(Comparator.comparing(ExchangeResponse::name))
                .toList();
    }

    public ExchangeResponse createExchange(CreateExchangeRequest request) {
        String name = request.name();

        if (findExchange(name).isPresent()) {
            throw new ConflictException("El exchange '" + name + "' ya existe");
        }

        boolean durable = request.durableOrDefault();
        Exchange exchange = switch (request.type().toLowerCase()) {
            case "direct" -> new DirectExchange(name, durable, false);
            case "topic" -> new TopicExchange(name, durable, false);
            case "fanout" -> new FanoutExchange(name, durable, false);
            case "headers" -> new HeadersExchange(name, durable, false);
            default -> throw new InvalidRequestException("Tipo de exchange no soportado: " + request.type());
        };

        amqpAdmin.declareExchange(exchange);
        log.info("[MQ-ADMIN] Exchange creado {} (tipo={})", name, exchange.getType());

        return new ExchangeResponse(name, exchange.getType(), durable, false, false);
    }

    public void deleteExchange(String name) {
        requireValidName(name, "exchange");
        requireNotProtected(name);
        requireExchangeExists(name);

        amqpAdmin.deleteExchange(name);
        log.info("[MQ-ADMIN] Exchange eliminado {}", name);
    }

    // =================================================================
    // BINDINGS
    // =================================================================

    public List<BindingResponse> listBindings() {
        return managementClient.getBindings().stream()
                .filter(this::inVhost)
                .filter(binding -> !text(binding, "source").isEmpty())
                .filter(binding -> DESTINATION_TYPE_QUEUE.equals(text(binding, "destination_type")))
                .map(this::toBindingResponse)
                .sorted(Comparator.comparing(BindingResponse::exchange).thenComparing(BindingResponse::queue))
                .toList();
    }

    public BindingResponse createBinding(BindingRequest request) {
        requireExchangeExists(request.exchange());
        if (!queueExists(request.queue())) {
            throw new ResourceNotFoundException("No existe la cola '" + request.queue() + "'");
        }
        if (bindingExists(request.exchange(), request.queue(), request.routingKey())) {
            throw new ConflictException("El binding " + describe(request.exchange(), request.queue(), request.routingKey())
                    + " ya existe");
        }

        amqpAdmin.declareBinding(queueBinding(request.exchange(), request.queue(), request.routingKey()));
        log.info("[MQ-ADMIN] Binding creado {}", describe(request.exchange(), request.queue(), request.routingKey()));

        return new BindingResponse(request.exchange(), request.queue(), request.routingKey(), false);
    }

    public void deleteBinding(String exchange, String queue, String routingKey) {
        requireValidName(exchange, "exchange");
        requireValidName(queue, "cola");
        if (routingKey == null || !routingKey.matches(ValidationPatterns.ROUTING_KEY)) {
            throw new InvalidRequestException("La routing key " + ValidationPatterns.ROUTING_KEY_MESSAGE);
        }
        if (properties.isProtected(exchange) && properties.isProtected(queue)) {
            throw new ConflictException("El binding " + describe(exchange, queue, routingKey)
                    + " pertenece a la topologia base y no se puede eliminar");
        }
        if (!bindingExists(exchange, queue, routingKey)) {
            throw new ResourceNotFoundException("No existe el binding " + describe(exchange, queue, routingKey));
        }

        amqpAdmin.removeBinding(queueBinding(exchange, queue, routingKey));
        log.info("[MQ-ADMIN] Binding eliminado {}", describe(exchange, queue, routingKey));
    }

    // =================================================================
    // MENSAJES DE PRUEBA
    // =================================================================

    /** Publica un comando con el envelope comun para probar el ruteo y los consumidores. */
    public PublishResponse publish(PublishMessageRequest request) {
        requireExchangeExists(request.exchange());

        String eventId = UUID.randomUUID().toString();
        Map<String, Object> envelope = Map.of(
                "eventId", eventId,
                "type", request.type(),
                "timestamp", Instant.now().toString(),
                "traceId", UUID.randomUUID().toString(),
                "correlationId", "mq-admin-" + eventId,
                "source", SOURCE,
                "payload", request.payload());

        Message message = MessageBuilder.withBody(jsonMapper.writeValueAsBytes(envelope))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding("UTF-8")
                .setMessageId(eventId)
                .setType(request.type())
                .setAppId(SOURCE)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();

        rabbitTemplate.send(request.exchange(), request.routingKey(), message);
        log.info("[MQ-ADMIN] Mensaje de prueba publicado | exchange={} routingKey={} type={} eventId={}",
                request.exchange(), request.routingKey(), request.type(), eventId);

        return new PublishResponse(eventId, request.exchange(), request.routingKey(), request.type());
    }

    // =================================================================
    // CLUSTER
    // =================================================================

    public List<ClusterNodeResponse> clusterNodes() {
        return managementClient.getNodes().stream()
                .map(node -> new ClusterNodeResponse(
                        text(node, "name"),
                        Boolean.TRUE.equals(node.get("running")),
                        text(node, "type"),
                        number(node, "uptime"),
                        number(node, "mem_used")))
                .sorted(Comparator.comparing(ClusterNodeResponse::name))
                .toList();
    }

    // =================================================================
    // Helpers
    // =================================================================

    private boolean queueExists(String name) {
        return amqpAdmin.getQueueInfo(name) != null;
    }

    private Optional<QueueResponse> findQueue(String name) {
        return managementClient.getQueues().stream()
                .filter(this::inVhost)
                .filter(queue -> name.equals(text(queue, "name")))
                .map(this::toQueueResponse)
                .findFirst();
    }

    private Optional<Map<String, Object>> findExchange(String name) {
        return managementClient.getExchanges().stream()
                .filter(this::inVhost)
                .filter(exchange -> name.equals(text(exchange, "name")))
                .findFirst();
    }

    private void requireExchangeExists(String name) {
        if (findExchange(name).isEmpty()) {
            throw new ResourceNotFoundException("No existe el exchange '" + name + "'");
        }
    }

    private boolean bindingExists(String exchange, String queue, String routingKey) {
        return managementClient.getBindings().stream()
                .filter(this::inVhost)
                .anyMatch(binding -> exchange.equals(text(binding, "source"))
                        && queue.equals(text(binding, "destination"))
                        && routingKey.equals(text(binding, "routing_key")));
    }

    private void requireValidName(String name, String resource) {
        if (name == null || !name.matches(ValidationPatterns.RESOURCE_NAME)) {
            throw new InvalidRequestException("El nombre de la " + resource + " '" + name + "' "
                    + ValidationPatterns.RESOURCE_NAME_MESSAGE);
        }
    }

    private void requireNotProtected(String name) {
        if (properties.isProtected(name)) {
            throw new ConflictException("'" + name + "' pertenece a la topologia base de VidaSalud y no se puede eliminar");
        }
    }

    private static Binding queueBinding(String exchange, String queue, String routingKey) {
        return new Binding(queue, Binding.DestinationType.QUEUE, exchange, routingKey, null);
    }

    private static String describe(String exchange, String queue, String routingKey) {
        return exchange + " --(" + routingKey + ")--> " + queue;
    }

    private boolean inVhost(Map<String, Object> resource) {
        return properties.vhost().equals(text(resource, "vhost"));
    }

    private QueueResponse toQueueResponse(Map<String, Object> queue) {
        Map<String, Object> arguments = map(queue.get("arguments"));
        String name = text(queue, "name");
        return new QueueResponse(
                name,
                text(queue, "type"),
                Boolean.TRUE.equals(queue.get("durable")),
                number(queue, "messages"),
                number(queue, "messages_ready"),
                number(queue, "messages_unacknowledged"),
                number(queue, "consumers"),
                text(queue, "node"),
                text(arguments, "x-dead-letter-exchange"),
                text(arguments, "x-dead-letter-routing-key"),
                properties.isProtected(name));
    }

    private ExchangeResponse toExchangeResponse(Map<String, Object> exchange) {
        String name = text(exchange, "name");
        return new ExchangeResponse(
                name,
                text(exchange, "type"),
                Boolean.TRUE.equals(exchange.get("durable")),
                name.startsWith("amq."),
                properties.isProtected(name));
    }

    private BindingResponse toBindingResponse(Map<String, Object> binding) {
        String exchange = text(binding, "source");
        String queue = text(binding, "destination");
        return new BindingResponse(
                exchange,
                queue,
                text(binding, "routing_key"),
                properties.isProtected(exchange) && properties.isProtected(queue));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static String text(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static long number(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value instanceof Number n ? n.longValue() : 0L;
    }
}
