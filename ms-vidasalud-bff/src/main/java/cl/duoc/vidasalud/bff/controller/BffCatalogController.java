package cl.duoc.vidasalud.bff.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cl.duoc.vidasalud.bff.client.CatalogClient;
import cl.duoc.vidasalud.bff.dto.HealthServiceResponse;

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
                @PathVariable Long id) {

        return ResponseEntity.ok(
            client.findServiceById(id)
        );
    }
}