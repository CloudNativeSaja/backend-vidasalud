package cl.duoc.vidasalud.bff.client;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import cl.duoc.vidasalud.bff.dto.HealthServiceResponse;

@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(
            RestClient.Builder builder,
            @Value("${services.catalog.url}")
            String catalogUrl) {

        this.restClient = builder
            .baseUrl(catalogUrl)
            .build();
    }

    public List<HealthServiceResponse> findServices() {

        HealthServiceResponse[] response =
            restClient.get()
                .uri("/api/catalog/services")
                .retrieve()
                .body(HealthServiceResponse[].class);

        if (response == null) {
            return List.of();
        }

        return Arrays.asList(response);
    }

    public HealthServiceResponse findServiceById(
            Long id) {

        return restClient.get()
            .uri(
                "/api/catalog/services/{id}",
                id
            )
            .retrieve()
            .body(HealthServiceResponse.class);
    }
}