package cl.duoc.vidasalud.bff.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;

class ForwardingClientTest {

    private HttpServer server;
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private final AtomicReference<String> receivedPathAndQuery = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            receivedPathAndQuery.set(exchange.getRequestURI().toString());
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            boolean conflict = exchange.getRequestURI().getPath().endsWith("/conflicto");
            byte[] json = (conflict ? "{\"message\":\"ya existe\"}" : "{\"ok\":true}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(conflict ? 409 : 200, json.length);
            exchange.getResponseBody().write(json);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private ForwardingClient client(String baseUrl) {
        return new ForwardingClient(RestClient.builder(), baseUrl, "ms-vidasalud-prueba") { };
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void reenviaRutaConsultaYCuerpoYDevuelveLaRespuesta() {
        ResponseEntity<String> response = client(baseUrl())
                .forward(HttpMethod.POST, "/api/kafka/topics?x=1", "{\"name\":\"t\"}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"ok\":true}", response.getBody());
        assertEquals("/api/kafka/topics?x=1", receivedPathAndQuery.get());
        assertEquals("{\"name\":\"t\"}", receivedBody.get());
    }

    @Test
    void conservaElCodigoDeErrorDelMicroservicio() {
        ResponseEntity<String> response = client(baseUrl()).forward(HttpMethod.GET, "/api/kafka/conflicto", null);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("{\"message\":\"ya existe\"}", response.getBody());
    }

    @Test
    void sinMicroservicioResponde503() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        ResponseEntity<String> response = client("http://127.0.0.1:" + closedPort)
                .forward(HttpMethod.GET, "/api/report/kpis", null);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertTrue(response.getBody().contains("ms-vidasalud-prueba no esta disponible"));
    }
}
