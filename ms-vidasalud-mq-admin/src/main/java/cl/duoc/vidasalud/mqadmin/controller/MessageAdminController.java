package cl.duoc.vidasalud.mqadmin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.vidasalud.mqadmin.dto.PublishMessageRequest;
import cl.duoc.vidasalud.mqadmin.dto.PublishResponse;
import cl.duoc.vidasalud.mqadmin.service.RabbitAdminService;
import jakarta.validation.Valid;

/**
 * POST /api/mq/messages   publica un comando de prueba (202 Accepted).
 */
@RestController
@RequestMapping("/api/mq/messages")
public class MessageAdminController {

    private final RabbitAdminService adminService;

    public MessageAdminController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<PublishResponse> publish(@Valid @RequestBody PublishMessageRequest request) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(adminService.publish(request));
    }
}
