package cl.duoc.vidasalud.mqadmin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * La API no debe permitir colas con nombres vacios o configuraciones invalidas.
 */
class CreateQueueRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void colaValidaNoTieneErrores() {
        CreateQueueRequest request = new CreateQueueRequest(
                "q.cmd.sms", true, "classic", "cmd.dead.dlx", null, true, 60000L, 1000L);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void nombreVacioEsRechazado() {
        CreateQueueRequest request = new CreateQueueRequest(
                "", null, null, null, null, null, null, null);

        assertThat(messages(validator.validate(request)))
                .contains("El nombre de la cola es obligatorio");
    }

    @Test
    void prefijoReservadoAmqEsRechazado() {
        CreateQueueRequest request = new CreateQueueRequest(
                "amq.mi-cola", null, null, null, null, null, null, null);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void tipoDeColaInvalidoEsRechazado() {
        CreateQueueRequest request = new CreateQueueRequest(
                "q.prueba", null, "stream-x", null, null, null, null, null);

        assertThat(messages(validator.validate(request)))
                .contains("El tipo de cola debe ser 'classic' o 'quorum'");
    }

    @Test
    void dlqSinDeadLetterExchangeEsRechazada() {
        CreateQueueRequest request = new CreateQueueRequest(
                "q.prueba", true, "classic", null, null, true, null, null);

        assertThat(messages(validator.validate(request)))
                .contains("deadLetterRoutingKey y withDeadLetterQueue requieren deadLetterExchange");
    }

    @Test
    void quorumNoDurableEsRechazada() {
        CreateQueueRequest request = new CreateQueueRequest(
                "q.prueba", false, "quorum", null, null, null, null, null);

        assertThat(messages(validator.validate(request)))
                .contains("Una cola quorum debe ser durable");
    }

    @Test
    void ttlNegativoEsRechazado() {
        CreateQueueRequest request = new CreateQueueRequest(
                "q.prueba", true, "classic", null, null, null, -5L, null);

        assertThat(messages(validator.validate(request)))
                .contains("El TTL debe ser mayor a 0");
    }

    private static Set<String> messages(Set<ConstraintViolation<CreateQueueRequest>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }
}
