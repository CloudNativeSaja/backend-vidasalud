package cl.duoc.vidasalud.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.vidasalud.catalog.entity.ClinicalBox;

public interface ClinicalBoxRepository
        extends JpaRepository<ClinicalBox, Long> {

    List<ClinicalBox> findByActive(Boolean active);
}