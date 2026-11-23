package cl.duoc.vidasalud.kafkaadmin.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.vidasalud.kafkaadmin.dto.CreateTopicRequest;
import cl.duoc.vidasalud.kafkaadmin.dto.IncreasePartitionsRequest;
import cl.duoc.vidasalud.kafkaadmin.dto.TopicResponses.TopicDetail;
import cl.duoc.vidasalud.kafkaadmin.dto.TopicResponses.TopicSummary;
import cl.duoc.vidasalud.kafkaadmin.dto.UpdateTopicConfigRequest;
import cl.duoc.vidasalud.kafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;

/** Administracion de topicos: crear, listar, modificar y eliminar. */
@RestController
@RequestMapping("/api/kafka/topics")
public class TopicAdminController {

    private final KafkaAdminService service;

    public TopicAdminController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<TopicSummary> list() {
        return service.listTopics();
    }

    @GetMapping("/{name}")
    public TopicDetail describe(@PathVariable String name) {
        return service.describeTopic(name);
    }

    @PostMapping
    public ResponseEntity<TopicDetail> create(@Valid @RequestBody CreateTopicRequest request) {
        TopicDetail created = service.createTopic(request);
        return ResponseEntity.created(URI.create("/api/kafka/topics/" + created.name())).body(created);
    }

    /** Cambia retencion, cleanup.policy, min.insync.replicas, etc. */
    @PutMapping("/{name}/config")
    public TopicDetail updateConfig(@PathVariable String name, @Valid @RequestBody UpdateTopicConfigRequest request) {
        return service.updateTopicConfig(name, request.configs());
    }

    @PatchMapping("/{name}/partitions")
    public TopicDetail increasePartitions(@PathVariable String name,
                                          @Valid @RequestBody IncreasePartitionsRequest request) {
        return service.increasePartitions(name, request.partitions());
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        service.deleteTopic(name);
        return ResponseEntity.noContent().build();
    }
}
