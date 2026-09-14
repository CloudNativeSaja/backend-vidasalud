package cl.duoc.vidasalud.catalog.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cl.duoc.vidasalud.catalog.dto.ClinicalBoxRequest;
import cl.duoc.vidasalud.catalog.dto.ClinicalBoxResponse;
import cl.duoc.vidasalud.catalog.service.ClinicalBoxService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/catalog/boxes")
public class ClinicalBoxController {

    private final ClinicalBoxService service;

    public ClinicalBoxController(
            ClinicalBoxService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ClinicalBoxResponse> create(
            @Valid
            @RequestBody ClinicalBoxRequest request) {

        ClinicalBoxResponse created =
            service.create(request);

        return ResponseEntity
            .created(
                URI.create(
                    "/api/catalog/boxes/"
                    + created.id()
                )
            )
            .body(created);
    }

    @GetMapping
    public ResponseEntity<List<ClinicalBoxResponse>>
            findAll(
                @RequestParam(required = false)
                Boolean active
            ) {

        return ResponseEntity.ok(
            service.findAll(active)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClinicalBoxResponse>
            findById(
                @PathVariable Long id
            ) {

        return ResponseEntity.ok(
            service.findById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClinicalBoxResponse>
            update(
                @PathVariable Long id,
                @Valid
                @RequestBody ClinicalBoxRequest request
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