package cl.duoc.vidasalud.bff.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cl.duoc.vidasalud.bff.client.AppointmentsClient;
import cl.duoc.vidasalud.bff.dto.AppointmentRequest;
import cl.duoc.vidasalud.bff.dto.AppointmentResponse;
import cl.duoc.vidasalud.bff.dto.AppointmentUpdateRequest;
import cl.duoc.vidasalud.bff.dto.StatusRequest;

@RestController
@RequestMapping("/api/bff/appointments")
public class BffAppointmentsController {

    private final AppointmentsClient client;

    public BffAppointmentsController(
            AppointmentsClient client) {
        this.client = client;
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponse>>
        findAll() {

        return ResponseEntity.ok(
            client.findAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse>
        findById(@PathVariable Long id) {

        return ResponseEntity.ok(
            client.findById(id)
        );
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse>
        create(
            @RequestBody AppointmentRequest request
        ) {

        AppointmentResponse created =
            client.create(request);

        return ResponseEntity
            .created(
                URI.create(
                    "/api/bff/appointments/"
                    + created.id()
                )
            )
            .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponse>
        update(
            @PathVariable Long id,
            @RequestBody
            AppointmentUpdateRequest request
        ) {

        return ResponseEntity.ok(
            client.update(id, request)
        );
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AppointmentResponse>
        updateStatus(
            @PathVariable Long id,
            @RequestBody StatusRequest request
        ) {

        return ResponseEntity.ok(
            client.updateStatus(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void>
        delete(@PathVariable Long id) {

        client.delete(id);

        return ResponseEntity
            .noContent()
            .build();
    }
}