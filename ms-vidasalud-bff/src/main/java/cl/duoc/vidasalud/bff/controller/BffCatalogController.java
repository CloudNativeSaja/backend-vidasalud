package cl.duoc.vidasalud.bff.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cl.duoc.vidasalud.bff.client.CatalogClient;
import cl.duoc.vidasalud.bff.dto.HealthServiceRequest;
import cl.duoc.vidasalud.bff.dto.HealthServiceResponse;
import cl.duoc.vidasalud.bff.dto.ClinicalBoxRequest;
import cl.duoc.vidasalud.bff.dto.ClinicalBoxResponse;
import cl.duoc.vidasalud.bff.dto.SlotRequest;
import cl.duoc.vidasalud.bff.dto.SlotResponse;

@RestController
@RequestMapping("/api/bff/catalog")
public class BffCatalogController {

    private final CatalogClient client;

    public BffCatalogController(
            CatalogClient client) {
        this.client = client;
    }

    @GetMapping("/services")
    public ResponseEntity<List<HealthServiceResponse>>
        findServices() {

        return ResponseEntity.ok(
            client.findServices()
        );
    }

    @GetMapping("/services/{id}")
    public ResponseEntity<HealthServiceResponse>
        findServiceById(
            @PathVariable Long id
        ) {

        return ResponseEntity.ok(
            client.findServiceById(id)
        );
    }

    @PostMapping("/services")
    public ResponseEntity<HealthServiceResponse>
        createService(
            @RequestBody
            HealthServiceRequest request
        ) {

        HealthServiceResponse created =
            client.createService(request);

        return ResponseEntity
            .created(
                URI.create(
                    "/api/bff/catalog/services/"
                    + created.id()
                )
            )
            .body(created);
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<HealthServiceResponse>
        updateService(
            @PathVariable Long id,
            @RequestBody
            HealthServiceRequest request
        ) {

        return ResponseEntity.ok(
            client.updateService(
                id,
                request
            )
        );
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void>
        deleteService(
            @PathVariable Long id
        ) {

        client.deleteService(id);

        return ResponseEntity
            .noContent()
            .build();
    }

    @GetMapping("/boxes")
public ResponseEntity<List<ClinicalBoxResponse>>
        findBoxes() {

    return ResponseEntity.ok(
        client.findBoxes()
    );
}

@GetMapping("/boxes/{id}")
public ResponseEntity<ClinicalBoxResponse>
        findBoxById(
            @PathVariable Long id
        ) {

    return ResponseEntity.ok(
        client.findBoxById(id)
    );
}

@PostMapping("/boxes")
public ResponseEntity<ClinicalBoxResponse>
        createBox(
            @RequestBody ClinicalBoxRequest request
        ) {

    ClinicalBoxResponse created =
        client.createBox(request);

    return ResponseEntity
        .created(
            URI.create(
                "/api/bff/catalog/boxes/"
                + created.id()
            )
        )
        .body(created);
}

@PutMapping("/boxes/{id}")
public ResponseEntity<ClinicalBoxResponse>
        updateBox(
            @PathVariable Long id,
            @RequestBody ClinicalBoxRequest request
        ) {

    return ResponseEntity.ok(
        client.updateBox(id, request)
    );
}

@DeleteMapping("/boxes/{id}")
public ResponseEntity<Void>
        deleteBox(
            @PathVariable Long id
        ) {

    client.deleteBox(id);

    return ResponseEntity
        .noContent()
        .build();
}
@GetMapping("/slots")
public ResponseEntity<List<SlotResponse>>
        findSlots() {

    return ResponseEntity.ok(
        client.findSlots()
    );
}

@GetMapping("/slots/{id}")
public ResponseEntity<SlotResponse>
        findSlotById(
            @PathVariable Long id
        ) {

    return ResponseEntity.ok(
        client.findSlotById(id)
    );
}

@PostMapping("/slots")
public ResponseEntity<SlotResponse>
        createSlot(
            @RequestBody SlotRequest request
        ) {

    SlotResponse created =
        client.createSlot(request);

    return ResponseEntity
        .created(
            URI.create(
                "/api/bff/catalog/slots/"
                + created.id()
            )
        )
        .body(created);
}

@PutMapping("/slots/{id}")
public ResponseEntity<SlotResponse>
        updateSlot(
            @PathVariable Long id,
            @RequestBody SlotRequest request
        ) {

    return ResponseEntity.ok(
        client.updateSlot(id, request)
    );
}

@PutMapping("/slots/{id}/reserve")
public ResponseEntity<SlotResponse>
        reserveSlot(
            @PathVariable Long id
        ) {

    return ResponseEntity.ok(
        client.reserveSlot(id)
    );
}

@PutMapping("/slots/{id}/release")
public ResponseEntity<SlotResponse>
        releaseSlot(
            @PathVariable Long id
        ) {

    return ResponseEntity.ok(
        client.releaseSlot(id)
    );
}

@DeleteMapping("/slots/{id}")
public ResponseEntity<Void>
        deleteSlot(
            @PathVariable Long id
        ) {

    client.deleteSlot(id);

    return ResponseEntity
        .noContent()
        .build();
}
}