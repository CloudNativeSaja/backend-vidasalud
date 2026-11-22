package cl.duoc.vidasalud.appointments.streaming.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Activa la configuracion del productor Kafka.
 *
 * El KafkaTemplate lo construye Spring Boot a partir del bloque "spring.kafka"
 * de application.yaml (bootstrap servers, serializers, acks, idempotencia).
 * Aqui solo se registran los nombres de topicos, separados de la logica de
 * negocio.
 */
@Configuration
@EnableConfigurationProperties(StreamingProperties.class)
@ConditionalOnProperty(prefix = "vidasalud.streaming", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaProducerConfig {
}
