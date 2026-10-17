package cl.duoc.vidasalud.mqadmin.dto;

public record ClusterNodeResponse(
        String name,
        boolean running,
        String type,
        long uptimeMs,
        long memoryUsedBytes
) {
}
