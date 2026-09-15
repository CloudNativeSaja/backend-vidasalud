package cl.duoc.vidasalud.catalog.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "VS_CLINICAL_BOXES")
public class ClinicalBox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150)
    private String centerName;

    @Column(nullable = false)
    private Boolean active = true;

    public ClinicalBox() {
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

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}