package cl.duoc.vidasalud.appointments.notification;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import cl.duoc.vidasalud.appointments.entity.Appointment;
import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;

/**
 * Punto unico que ve el servicio de negocio: reparte cada aviso a todos los
 * canales activos (RabbitMQ, Kafka...).
 *
 * Cada canal se ejecuta aislado: si uno falla, los demas igual reciben el
 * aviso y la atencion ya guardada no se ve afectada.
 */
@Component
@Primary
public class CompositeAppointmentNotifier implements AppointmentNotifier {

    private static final Logger log = LoggerFactory.getLogger(CompositeAppointmentNotifier.class);

    private final List<AppointmentNotifier> delegates;

    /** Spring no incluye este mismo bean en la lista que inyecta. */
    public CompositeAppointmentNotifier(List<AppointmentNotifier> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public void appointmentCreated(Appointment appointment) {
        for (AppointmentNotifier delegate : delegates) {
            try {
                delegate.appointmentCreated(appointment);
            } catch (RuntimeException e) {
                log.warn("{} fallo al avisar la creacion de la atencion {}: {}",
                        delegate.getClass().getSimpleName(), appointment.getId(), e.toString());
            }
        }
    }

    @Override
    public void statusChanged(Appointment appointment, AppointmentStatus previousStatus) {
        for (AppointmentNotifier delegate : delegates) {
            try {
                delegate.statusChanged(appointment, previousStatus);
            } catch (RuntimeException e) {
                log.warn("{} fallo al avisar el cambio de estado de la atencion {}: {}",
                        delegate.getClass().getSimpleName(), appointment.getId(), e.toString());
            }
        }
    }
}
