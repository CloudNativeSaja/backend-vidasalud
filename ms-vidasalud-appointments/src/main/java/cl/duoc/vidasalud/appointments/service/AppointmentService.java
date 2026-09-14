package cl.duoc.vidasalud.appointments.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.vidasalud.appointments.dto.AppointmentRequest;
import cl.duoc.vidasalud.appointments.dto.AppointmentResponse;
import cl.duoc.vidasalud.appointments.dto.AppointmentUpdateRequest;
import cl.duoc.vidasalud.appointments.entity.Appointment;
import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;
import cl.duoc.vidasalud.appointments.exception.BusinessRuleException;
import cl.duoc.vidasalud.appointments.exception.ResourceNotFoundException;
import cl.duoc.vidasalud.appointments.repository.AppointmentRepository;

@Service
public class AppointmentService {

    private final AppointmentRepository repository;

    public AppointmentService(
            AppointmentRepository repository) {
        this.repository = repository;
    }

    // CREATE
    public AppointmentResponse create(
            AppointmentRequest request) {

        Appointment appointment = new Appointment();

        appointment.setPatientId(request.patientId());
        appointment.setServiceId(request.serviceId());
        appointment.setBoxId(request.boxId());
        appointment.setAppointmentDate(
            request.appointmentDate()
        );
        appointment.setStatus(
            AppointmentStatus.SOLICITADA
        );

        return toResponse(
            repository.save(appointment)
        );
    }

    // READ - TODOS / FILTROS
    public List<AppointmentResponse> findAll(
            AppointmentStatus status,
            LocalDateTime from,
            LocalDateTime to) {

        List<Appointment> appointments;

        if (status != null && from != null && to != null) {

            appointments =
                repository.findByStatusAndAppointmentDateBetween(
                    status,
                    from,
                    to
                );

        } else if (status != null) {

            appointments =
                repository.findByStatus(status);

        } else if (from != null && to != null) {

            appointments =
                repository.findByAppointmentDateBetween(
                    from,
                    to
                );

        } else {

            appointments =
                repository.findAll();
        }

        return appointments
            .stream()
            .map(this::toResponse)
            .toList();
    }

    // READ - ID
    public AppointmentResponse findById(Long id) {

        Appointment appointment =
            getAppointment(id);

        return toResponse(appointment);
    }

    // UPDATE
    public AppointmentResponse update(
            Long id,
            AppointmentUpdateRequest request) {

        Appointment appointment =
            getAppointment(id);

        if (appointment.getStatus()
                == AppointmentStatus.CERRADA) {

            throw new BusinessRuleException(
                "No se puede modificar una atención cerrada"
            );
        }

        if (appointment.getStatus()
                == AppointmentStatus.CANCELADA) {

            throw new BusinessRuleException(
                "No se puede modificar una atención cancelada"
            );
        }

        appointment.setServiceId(
            request.serviceId()
        );

        appointment.setBoxId(
            request.boxId()
        );

        appointment.setAppointmentDate(
            request.appointmentDate()
        );

        return toResponse(
            repository.save(appointment)
        );
    }

    // UPDATE STATUS
    public AppointmentResponse updateStatus(
            Long id,
            AppointmentStatus newStatus) {

        Appointment appointment =
            getAppointment(id);

        validateStatusTransition(
            appointment.getStatus(),
            newStatus
        );

        appointment.setStatus(newStatus);

        return toResponse(
            repository.save(appointment)
        );
    }

    // DELETE
    public void delete(Long id) {

        Appointment appointment =
            getAppointment(id);

        repository.delete(appointment);
    }

    private Appointment getAppointment(Long id) {

        return repository.findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "No existe una atención con ID " + id
                )
            );
    }

    private void validateStatusTransition(
            AppointmentStatus current,
            AppointmentStatus next) {

        if (current == next) {
            throw new BusinessRuleException(
                "La atención ya se encuentra en estado " + current
            );
        }

        boolean valid = switch (current) {

            case SOLICITADA ->
                next == AppointmentStatus.CONFIRMADA ||
                next == AppointmentStatus.CANCELADA;

            case CONFIRMADA ->
                next == AppointmentStatus.EN_ESPERA ||
                next == AppointmentStatus.CANCELADA;

            case EN_ESPERA ->
                next == AppointmentStatus.EN_ATENCION ||
                next == AppointmentStatus.CANCELADA;

            case EN_ATENCION ->
                next == AppointmentStatus.CERRADA;

            case CERRADA, CANCELADA ->
                false;
        };

        if (!valid) {
            throw new BusinessRuleException(
                "Transición de estado inválida: "
                + current + " -> " + next
            );
        }
    }

    private AppointmentResponse toResponse(
            Appointment appointment) {

        return new AppointmentResponse(
            appointment.getId(),
            appointment.getPatientId(),
            appointment.getServiceId(),
            appointment.getBoxId(),
            appointment.getAppointmentDate(),
            appointment.getStatus(),
            appointment.getCreatedAt(),
            appointment.getUpdatedAt()
        );
    }
}