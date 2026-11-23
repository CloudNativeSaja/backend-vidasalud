package cl.duoc.vidasalud.audit.listener;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import cl.duoc.vidasalud.audit.event.AppointmentEvent;
import cl.duoc.vidasalud.audit.event.AuditEvent;
import cl.duoc.vidasalud.audit.service.AuditMapper;
import cl.duoc.vidasalud.audit.service.AuditPublisher;

/**
 * Consumer group "auditoria": lee los eventos de citas (el mismo topico que
 * lee reportes, pero con su propio grupo y sus propios offsets) y publica un
 * evento de auditoria por cada uno.
 */
@Component
public class AppointmentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventListener.class);

    private final EventValidator validator;
    private final AuditMapper mapper;
    private final AuditPublisher publisher;

    public AppointmentEventListener(EventValidator validator, AuditMapper mapper, AuditPublisher publisher) {
        this.validator = validator;
        this.mapper = mapper;
        this.publisher = publisher;
    }

    @KafkaListener(
            id = "auditoria-citas",
            topics = "${vidasalud.kafka.topics.appointments-events}",
            groupId = "${vidasalud.kafka.groups.auditoria}",
            properties = "spring.json.value.default.type=cl.duoc.vidasalud.audit.event.AppointmentEvent")
    public void onAppointmentEvent(ConsumerRecord<String, AppointmentEvent> record) {
        AppointmentEvent event = validator.validate(record.value(), record.offset());

        AuditEvent audit = mapper.fromAppointment(event);
        publisher.publish(audit);

        log.info("Cita {} -> auditoria {} (particion={}, offset={})",
                event.appointmentId(), audit.accion(), record.partition(), record.offset());
    }
}
