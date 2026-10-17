package cl.duoc.vidasalud.notify.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ de VidaSalud (3 flujos + 3 DLQ).
 *
 * <pre>
 *  Productor ──► cmd.direct ──(email.send)─────────► q.cmd.email ─────┐
 *            └─► cmd.topic  ──(email.*)────────────► q.cmd.email      │ NACK / max reintentos
 *                cmd.direct ──(admission.ticket)───► q.cmd.admission  │
 *                cmd.topic  ──(admission.#)────────► q.cmd.admission  ├──► cmd.dead.dlx ──► q.cmd.*.dlq
 *                cmd.direct ──(record.gen)─────────► q.cmd.record     │
 *                cmd.topic  ──(record.*)───────────► q.cmd.record ────┘
 * </pre>
 *
 * Cada caso de uso tiene sus beans explicitos (Queue, DLQ y Bindings).
 * RabbitAdmin (autoconfigurado por Spring Boot) declara todos estos beans
 * en el broker al conectarse; la declaracion es idempotente.
 * Todos los nombres provienen de {@link MessagingProperties}.
 */
@Configuration
public class RabbitTopologyConfig {

    private static final String QUEUE_TYPE_ARGUMENT = "x-queue-type";

    private final MessagingProperties properties;

    public RabbitTopologyConfig(MessagingProperties properties) {
        this.properties = properties;
    }

    // =================================================================
    // EXCHANGES (durables, no se borran automaticamente)
    // =================================================================

    /** Exchange direct: entrega por coincidencia exacta de routing key. */
    @Bean
    public DirectExchange commandDirectExchange() {
        return new DirectExchange(properties.exchanges().direct(), true, false);
    }

    /** Exchange topic: entrega por patron (email.*, admission.#, record.*). */
    @Bean
    public TopicExchange commandTopicExchange() {
        return new TopicExchange(properties.exchanges().topic(), true, false);
    }

    /** Dead Letter Exchange: recibe los mensajes rechazados (NACK sin requeue). */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(properties.exchanges().deadLetter(), true, false);
    }

    // =================================================================
    // CASO DE USO 1: EMAIL / PUSH AL PACIENTE
    // =================================================================

    /** Cola principal de emails; sus mensajes muertos van a cmd.dead.dlx. */
    @Bean
    public Queue emailQueue() {
        return mainQueue(properties.routes().email());
    }

    /** DLQ de emails: mensajes que fallaron, retenidos para analisis. */
    @Bean
    public Queue emailDeadLetterQueue() {
        return deadLetterQueue(properties.routes().email());
    }

    /** cmd.direct --(email.send)--> q.cmd.email */
    @Bean
    public Binding emailDirectBinding() {
        return BindingBuilder.bind(emailQueue())
                .to(commandDirectExchange())
                .with(properties.routes().email().directKey());
    }

    /** cmd.topic --(email.*)--> q.cmd.email */
    @Bean
    public Binding emailTopicBinding() {
        return BindingBuilder.bind(emailQueue())
                .to(commandTopicExchange())
                .with(properties.routes().email().topicPattern());
    }

    /** cmd.dead.dlx --(q.cmd.email.dlq)--> q.cmd.email.dlq */
    @Bean
    public Binding emailDeadLetterBinding() {
        return BindingBuilder.bind(emailDeadLetterQueue())
                .to(deadLetterExchange())
                .with(properties.routes().email().deadLetterKey());
    }

    // =================================================================
    // CASO DE USO 2: TICKET DE ADMISION / LLAMADO A BOX
    // =================================================================

    /** Cola principal de admision (tickets y llamados a box). */
    @Bean
    public Queue admissionQueue() {
        return mainQueue(properties.routes().admission());
    }

    /** DLQ de admision. */
    @Bean
    public Queue admissionDeadLetterQueue() {
        return deadLetterQueue(properties.routes().admission());
    }

    /** cmd.direct --(admission.ticket)--> q.cmd.admission */
    @Bean
    public Binding admissionDirectBinding() {
        return BindingBuilder.bind(admissionQueue())
                .to(commandDirectExchange())
                .with(properties.routes().admission().directKey());
    }

    /** cmd.topic --(admission.#)--> q.cmd.admission */
    @Bean
    public Binding admissionTopicBinding() {
        return BindingBuilder.bind(admissionQueue())
                .to(commandTopicExchange())
                .with(properties.routes().admission().topicPattern());
    }

    /** cmd.dead.dlx --(q.cmd.admission.dlq)--> q.cmd.admission.dlq */
    @Bean
    public Binding admissionDeadLetterBinding() {
        return BindingBuilder.bind(admissionDeadLetterQueue())
                .to(deadLetterExchange())
                .with(properties.routes().admission().deadLetterKey());
    }

    // =================================================================
    // CASO DE USO 3: GENERACION DE DOCUMENTOS (COMPROBANTE / RESUMEN PDF)
    // =================================================================

    /** Cola principal de generacion de documentos. */
    @Bean
    public Queue recordQueue() {
        return mainQueue(properties.routes().record());
    }

    /** DLQ de documentos. */
    @Bean
    public Queue recordDeadLetterQueue() {
        return deadLetterQueue(properties.routes().record());
    }

    /** cmd.direct --(record.gen)--> q.cmd.record */
    @Bean
    public Binding recordDirectBinding() {
        return BindingBuilder.bind(recordQueue())
                .to(commandDirectExchange())
                .with(properties.routes().record().directKey());
    }

    /** cmd.topic --(record.*)--> q.cmd.record */
    @Bean
    public Binding recordTopicBinding() {
        return BindingBuilder.bind(recordQueue())
                .to(commandTopicExchange())
                .with(properties.routes().record().topicPattern());
    }

    /** cmd.dead.dlx --(q.cmd.record.dlq)--> q.cmd.record.dlq */
    @Bean
    public Binding recordDeadLetterBinding() {
        return BindingBuilder.bind(recordDeadLetterQueue())
                .to(deadLetterExchange())
                .with(properties.routes().record().deadLetterKey());
    }

    // =================================================================
    // Plantillas comunes (mismas reglas para las 3 rutas)
    // =================================================================

    /**
     * Cola principal durable con DLX configurado:
     * x-dead-letter-exchange = cmd.dead.dlx
     * x-dead-letter-routing-key = nombre de su DLQ
     */
    private Queue mainQueue(MessagingProperties.Route route) {
        return QueueBuilder.durable(route.queue())
                .withArgument(QUEUE_TYPE_ARGUMENT, properties.queueType())
                .deadLetterExchange(properties.exchanges().deadLetter())
                .deadLetterRoutingKey(route.deadLetterKey())
                .build();
    }

    /** DLQ durable con politica de retencion (x-message-ttl). */
    private Queue deadLetterQueue(MessagingProperties.Route route) {
        return QueueBuilder.durable(route.dlq())
                .withArgument(QUEUE_TYPE_ARGUMENT, properties.queueType())
                .ttl((int) properties.dlqRetentionMs())
                .build();
    }
}
