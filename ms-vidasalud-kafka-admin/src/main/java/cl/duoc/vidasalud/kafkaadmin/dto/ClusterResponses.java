package cl.duoc.vidasalud.kafkaadmin.dto;

import java.util.List;

/** Respuestas de la API de cluster y consumer groups. */
public final class ClusterResponses {

    private ClusterResponses() {
    }

    public record BrokerInfo(
            int id,
            String host,
            int port,
            boolean controller
    ) {
    }

    public record ClusterInfo(
            String clusterId,
            int controllerId,
            List<BrokerInfo> brokers
    ) {
    }

    public record ConsumerGroupSummary(
            String groupId,
            String state
    ) {
    }

    public record MemberInfo(
            String memberId,
            String clientId,
            String host,
            List<String> assignedPartitions
    ) {
    }

    /** Posicion de un grupo en una particion y cuanto le falta por leer. */
    public record PartitionLag(
            String topic,
            int partition,
            long currentOffset,
            long endOffset,
            long lag
    ) {
    }

    public record ConsumerGroupDetail(
            String groupId,
            String state,
            String partitionAssignor,
            int coordinatorId,
            List<MemberInfo> members,
            List<PartitionLag> offsets,
            long totalLag
    ) {
    }
}
