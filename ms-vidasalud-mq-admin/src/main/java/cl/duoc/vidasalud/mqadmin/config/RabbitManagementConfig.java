package cl.duoc.vidasalud.mqadmin.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP hacia RabbitMQ Management (puerto 15672) con autenticacion basica.
 * Se usa para listar recursos y ver el estado del cluster, operaciones que
 * AmqpAdmin (protocolo AMQP) no ofrece.
 */
@Configuration
@EnableConfigurationProperties(MqAdminProperties.class)
public class RabbitManagementConfig {

    @Bean
    public RestClient rabbitManagementRestClient(MqAdminProperties properties) {
        MqAdminProperties.Management management = properties.management();

        String credentials = management.username() + ":" + management.password();
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return RestClient.builder()
                .baseUrl(management.url())
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth)
                .build();
    }
}
