package cl.duoc.vidasalud.mqadmin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.vidasalud.mqadmin.dto.BindingRequest;
import cl.duoc.vidasalud.mqadmin.dto.BindingResponse;
import cl.duoc.vidasalud.mqadmin.service.RabbitAdminService;
import jakarta.validation.Valid;

/**
 * Administracion de bindings (exchange -> cola).
 *
 * GET    /api/mq/bindings                                         lista los bindings
 * POST   /api/mq/bindings                                         crea un binding
 * DELETE /api/mq/bindings?exchange=..&amp;queue=..&amp;routingKey=..   elimina un binding
 */
@RestController
@RequestMapping("/api/mq/bindings")
public class BindingAdminController {

    private final RabbitAdminService adminService;

    public BindingAdminController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<BindingResponse> list() {
        return adminService.listBindings();
    }

    @PostMapping
    public ResponseEntity<BindingResponse> create(@Valid @RequestBody BindingRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(adminService.createBinding(request));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @RequestParam String exchange,
            @RequestParam String queue,
            @RequestParam(defaultValue = "") String routingKey) {

        adminService.deleteBinding(exchange, queue, routingKey);
        return ResponseEntity.noContent().build();
    }
}
