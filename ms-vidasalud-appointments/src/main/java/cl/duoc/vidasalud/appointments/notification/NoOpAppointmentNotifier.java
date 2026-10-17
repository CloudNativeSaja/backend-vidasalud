package cl.duoc.vidasalud.appointments.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cl.duoc.vidasalud.appointments.entity.Appointment;
import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;

/**
 * Implementacion vacia usada cuando la mensajeria esta desactivada
 * (vidasalud.messaging.enabled=false). La API de citas funciona igual.
 */
@Component
@ConditionalOnProperty(prefix = "vidasalud.messaging", name = "enabled", havingValue = "false")
public class NoOpAppointmentNotifier implements AppointmentNotifier {

    private static final Logger log = LoggerFactory.getLogger(NoOpAppointmentNotifier.class);

    @Override
    public void appointmentCreated(Appointment appointment) {
        log.debug("Mensajeria desactivada: no se publica la creacion de la atencion {}", appointment.getId());
    }

    @Override
    public void statusChanged(Appointment appointment, AppointmentStatus previousStatus) {
        log.debug("Mensajeria desactivada: no se publica el cambio de estado de la atencion {}", appointment.getId());
    }
}
