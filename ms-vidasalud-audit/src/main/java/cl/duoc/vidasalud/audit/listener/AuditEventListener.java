package cl.duoc.vidasalud.audit.listener;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import cl.duoc.vidasalud.audit.event.AuditEvent;
import cl.duoc.vidasalud.audit.service.AuditTimelineService;

/**
 * Consumer group "auditoria-timeline": lee el topico de auditoria y arma la
 * linea de tiempo que consulta el frontend.
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final EventValidator validator;
    private final AuditTimelineService timeline;

    public AuditEventListener(EventValidator validator, AuditTimelineService timeline) {
        this.validator = validator;
        this.timeline = timeline;
    }

    @KafkaListener(
            id = "auditoria-timeline",
            topics = "${vidasalud.kafka.topics.audit-events}",
            groupId = "${vidasalud.kafka.groups.timeline}",
            properties = "spring.json.value.default.type=cl.duoc.vidasalud.audit.event.AuditEvent")
    public void onAuditEvent(ConsumerRecord<String, AuditEvent> record) {
        AuditEvent event = validator.validate(record.value(), record.offset());

        if (timeline.add(event)) {
            log.info("Linea de tiempo: {} {} (particion={}, offset={})",
                    event.accion(), event.correlationId(), record.partition(), record.offset());
        } else {
            log.info("Evento de auditoria duplicado descartado: {}", event.eventId());
        }
    }
}
