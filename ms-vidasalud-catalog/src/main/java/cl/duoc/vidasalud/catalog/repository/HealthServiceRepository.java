package cl.duoc.vidasalud.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.vidasalud.catalog.entity.HealthService;

public interface HealthServiceRepository
        extends JpaRepository<HealthService, Long> {

    List<HealthService> findByActive(Boolean active);
}