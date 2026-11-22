package cl.duoc.vidasalud.appointments.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import cl.duoc.vidasalud.appointments.entity.Appointment;
import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;
import cl.duoc.vidasalud.appointments.notification.AppointmentNotifier;
import cl.duoc.vidasalud.appointments.streaming.config.StreamingProperties;

/**
 * Publica en Kafka un {@link AppointmentEvent} por cada hecho de una cita.
 *
 * Complementa a RabbitMQ sin reemplazarlo: RabbitMQ entrega comandos de
 * trabajo ("manda este correo") y Kafka deja un registro de eventos que
 * reportes y auditoria leen cada uno con su consumer group.
 *
 * La clave del mensaje es el id de la cita, asi todos los eventos de una misma
 * cita caen en la misma particion y se consumen en orden.
 *
 * El envio es asincrono y nunca lanza excepciones: si Kafka no responde, la
 * cita igual se guarda y solo queda un aviso en el log.
 */
@Component
@ConditionalOnProperty(prefix = "vidasalud.streaming", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaAppointmentNotifier implements AppointmentNotifier {

    private static final Logger log = LoggerFactory.getLogger(KafkaAppointmentNotifier.class);

    private final KafkaTemplate<String, AppointmentEvent> kafkaTemplate;
    private final String topic;

    public KafkaAppointmentNotifier(
            KafkaTemplate<String, AppointmentEvent> kafkaTemplate,
            StreamingProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = properties.topics().appointmentsEvents();
    }

    @Override
    public void appointmentCreated(Appointment appointment) {
        publish(AppointmentEvent.created(appointment));
    }

    @Override
    public void statusChanged(Appointment appointment, AppointmentStatus previousStatus) {
        publish(AppointmentEvent.statusChanged(appointment, previousStatus));
    }

    private void publish(AppointmentEvent event) {
        try {
            kafkaTemplate.send(topic, event.key(), event)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.warn("No se pudo publicar el evento {} ({}) de la atencion {} en {}: {}",
                                    event.eventId(), event.type(), event.appointmentId(), topic,
                                    error.toString());
                        } else {
                            log.debug("Evento {} ({}) de la atencion {} publicado en {} particion {} offset {}",
                                    event.eventId(), event.type(), event.appointmentId(), topic,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (RuntimeException e) {
            // Ej.: Kafka caido y max.block.ms agotado al pedir metadatos.
            log.warn("No se pudo publicar el evento {} ({}) de la atencion {} en {}: {}",
                    event.eventId(), event.type(), event.appointmentId(), topic, e.toString());
        }
    }
}
