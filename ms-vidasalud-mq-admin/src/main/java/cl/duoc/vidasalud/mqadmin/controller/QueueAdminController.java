package cl.duoc.vidasalud.mqadmin.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.vidasalud.mqadmin.dto.CreateQueueRequest;
import cl.duoc.vidasalud.mqadmin.dto.PurgeResponse;
import cl.duoc.vidasalud.mqadmin.dto.QueueResponse;
import cl.duoc.vidasalud.mqadmin.service.RabbitAdminService;
import jakarta.validation.Valid;

/**
 * Administracion de colas.
 *
 * GET    /api/mq/queues                 lista las colas con sus contadores
 * GET    /api/mq/queues/{name}          detalle de una cola
 * POST   /api/mq/queues                 crea una cola (opcionalmente con DLQ)
 * DELETE /api/mq/queues/{name}          elimina una cola (?ifEmpty=true para exigir que este vacia)
 * POST   /api/mq/queues/{name}/purge    vacia una cola
 */
@RestController
@RequestMapping("/api/mq/queues")
public class QueueAdminController {

    private final RabbitAdminService adminService;

    public QueueAdminController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<QueueResponse> list() {
        return adminService.listQueues();
    }

    @GetMapping("/{name}")
    public QueueResponse get(@PathVariable String name) {
        return adminService.getQueue(name);
    }

    @PostMapping
    public ResponseEntity<QueueResponse> create(@Valid @RequestBody CreateQueueRequest request) {
        QueueResponse created = adminService.createQueue(request);
        return ResponseEntity
                .created(URI.create("/api/mq/queues/" + created.name()))
                .body(created);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(
            @PathVariable String name,
            @RequestParam(defaultValue = "false") boolean ifEmpty) {

        adminService.deleteQueue(name, ifEmpty);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{name}/purge")
    public PurgeResponse purge(@PathVariable String name) {
        return adminService.purgeQueue(name);
    }
}
