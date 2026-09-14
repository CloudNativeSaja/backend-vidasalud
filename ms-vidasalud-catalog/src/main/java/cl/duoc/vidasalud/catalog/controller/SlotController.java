package cl.duoc.vidasalud.catalog.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cl.duoc.vidasalud.catalog.dto.SlotRequest;
import cl.duoc.vidasalud.catalog.dto.SlotResponse;
import cl.duoc.vidasalud.catalog.service.SlotService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/catalog/slots")
public class SlotController {

    private final SlotService service;

    public SlotController(SlotService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SlotResponse> create(
            @Valid
            @RequestBody SlotRequest request) {

        SlotResponse created =
            service.create(request);

        return ResponseEntity
            .created(
                URI.create(
                    "/api/catalog/slots/"
                    + created.id()
                )
            )
            .body(created);
    }

    @GetMapping
    public ResponseEntity<List<SlotResponse>> findAll(

            @RequestParam(required = false)
            Boolean available,

            @RequestParam(required = false)
            Long boxId,

            @RequestParam(required = false)
            Long serviceId,

            @RequestParam(required = false)
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(
                iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime to
        ) {

        return ResponseEntity.ok(
            service.findAll(
                available,
                boxId,
                serviceId,
                from,
                to
            )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<SlotResponse> findById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
            service.findById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<SlotResponse> update(
            @PathVariable Long id,
            @Valid
            @RequestBody SlotRequest request) {

        return ResponseEntity.ok(
            service.update(id, request)
        );
    }

    @PutMapping("/{id}/reserve")
    public ResponseEntity<SlotResponse> reserve(
            @PathVariable Long id) {

        return ResponseEntity.ok(
            service.reserve(id)
        );
    }

    @PutMapping("/{id}/release")
    public ResponseEntity<SlotResponse> release(
            @PathVariable Long id) {

        return ResponseEntity.ok(
            service.release(id)
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