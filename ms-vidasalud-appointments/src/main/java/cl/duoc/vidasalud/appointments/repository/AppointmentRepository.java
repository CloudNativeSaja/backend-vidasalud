package cl.duoc.vidasalud.appointments.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.vidasalud.appointments.entity.Appointment;
import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;

public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {

    List<Appointment> findByStatus(AppointmentStatus status);

    List<Appointment> findByAppointmentDateBetween(
        LocalDateTime from,
        LocalDateTime to
    );

    List<Appointment> findByStatusAndAppointmentDateBetween(
        AppointmentStatus status,
        LocalDateTime from,
        LocalDateTime to
    );
}