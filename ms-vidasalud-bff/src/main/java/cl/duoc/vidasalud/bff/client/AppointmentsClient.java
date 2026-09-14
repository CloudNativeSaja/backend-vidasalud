package cl.duoc.vidasalud.bff.client;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import cl.duoc.vidasalud.bff.dto.AppointmentResponse;

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

        if (response == null) {
            return List.of();
        }

        return Arrays.asList(response);
    }

    public AppointmentResponse findById(Long id) {

        return restClient.get()
            .uri("/api/appointments/{id}", id)
            .retrieve()
            .body(AppointmentResponse.class);
    }
}