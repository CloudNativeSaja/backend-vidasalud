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
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.vidasalud.mqadmin.dto.CreateExchangeRequest;
import cl.duoc.vidasalud.mqadmin.dto.ExchangeResponse;
import cl.duoc.vidasalud.mqadmin.service.RabbitAdminService;
import jakarta.validation.Valid;

/**
 * Administracion de exchanges.
 *
 * GET    /api/mq/exchanges           lista los exchanges
 * POST   /api/mq/exchanges           crea un exchange (direct, topic, fanout, headers)
 * DELETE /api/mq/exchanges/{name}    elimina un exchange
 */
@RestController
@RequestMapping("/api/mq/exchanges")
public class ExchangeAdminController {

    private final RabbitAdminService adminService;

    public ExchangeAdminController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<ExchangeResponse> list() {
        return adminService.listExchanges();
    }

    @PostMapping
    public ResponseEntity<ExchangeResponse> create(@Valid @RequestBody CreateExchangeRequest request) {
        ExchangeResponse created = adminService.createExchange(request);
        return ResponseEntity
                .created(URI.create("/api/mq/exchanges/" + created.name()))
                .body(created);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        adminService.deleteExchange(name);
        return ResponseEntity.noContent().build();
    }
}
