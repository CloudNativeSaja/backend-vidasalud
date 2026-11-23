package cl.duoc.vidasalud.kafkaadmin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.vidasalud.kafkaadmin.dto.ClusterResponses.ConsumerGroupDetail;
import cl.duoc.vidasalud.kafkaadmin.dto.ClusterResponses.ConsumerGroupSummary;
import cl.duoc.vidasalud.kafkaadmin.service.KafkaAdminService;

/** Inspeccion de consumer groups: estado, miembros y lag por particion. */
@RestController
@RequestMapping("/api/kafka/consumer-groups")
public class ConsumerGroupController {

    private final KafkaAdminService service;

    public ConsumerGroupController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<ConsumerGroupSummary> list() {
        return service.listConsumerGroups();
    }

    @GetMapping("/{groupId}")
    public ConsumerGroupDetail describe(@PathVariable String groupId) {
        return service.describeConsumerGroup(groupId);
    }

    /** Solo se puede eliminar un grupo sin consumidores activos. */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> delete(@PathVariable String groupId) {
        service.deleteConsumerGroup(groupId);
        return ResponseEntity.noContent().build();
    }
}
