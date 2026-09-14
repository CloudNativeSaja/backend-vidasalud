package cl.duoc.vidasalud.catalog.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "VS_HEALTH_SERVICES")
public class HealthService {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "health_service_seq"
    )
    @SequenceGenerator(
        name = "health_service_seq",
        sequenceName = "VS_HEALTH_SERVICE_SEQ",
        allocationSize = 1
    )
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false)
    private Boolean active = true;

    public HealthService() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}