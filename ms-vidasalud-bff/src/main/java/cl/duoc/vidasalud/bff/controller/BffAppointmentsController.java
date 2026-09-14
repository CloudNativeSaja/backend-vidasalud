package cl.duoc.vidasalud.bff.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cl.duoc.vidasalud.bff.client.AppointmentsClient;
import cl.duoc.vidasalud.bff.dto.AppointmentResponse;

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
}