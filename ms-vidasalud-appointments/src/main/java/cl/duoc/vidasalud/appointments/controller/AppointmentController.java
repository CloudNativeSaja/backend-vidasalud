package cl.duoc.vidasalud.appointments.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cl.duoc.vidasalud.appointments.dto.AppointmentRequest;
import cl.duoc.vidasalud.appointments.dto.AppointmentResponse;
import cl.duoc.vidasalud.appointments.dto.AppointmentUpdateRequest;
import cl.duoc.vidasalud.appointments.dto.StatusRequest;
import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;
import cl.duoc.vidasalud.appointments.service.AppointmentService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(
            AppointmentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(
            @Valid
            @RequestBody AppointmentRequest request) {

        AppointmentResponse created =
            service.create(request);

        return ResponseEntity
            .created(
                URI.create(
                    "/api/appointments/"
                    + created.id()
                )
            )
            .body(created);
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponse>>
        findAll(

            @RequestParam(required = false)
            AppointmentStatus status,

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
                status,
                from,
                to
            )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse>
        findById(
            @PathVariable Long id
        ) {

        return ResponseEntity.ok(
            service.findById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponse>
        update(
            @PathVariable Long id,
            @Valid
            @RequestBody
            AppointmentUpdateRequest request
        ) {

        return ResponseEntity.ok(
            service.update(
                id,
                request
            )
        );
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AppointmentResponse>
        updateStatus(
            @PathVariable Long id,
            @Valid
            @RequestBody StatusRequest request
        ) {

        return ResponseEntity.ok(
            service.updateStatus(
                id,
                request.status()
            )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void>
        delete(
            @PathVariable Long id
        ) {

        service.delete(id);

        return ResponseEntity
            .noContent()
            .build();
    }
}