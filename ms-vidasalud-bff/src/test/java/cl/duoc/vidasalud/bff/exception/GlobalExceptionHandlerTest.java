package cl.duoc.vidasalud.bff.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new FailingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void reenviaElCodigoYElCuerpoDelMicroservicio() throws Exception {
        mockMvc.perform(get("/fail/bad-request"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("El cupo debe corresponder a una fecha futura"));
    }

    @Test
    void reenviaConflictoDeReglaDeNegocio() throws Exception {
        mockMvc.perform(get("/fail/conflict"))
            .andExpect(status().isConflict());
    }

    @Test
    void respondeCuerpoPorDefectoSiElMicroservicioNoEnviaCuerpo() throws Exception {
        mockMvc.perform(get("/fail/not-found-empty"))
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"message\":\"Not Found\"}"));
    }

    @Test
    void respondeServiceUnavailableSiElMicroservicioNoResponde() throws Exception {
        mockMvc.perform(get("/fail/unavailable"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value(503));
    }

    @RestController
    static class FailingController {

        @GetMapping("/fail/bad-request")
        String badRequest() {
            throw new RestClientResponseException("400", HttpStatus.BAD_REQUEST.value(), "Bad Request",
                new HttpHeaders(),
                "{\"message\":\"El cupo debe corresponder a una fecha futura\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        }

        @GetMapping("/fail/conflict")
        String conflict() {
            throw new RestClientResponseException("409", HttpStatus.CONFLICT.value(), "Conflict",
                new HttpHeaders(), "{\"message\":\"Ya existe un cupo\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        }

        @GetMapping("/fail/not-found-empty")
        String notFoundEmpty() {
            throw new RestClientResponseException("404", HttpStatus.NOT_FOUND.value(), "Not Found",
                new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
        }

        @GetMapping("/fail/unavailable")
        String unavailable() {
            throw new ResourceAccessException("Connection refused");
        }
    }
}
