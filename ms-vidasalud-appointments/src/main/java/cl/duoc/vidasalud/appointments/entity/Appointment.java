package cl.duoc.vidasalud.appointments.entity;

import java.time.LocalDateTime;

import cl.duoc.vidasalud.appointments.enums.AppointmentStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "VS_APPOINTMENTS")
public class Appointment {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "appointment_seq"
    )
    @SequenceGenerator(
        name = "appointment_seq",
        sequenceName = "VS_APPOINTMENT_SEQ",
        allocationSize = 1
    )
    private Long id;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private Long serviceId;

    private Long boxId;

    @Column(nullable = false)
    private LocalDateTime appointmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = AppointmentStatus.SOLICITADA;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Appointment() {
    }

    public Long getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Long getBoxId() {
        return boxId;
    }

    public void setBoxId(Long boxId) {
        this.boxId = boxId;
    }

    public LocalDateTime getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDateTime appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}