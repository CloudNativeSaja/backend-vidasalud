package cl.duoc.vidasalud.audit.listener;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import cl.duoc.vidasalud.audit.exception.InvalidEventException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/** Valida el DTO recibido antes de procesarlo; si falla, el evento va a la DLT. */
@Component
public class EventValidator {

    private final Validator validator;

    public EventValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> T validate(T event, long offset) {
        if (event == null) {
            throw new InvalidEventException("Mensaje sin contenido en offset " + offset);
        }
        Set<ConstraintViolation<T>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new InvalidEventException("Evento invalido (" + detail + ")");
        }
        return event;
    }
}
