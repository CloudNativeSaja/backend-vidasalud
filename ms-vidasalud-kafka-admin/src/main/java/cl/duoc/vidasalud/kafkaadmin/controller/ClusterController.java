package cl.duoc.vidasalud.kafkaadmin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.vidasalud.kafkaadmin.dto.ClusterResponses.ClusterInfo;
import cl.duoc.vidasalud.kafkaadmin.service.KafkaAdminService;

/** Estado del cluster: brokers y controlador. */
@RestController
@RequestMapping("/api/kafka/cluster")
public class ClusterController {

    private final KafkaAdminService service;

    public ClusterController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping
    public ClusterInfo cluster() {
        return service.describeCluster();
    }
}
