package cl.duoc.vidasalud.kafkaadmin.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.stereotype.Service;

import cl.duoc.vidasalud.kafkaadmin.config.KafkaAdminProperties;
import cl.duoc.vidasalud.kafkaadmin.dto.ClusterResponses.BrokerInfo;
import cl.duoc.vidasalud.kafkaadmin.dto.ClusterResponses.ClusterInfo;
import cl.duoc.vidasalud.kafkaadmin.dto.ClusterResponses.ConsumerGroupDetail;
import cl.duoc.vidasalud.kafkaadmin.dto.ClusterResponses.ConsumerGroupSummary;
import cl.duoc.vidasalud.kafkaadmin.dto.ClusterResponses.MemberInfo;
import cl.duoc.vidasalud.kafkaadmin.dto.ClusterResponses.PartitionLag;
import cl.duoc.vidasalud.kafkaadmin.dto.CreateTopicRequest;
import cl.duoc.vidasalud.kafkaadmin.dto.TopicResponses.ConfigValue;
import cl.duoc.vidasalud.kafkaadmin.dto.TopicResponses.PartitionInfo;
import cl.duoc.vidasalud.kafkaadmin.dto.TopicResponses.TopicDetail;
import cl.duoc.vidasalud.kafkaadmin.dto.TopicResponses.TopicSummary;
import cl.duoc.vidasalud.kafkaadmin.exception.ConflictException;
import cl.duoc.vidasalud.kafkaadmin.exception.InvalidRequestException;
import cl.duoc.vidasalud.kafkaadmin.exception.ResourceNotFoundException;

/**
 * Operaciones de administracion de Kafka sobre el AdminClient: topicos,
 * configuraciones, consumer groups y estado del cluster.
 *
 * Traduce los errores de Kafka a excepciones de negocio (404, 409, 400) y
 * deja pasar las de conexion para que el GlobalExceptionHandler responda 503.
 */
@Service
public class KafkaAdminService {

    /** Configuraciones que siempre se muestran en el detalle de un topico. */
    private static final List<String> RELEVANT_CONFIGS = List.of(
            "retention.ms", "retention.bytes", "cleanup.policy", "min.insync.replicas",
            "max.message.bytes", "segment.bytes", "delete.retention.ms", "compression.type");

    private static final Set<String> CLEANUP_POLICIES = Set.of("delete", "compact", "compact,delete", "delete,compact");

    private final Admin admin;
    private final KafkaAdminProperties properties;

    public KafkaAdminService(Admin admin, KafkaAdminProperties properties) {
        this.admin = admin;
        this.properties = properties;
    }

    // ------------------------------------------------------------------
    // Cluster
    // ------------------------------------------------------------------

    public ClusterInfo describeCluster() {
        DescribeClusterResult result = admin.describeCluster();
        String clusterId = await(result.clusterId());
        Node controller = await(result.controller());
        Collection<Node> nodes = await(result.nodes());

        int controllerId = controller != null ? controller.id() : -1;
        List<BrokerInfo> brokers = nodes.stream()
                .sorted(Comparator.comparingInt(Node::id))
                .map(n -> new BrokerInfo(n.id(), n.host(), n.port(), n.id() == controllerId))
                .toList();
        return new ClusterInfo(clusterId, controllerId, brokers);
    }

    // ------------------------------------------------------------------
    // Topicos
    // ------------------------------------------------------------------

    public List<TopicSummary> listTopics() {
        Set<String> names = await(admin.listTopics().names());
        if (names.isEmpty()) {
            return List.of();
        }
        Map<String, TopicDescription> descriptions = await(admin.describeTopics(names).allTopicNames());
        return descriptions.values().stream()
                .sorted(Comparator.comparing(TopicDescription::name))
                .map(d -> new TopicSummary(d.name(), d.partitions().size(), replicationFactor(d), isProtected(d.name())))
                .toList();
    }

    public TopicDetail describeTopic(String name) {
        TopicDescription description = describe(name);

        List<PartitionInfo> partitions = description.partitions().stream()
                .sorted(Comparator.comparingInt(p -> p.partition()))
                .map(p -> new PartitionInfo(
                        p.partition(),
                        p.leader() != null ? p.leader().id() : -1,
                        p.replicas().stream().map(Node::id).toList(),
                        p.isr().stream().map(Node::id).toList()))
                .toList();

        return new TopicDetail(
                name,
                partitions.size(),
                replicationFactor(description),
                isProtected(name),
                partitions,
                topicConfigs(name));
    }

    public TopicDetail createTopic(CreateTopicRequest request) {
        int partitions = request.partitions() != null
                ? request.partitions()
                : properties.defaults().partitions();
        short replicationFactor = request.replicationFactor() != null
                ? request.replicationFactor()
                : properties.defaults().replicationFactor();
        Map<String, String> configs = validateConfigs(request.configs());

        NewTopic topic = new NewTopic(request.name(), partitions, replicationFactor).configs(configs);
        try {
            await(admin.createTopics(List.of(topic)).all());
        } catch (TopicExistsException e) {
            throw new ConflictException("El topico '" + request.name() + "' ya existe");
        } catch (InvalidReplicationFactorException e) {
            throw new InvalidRequestException(
                    "Factor de replicacion " + replicationFactor + " invalido: " + e.getMessage());
        } catch (InvalidTopicException e) {
            throw new InvalidRequestException(e.getMessage());
        } catch (InvalidConfigurationException e) {
            throw new InvalidRequestException(e.getMessage());
        }
        return describeTopic(request.name());
    }

    public TopicDetail updateTopicConfig(String name, Map<String, String> configs) {
        describe(name);
        Map<String, String> validated = validateConfigs(configs);
        if (validated.isEmpty()) {
            throw new InvalidRequestException("Debe indicar al menos una configuracion");
        }

        List<AlterConfigOp> operations = validated.entrySet().stream()
                .map(e -> new AlterConfigOp(new ConfigEntry(e.getKey(), e.getValue()), AlterConfigOp.OpType.SET))
                .toList();
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        try {
            await(admin.incrementalAlterConfigs(Map.of(resource, operations)).all());
        } catch (InvalidConfigurationException e) {
            throw new InvalidRequestException(e.getMessage());
        }
        return describeTopic(name);
    }

    public TopicDetail increasePartitions(String name, int newTotal) {
        int current = describe(name).partitions().size();
        if (newTotal <= current) {
            throw new InvalidRequestException(
                    "Kafka solo permite aumentar particiones: el topico tiene " + current
                            + " y se pidio " + newTotal);
        }
        try {
            await(admin.createPartitions(Map.of(name, NewPartitions.increaseTo(newTotal))).all());
        } catch (InvalidPartitionsException e) {
            throw new InvalidRequestException(e.getMessage());
        }
        return describeTopic(name);
    }

    public void deleteTopic(String name) {
        if (isProtected(name)) {
            throw new ConflictException("El topico '" + name + "' es parte de la arquitectura base y no se puede eliminar");
        }
        describe(name);
        await(admin.deleteTopics(List.of(name)).all());
    }

    // ------------------------------------------------------------------
    // Consumer groups
    // ------------------------------------------------------------------

    public List<ConsumerGroupSummary> listConsumerGroups() {
        Collection<ConsumerGroupListing> groups = await(admin.listConsumerGroups().all());
        return groups.stream()
                .sorted(Comparator.comparing(ConsumerGroupListing::groupId))
                .map(g -> new ConsumerGroupSummary(g.groupId(),
                        g.groupState().map(Enum::name).orElse("UNKNOWN")))
                .toList();
    }

    public ConsumerGroupDetail describeConsumerGroup(String groupId) {
        ConsumerGroupDescription description = describeGroup(groupId);

        List<MemberInfo> members = description.members().stream()
                .map(m -> new MemberInfo(
                        m.consumerId(),
                        m.clientId(),
                        m.host(),
                        m.assignment().topicPartitions().stream()
                                .sorted(Comparator.comparing(TopicPartition::topic)
                                        .thenComparingInt(TopicPartition::partition))
                                .map(tp -> tp.topic() + "-" + tp.partition())
                                .toList()))
                .toList();

        List<PartitionLag> offsets = lag(groupId);
        long totalLag = offsets.stream().mapToLong(PartitionLag::lag).sum();

        return new ConsumerGroupDetail(
                groupId,
                description.groupState().name(),
                description.partitionAssignor(),
                description.coordinator() != null ? description.coordinator().id() : -1,
                members,
                offsets,
                totalLag);
    }

    public void deleteConsumerGroup(String groupId) {
        describeGroup(groupId);
        try {
            await(admin.deleteConsumerGroups(List.of(groupId)).all());
        } catch (GroupNotEmptyException e) {
            throw new ConflictException("El consumer group '" + groupId
                    + "' tiene consumidores activos: detengalos antes de eliminarlo");
        } catch (GroupIdNotFoundException e) {
            throw new ResourceNotFoundException("El consumer group '" + groupId + "' no existe");
        }
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    private List<PartitionLag> lag(String groupId) {
        Map<TopicPartition, OffsetAndMetadata> committed =
                await(admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());
        if (committed.isEmpty()) {
            return List.of();
        }

        Map<TopicPartition, OffsetSpec> latest = new LinkedHashMap<>();
        committed.keySet().forEach(tp -> latest.put(tp, OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResultInfo> ends = await(admin.listOffsets(latest).all());

        return committed.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(TopicPartition::topic)
                        .thenComparingInt(TopicPartition::partition)))
                .map(e -> {
                    TopicPartition tp = e.getKey();
                    long end = ends.get(tp).offset();
                    long current = e.getValue().offset();
                    return new PartitionLag(tp.topic(), tp.partition(), current, end, Math.max(0, end - current));
                })
                .toList();
    }

    private Map<String, String> validateConfigs(Map<String, String> configs) {
        if (configs == null || configs.isEmpty()) {
            return Map.of();
        }
        Map<String, String> validated = new LinkedHashMap<>();
        configs.forEach((key, value) -> {
            if (!properties.editableConfigs().contains(key)) {
                throw new InvalidRequestException("La configuracion '" + key + "' no se puede modificar. Permitidas: "
                        + String.join(", ", properties.editableConfigs()));
            }
            if (value == null || value.isBlank()) {
                throw new InvalidRequestException("La configuracion '" + key + "' no puede estar vacia");
            }
            validateValue(key, value.trim());
            validated.put(key, value.trim());
        });
        return validated;
    }

    private static void validateValue(String key, String value) {
        switch (key) {
            case "cleanup.policy" -> {
                if (!CLEANUP_POLICIES.contains(value)) {
                    throw new InvalidRequestException("cleanup.policy debe ser delete, compact o compact,delete");
                }
            }
            case "compression.type" -> {
                if (!Set.of("producer", "uncompressed", "gzip", "snappy", "lz4", "zstd").contains(value)) {
                    throw new InvalidRequestException("compression.type invalido: " + value);
                }
            }
            case "retention.ms", "retention.bytes" -> {
                long number = parseLong(key, value);
                if (number < -1) {
                    throw new InvalidRequestException(key + " debe ser -1 (sin limite) o mayor o igual a 0");
                }
            }
            default -> {
                if (parseLong(key, value) < 1) {
                    throw new InvalidRequestException(key + " debe ser un numero mayor o igual a 1");
                }
            }
        }
    }

    private static long parseLong(String key, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new InvalidRequestException(key + " debe ser un numero entero: " + value);
        }
    }

    private Map<String, ConfigValue> topicConfigs(String name) {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        Config config = await(admin.describeConfigs(List.of(resource)).all()).get(resource);

        Map<String, ConfigValue> result = new LinkedHashMap<>();
        for (String key : RELEVANT_CONFIGS) {
            ConfigEntry entry = config.get(key);
            if (entry != null) {
                result.put(key, new ConfigValue(entry.value(), entry.source().name(), entry.isReadOnly()));
            }
        }
        return result;
    }

    private TopicDescription describe(String name) {
        try {
            return await(admin.describeTopics(List.of(name)).allTopicNames()).get(name);
        } catch (UnknownTopicOrPartitionException e) {
            throw new ResourceNotFoundException("El topico '" + name + "' no existe");
        }
    }

    private ConsumerGroupDescription describeGroup(String groupId) {
        ConsumerGroupDescription description;
        try {
            description = await(admin.describeConsumerGroups(List.of(groupId)).all()).get(groupId);
        } catch (GroupIdNotFoundException e) {
            throw new ResourceNotFoundException("El consumer group '" + groupId + "' no existe");
        }
        // Kafka describe un grupo inexistente como un grupo DEAD, sin error
        if (description == null || description.groupState().name().equals("DEAD")) {
            throw new ResourceNotFoundException("El consumer group '" + groupId + "' no existe");
        }
        return description;
    }

    private boolean isProtected(String name) {
        return properties.protectedNames().contains(name) || name.startsWith("__");
    }

    private static int replicationFactor(TopicDescription description) {
        return description.partitions().isEmpty() ? 0 : description.partitions().get(0).replicas().size();
    }

    /**
     * Espera el resultado de Kafka. Los errores de Kafka llegan envueltos en
     * ExecutionException: se desenvuelven para poder tratarlos por tipo.
     */
    private <T> T await(KafkaFuture<T> future) {
        try {
            return future.get(properties.timeout().toMillis() * 2, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operacion interrumpida", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(e.getCause());
        } catch (TimeoutException e) {
            throw new org.apache.kafka.common.errors.TimeoutException("Kafka no respondio a tiempo", e);
        }
    }
}
