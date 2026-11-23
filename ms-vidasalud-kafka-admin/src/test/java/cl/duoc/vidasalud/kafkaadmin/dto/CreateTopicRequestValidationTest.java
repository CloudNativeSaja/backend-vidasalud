package cl.duoc.vidasalud.kafkaadmin.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class CreateTopicRequestValidationTest {

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

    private static Set<ConstraintViolation<CreateTopicRequest>> validate(
            String name, Integer partitions, Short replicationFactor) {
        return validator.validate(new CreateTopicRequest(name, partitions, replicationFactor, null));
    }

    @Test
    void nombreValidoYValoresPorDefectoSonAceptados() {
        assertTrue(validate("vidasalud.demo-1_a", null, null).isEmpty());
        assertTrue(validate("vidasalud.demo", 3, (short) 3).isEmpty());
    }

    @Test
    void nombreVacioONulo() {
        assertEquals(1, validate("", 3, (short) 3).stream()
                .filter(v -> v.getPropertyPath().toString().equals("name")).count() >= 1 ? 1 : 0);
        assertTrue(validate(null, 3, (short) 3).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void nombreConCaracteresInvalidos() {
        assertTrue(validate("mi topico", 3, (short) 3).stream()
                .anyMatch(v -> v.getMessage().contains("solo admite")));
        assertTrue(validate("topico/raro", 3, (short) 3).stream()
                .anyMatch(v -> v.getMessage().contains("solo admite")));
    }

    @Test
    void particionesYReplicacionFueraDeRango() {
        assertFalse(validate("t", 0, (short) 3).isEmpty());
        assertFalse(validate("t", 101, (short) 3).isEmpty());
        assertFalse(validate("t", 3, (short) 0).isEmpty());
        assertFalse(validate("t", 3, (short) 11).isEmpty());
    }

    private static void assertFalse(boolean condition) {
        org.junit.jupiter.api.Assertions.assertFalse(condition);
    }
}
