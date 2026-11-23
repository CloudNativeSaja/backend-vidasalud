package cl.duoc.vidasalud.kafkaadmin.dto;

import java.util.List;
import java.util.Map;

/** Respuestas de la API de topicos. */
public final class TopicResponses {

    private TopicResponses() {
    }

    /** Fila del listado de topicos. */
    public record TopicSummary(
            String name,
            int partitions,
            int replicationFactor,
            boolean protectedTopic
    ) {
    }

    /** Estado de una particion: quien es el lider y que replicas estan sincronizadas. */
    public record PartitionInfo(
            int partition,
            int leader,
            List<Integer> replicas,
            List<Integer> inSyncReplicas
    ) {
    }

    /** Valor de una configuracion y de donde viene (topico, broker o por defecto). */
    public record ConfigValue(
            String value,
            String source,
            boolean readOnly
    ) {
    }

    public record TopicDetail(
            String name,
            int partitions,
            int replicationFactor,
            boolean protectedTopic,
            List<PartitionInfo> partitionDetails,
            Map<String, ConfigValue> configs
    ) {
    }
}
