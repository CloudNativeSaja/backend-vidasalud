package cl.duoc.vidasalud.bff.client;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import cl.duoc.vidasalud.bff.dto.HealthServiceRequest;
import cl.duoc.vidasalud.bff.dto.HealthServiceResponse;
import cl.duoc.vidasalud.bff.dto.ClinicalBoxRequest;
import cl.duoc.vidasalud.bff.dto.ClinicalBoxResponse;
import cl.duoc.vidasalud.bff.dto.SlotRequest;
import cl.duoc.vidasalud.bff.dto.SlotResponse;

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

        return response == null
            ? List.of()
            : Arrays.asList(response);
    }

    public HealthServiceResponse findServiceById(
            Long id) {

        return restClient.get()
            .uri("/api/catalog/services/{id}", id)
            .retrieve()
            .body(HealthServiceResponse.class);
    }

    public HealthServiceResponse createService(
            HealthServiceRequest request) {

        return restClient.post()
            .uri("/api/catalog/services")
            .body(request)
            .retrieve()
            .body(HealthServiceResponse.class);
    }

    public HealthServiceResponse updateService(
            Long id,
            HealthServiceRequest request) {

        return restClient.put()
            .uri("/api/catalog/services/{id}", id)
            .body(request)
            .retrieve()
            .body(HealthServiceResponse.class);
    }

    public void deleteService(Long id) {

        restClient.delete()
            .uri("/api/catalog/services/{id}", id)
            .retrieve()
            .toBodilessEntity();
    }
    public List<ClinicalBoxResponse> findBoxes() {

    ClinicalBoxResponse[] response =
        restClient.get()
            .uri("/api/catalog/boxes")
            .retrieve()
            .body(ClinicalBoxResponse[].class);

    return response == null
        ? List.of()
        : Arrays.asList(response);
}

public ClinicalBoxResponse findBoxById(Long id) {

    return restClient.get()
        .uri("/api/catalog/boxes/{id}", id)
        .retrieve()
        .body(ClinicalBoxResponse.class);
}

public ClinicalBoxResponse createBox(
        ClinicalBoxRequest request) {

    return restClient.post()
        .uri("/api/catalog/boxes")
        .body(request)
        .retrieve()
        .body(ClinicalBoxResponse.class);
}

public ClinicalBoxResponse updateBox(
        Long id,
        ClinicalBoxRequest request) {

    return restClient.put()
        .uri("/api/catalog/boxes/{id}", id)
        .body(request)
        .retrieve()
        .body(ClinicalBoxResponse.class);
}

public void deleteBox(Long id) {

    restClient.delete()
        .uri("/api/catalog/boxes/{id}", id)
        .retrieve()
        .toBodilessEntity();
}
public List<SlotResponse> findSlots() {

    SlotResponse[] response =
        restClient.get()
            .uri("/api/catalog/slots")
            .retrieve()
            .body(SlotResponse[].class);

    return response == null
        ? List.of()
        : Arrays.asList(response);
}

public SlotResponse findSlotById(Long id) {

    return restClient.get()
        .uri("/api/catalog/slots/{id}", id)
        .retrieve()
        .body(SlotResponse.class);
}

public SlotResponse createSlot(
        SlotRequest request) {

    return restClient.post()
        .uri("/api/catalog/slots")
        .body(request)
        .retrieve()
        .body(SlotResponse.class);
}

public SlotResponse updateSlot(
        Long id,
        SlotRequest request) {

    return restClient.put()
        .uri("/api/catalog/slots/{id}", id)
        .body(request)
        .retrieve()
        .body(SlotResponse.class);
}

public SlotResponse reserveSlot(Long id) {

    return restClient.put()
        .uri("/api/catalog/slots/{id}/reserve", id)
        .retrieve()
        .body(SlotResponse.class);
}

public SlotResponse releaseSlot(Long id) {

    return restClient.put()
        .uri("/api/catalog/slots/{id}/release", id)
        .retrieve()
        .body(SlotResponse.class);
}

public void deleteSlot(Long id) {

    restClient.delete()
        .uri("/api/catalog/slots/{id}", id)
        .retrieve()
        .toBodilessEntity();
}
}