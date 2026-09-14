package cl.duoc.vidasalud.catalog.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cl.duoc.vidasalud.catalog.dto.HealthServiceRequest;
import cl.duoc.vidasalud.catalog.dto.HealthServiceResponse;
import cl.duoc.vidasalud.catalog.service.HealthServiceService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/catalog/services")
public class HealthServiceController {

    private final HealthServiceService service;

    public HealthServiceController(
            HealthServiceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<HealthServiceResponse> create(
            @Valid
            @RequestBody HealthServiceRequest request) {

        HealthServiceResponse created =
            service.create(request);

        return ResponseEntity
            .created(
                URI.create(
                    "/api/catalog/services/"
                    + created.id()
                )
            )
            .body(created);
    }

    @GetMapping
    public ResponseEntity<List<HealthServiceResponse>>
            findAll(
                @RequestParam(required = false)
                Boolean active
            ) {

        return ResponseEntity.ok(
            service.findAll(active)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<HealthServiceResponse>
            findById(
                @PathVariable Long id
            ) {

        return ResponseEntity.ok(
            service.findById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<HealthServiceResponse>
            update(
                @PathVariable Long id,
                @Valid
                @RequestBody HealthServiceRequest request
            ) {

        return ResponseEntity.ok(
            service.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        service.delete(id);

        return ResponseEntity
            .noContent()
            .build();
    }
}