package cl.duoc.vidasalud.bff.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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



    // Consultar todas las citas
    // Admin, Recepcionista y Auditor pueden visualizar
    @GetMapping
    @PreAuthorize(
        "hasAnyRole('ADMIN','RECEPCIONISTA','AUDITOR')"
    )
    public ResponseEntity<List<AppointmentResponse>>
        findAll() {

        return ResponseEntity.ok(
            client.findAll()
        );
    }




    // Consultar cita específica
    // Paciente puede consultar,
    // además de roles administrativos
    @GetMapping("/{id}")
    @PreAuthorize(
        "hasAnyRole('ADMIN','PACIENTE','RECEPCIONISTA','AUDITOR')"
    )
    public ResponseEntity<AppointmentResponse>
        findById(
            @PathVariable Long id
        ) {

        return ResponseEntity.ok(
            client.findById(id)
        );
    }




    // Crear cita
    // Paciente solicita atención
    // Recepcionista puede crear manualmente
    @PostMapping
    @PreAuthorize(
        "hasAnyRole('ADMIN','PACIENTE','RECEPCIONISTA')"
    )
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





    // Actualizar datos de una cita
    // Gestión administrativa
    @PutMapping("/{id}")
    @PreAuthorize(
        "hasAnyRole('ADMIN','RECEPCIONISTA')"
    )
    public ResponseEntity<AppointmentResponse>
        update(
            @PathVariable Long id,
            @RequestBody AppointmentUpdateRequest request
        ) {

        return ResponseEntity.ok(
            client.update(id, request)
        );
    }





    // Cambiar estado:
    // CONFIRMADA, CANCELADA, ATENDIDA, etc.
    @PutMapping("/{id}/status")
    @PreAuthorize(
        "hasAnyRole('ADMIN','RECEPCIONISTA')"
    )
    public ResponseEntity<AppointmentResponse>
        updateStatus(
            @PathVariable Long id,
            @RequestBody StatusRequest request
        ) {

        return ResponseEntity.ok(
            client.updateStatus(id, request)
        );
    }





    // Eliminar cita
    // Solo administración o recepción
    @DeleteMapping("/{id}")
    @PreAuthorize(
        "hasAnyRole('ADMIN','RECEPCIONISTA')"
    )
    public ResponseEntity<Void>
        delete(
            @PathVariable Long id
        ) {


        client.delete(id);


        return ResponseEntity
            .noContent()
            .build();
    }

}