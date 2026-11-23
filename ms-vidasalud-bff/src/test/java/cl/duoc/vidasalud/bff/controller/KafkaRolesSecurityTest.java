package cl.duoc.vidasalud.bff.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;

/**
 * Matriz de roles de los endpoints de Kafka. Los microservicios internos no
 * estan levantados: un usuario autorizado recibe 503 del reenvio, y uno sin
 * el rol recibe 403 antes de llegar a ellos.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "services.report.url=http://127.0.0.1:1",
        "services.audit.url=http://127.0.0.1:1",
        "services.kafka-admin.url=http://127.0.0.1:1"
})
class KafkaRolesSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor rol(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/bff/report/kpis")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/bff/audit/events")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/bff/kafka/topics")).andExpect(status().isUnauthorized());
    }

    @Test
    void reportesSoloParaAdmin() throws Exception {
        mockMvc.perform(get("/api/bff/report/kpis").with(rol("ADMIN"))).andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/bff/report/kpis").with(rol("AUDITOR"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/bff/report/kpis").with(rol("RECEPCIONISTA"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/bff/report/kpis").with(rol("PACIENTE"))).andExpect(status().isForbidden());
    }

    @Test
    void auditoriaParaAdminYAuditor() throws Exception {
        mockMvc.perform(get("/api/bff/audit/events").with(rol("ADMIN"))).andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/bff/audit/events").with(rol("AUDITOR"))).andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/bff/audit/events").with(rol("RECEPCIONISTA"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/bff/audit/events").with(rol("PACIENTE"))).andExpect(status().isForbidden());
    }

    @Test
    void administracionDeKafkaSoloParaAdmin() throws Exception {
        mockMvc.perform(get("/api/bff/kafka/topics").with(rol("ADMIN"))).andExpect(status().isServiceUnavailable());
        mockMvc.perform(delete("/api/bff/kafka/topics/x").with(rol("ADMIN"))).andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/bff/kafka/topics").with(rol("AUDITOR"))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/bff/kafka/topics/x").with(rol("RECEPCIONISTA"))).andExpect(status().isForbidden());
    }
}
