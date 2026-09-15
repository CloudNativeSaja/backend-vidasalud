package cl.duoc.vidasalud.catalog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "VS_SLOTS")
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "box_id", nullable = false)
    private ClinicalBox clinicalBox;

    @ManyToOne(optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private HealthService healthService;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private Boolean available = true;

    public Slot() {
    }

    public Long getId() {
        return id;
    }

    public ClinicalBox getClinicalBox() {
        return clinicalBox;
    }

    public void setClinicalBox(ClinicalBox clinicalBox) {
        this.clinicalBox = clinicalBox;
    }

    public HealthService getHealthService() {
        return healthService;
    }

    public void setHealthService(HealthService healthService) {
        this.healthService = healthService;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}