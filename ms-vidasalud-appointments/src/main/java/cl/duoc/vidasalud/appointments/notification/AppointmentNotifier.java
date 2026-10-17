package cl.duoc.vidasalud.appointments.notification;

import cl.duoc.vidasalud.appointments.entity.Appointment;
import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;

/**
 * Puerto de salida del dominio de atenciones.
 *
 * El servicio de negocio solo avisa "que paso" con la atencion; no sabe
 * nada de RabbitMQ, exchanges ni routing keys. La implementacion concreta
 * (mensajeria) vive en el paquete messaging.
 */
public interface AppointmentNotifier {

    /** Se registro una nueva atencion (estado SOLICITADA). */
    void appointmentCreated(Appointment appointment);

    /** La atencion cambio de estado. */
    void statusChanged(Appointment appointment, AppointmentStatus previousStatus);
}
