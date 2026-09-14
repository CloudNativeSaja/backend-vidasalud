package cl.duoc.vidasalud.catalog.service;

import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.vidasalud.catalog.dto.HealthServiceRequest;
import cl.duoc.vidasalud.catalog.dto.HealthServiceResponse;
import cl.duoc.vidasalud.catalog.entity.HealthService;
import cl.duoc.vidasalud.catalog.exception.ResourceNotFoundException;
import cl.duoc.vidasalud.catalog.repository.HealthServiceRepository;

@Service
public class HealthServiceService {

    private final HealthServiceRepository repository;

    public HealthServiceService(
            HealthServiceRepository repository) {
        this.repository = repository;
    }

    public HealthServiceResponse create(
            HealthServiceRequest request) {

        HealthService healthService =
            new HealthService();

        healthService.setName(request.name());
        healthService.setDescription(
            request.description()
        );
        healthService.setPrice(request.price());
        healthService.setDurationMinutes(
            request.durationMinutes()
        );

        healthService.setActive(
            request.active() != null
                ? request.active()
                : true
        );

        return toResponse(
            repository.save(healthService)
        );
    }

    public List<HealthServiceResponse> findAll(
            Boolean active) {

        List<HealthService> services =
            active == null
                ? repository.findAll()
                : repository.findByActive(active);

        return services.stream()
            .map(this::toResponse)
            .toList();
    }

    public HealthServiceResponse findById(Long id) {

        return toResponse(
            getHealthService(id)
        );
    }

    public HealthServiceResponse update(
            Long id,
            HealthServiceRequest request) {

        HealthService healthService =
            getHealthService(id);

        healthService.setName(request.name());
        healthService.setDescription(
            request.description()
        );
        healthService.setPrice(request.price());
        healthService.setDurationMinutes(
            request.durationMinutes()
        );

        if (request.active() != null) {
            healthService.setActive(request.active());
        }

        return toResponse(
            repository.save(healthService)
        );
    }

    public void delete(Long id) {

        HealthService healthService =
            getHealthService(id);

        repository.delete(healthService);
    }

    private HealthService getHealthService(Long id) {

        return repository.findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "No existe una prestación con ID "
                    + id
                )
            );
    }

    private HealthServiceResponse toResponse(
            HealthService healthService) {

        return new HealthServiceResponse(
            healthService.getId(),
            healthService.getName(),
            healthService.getDescription(),
            healthService.getPrice(),
            healthService.getDurationMinutes(),
            healthService.getActive()
        );
    }
}