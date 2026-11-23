package cl.duoc.vidasalud.kafkaadmin.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Cliente de administracion de Kafka. Parte de la configuracion estandar de
 * Spring Boot (spring.kafka) y acota los tiempos de espera para que la API
 * responda rapido cuando el cluster no esta disponible.
 */
@Configuration
@EnableConfigurationProperties(KafkaAdminProperties.class)
public class KafkaAdminConfig {

    @Bean(destroyMethod = "close")
    public Admin kafkaAdminClient(KafkaAdmin kafkaAdmin, KafkaAdminProperties properties) {
        Map<String, Object> config = new HashMap<>(kafkaAdmin.getConfigurationProperties());
        int timeoutMs = (int) properties.timeout().toMillis();
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, timeoutMs);
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, timeoutMs);
        return Admin.create(config);
    }
}
