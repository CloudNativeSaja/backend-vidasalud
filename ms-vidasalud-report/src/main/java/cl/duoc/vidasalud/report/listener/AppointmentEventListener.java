package cl.duoc.vidasalud.report.listener;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import cl.duoc.vidasalud.report.event.AppointmentEvent;
import cl.duoc.vidasalud.report.exception.InvalidEventException;
import cl.duoc.vidasalud.report.service.KpiService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Consumidor de eventos de citas (consumer group "reportes").
 *
 * Solo hace de puente: valida el mensaje y delega en {@link KpiService}.
 * Los reintentos y la DLT los maneja el DefaultErrorHandler configurado en
 * KafkaConsumerConfig.
 *
 * Con 3 particiones y concurrency=3, cada hilo atiende una particion. Si se
 * levanta otra instancia del servicio con el mismo group-id, Kafka reparte
 * las particiones entre ambas (balanceo de carga).
 */
@Component
public class AppointmentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventListener.class);

    private final KpiService kpiService;
    private final Validator validator;

    public AppointmentEventListener(KpiService kpiService, Validator validator) {
        this.kpiService = kpiService;
        this.validator = validator;
    }

    @KafkaListener(
            id = "reportes-citas",
            topics = "${vidasalud.kafka.topics.appointments-events}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onAppointmentEvent(ConsumerRecord<String, AppointmentEvent> record) {
        AppointmentEvent event = record.value();
        validate(event, record);

        boolean applied = kpiService.apply(event);
        if (applied) {
            log.info("KPI actualizado: evento={} tipo={} cita={} estado={} (particion={}, offset={})",
                    event.eventId(), event.type(), event.appointmentId(), event.newStatus(),
                    record.partition(), record.offset());
        } else {
            log.info("Evento duplicado descartado: {} (particion={}, offset={})",
                    event.eventId(), record.partition(), record.offset());
        }
    }

    private void validate(AppointmentEvent event, ConsumerRecord<String, AppointmentEvent> record) {
        if (event == null) {
            throw new InvalidEventException("Mensaje sin contenido en offset " + record.offset());
        }
        Set<ConstraintViolation<AppointmentEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new InvalidEventException("Evento invalido (" + detail + ")");
        }
    }
}
