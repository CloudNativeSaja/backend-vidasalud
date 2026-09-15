package cl.duoc.vidasalud.bff.client;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import cl.duoc.vidasalud.bff.dto.AppointmentRequest;
import cl.duoc.vidasalud.bff.dto.AppointmentResponse;
import cl.duoc.vidasalud.bff.dto.AppointmentUpdateRequest;
import cl.duoc.vidasalud.bff.dto.StatusRequest;

@Component
public class AppointmentsClient {

    private final RestClient restClient;

    public AppointmentsClient(
            RestClient.Builder builder,
            @Value("${services.appointments.url}")
            String appointmentsUrl) {

        this.restClient = builder
            .baseUrl(appointmentsUrl)
            .build();
    }

    public List<AppointmentResponse> findAll() {

        AppointmentResponse[] response =
            restClient.get()
                .uri("/api/appointments")
                .retrieve()
                .body(AppointmentResponse[].class);

        return response == null
            ? List.of()
            : Arrays.asList(response);
    }

    public AppointmentResponse findById(Long id) {

        return restClient.get()
            .uri("/api/appointments/{id}", id)
            .retrieve()
            .body(AppointmentResponse.class);
    }

    public AppointmentResponse create(
            AppointmentRequest request) {

        return restClient.post()
            .uri("/api/appointments")
            .body(request)
            .retrieve()
            .body(AppointmentResponse.class);
    }

    public AppointmentResponse update(
            Long id,
            AppointmentUpdateRequest request) {

        return restClient.put()
            .uri("/api/appointments/{id}", id)
            .body(request)
            .retrieve()
            .body(AppointmentResponse.class);
    }

    public AppointmentResponse updateStatus(
            Long id,
            StatusRequest request) {

        return restClient.put()
            .uri("/api/appointments/{id}/status", id)
            .body(request)
            .retrieve()
            .body(AppointmentResponse.class);
    }

    public void delete(Long id) {

        restClient.delete()
            .uri("/api/appointments/{id}", id)
            .retrieve()
            .toBodilessEntity();
    }
}