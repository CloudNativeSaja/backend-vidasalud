package cl.duoc.vidasalud.catalog.service;

import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.vidasalud.catalog.dto.ClinicalBoxRequest;
import cl.duoc.vidasalud.catalog.dto.ClinicalBoxResponse;
import cl.duoc.vidasalud.catalog.entity.ClinicalBox;
import cl.duoc.vidasalud.catalog.exception.ResourceNotFoundException;
import cl.duoc.vidasalud.catalog.repository.ClinicalBoxRepository;

@Service
public class ClinicalBoxService {

    private final ClinicalBoxRepository repository;

    public ClinicalBoxService(
            ClinicalBoxRepository repository) {
        this.repository = repository;
    }

    public ClinicalBoxResponse create(
            ClinicalBoxRequest request) {

        ClinicalBox box = new ClinicalBox();

        box.setName(request.name());
        box.setCenterName(request.centerName());

        box.setActive(
            request.active() != null
                ? request.active()
                : true
        );

        return toResponse(
            repository.save(box)
        );
    }

    public List<ClinicalBoxResponse> findAll(
            Boolean active) {

        List<ClinicalBox> boxes =
            active == null
                ? repository.findAll()
                : repository.findByActive(active);

        return boxes.stream()
            .map(this::toResponse)
            .toList();
    }

    public ClinicalBoxResponse findById(Long id) {

        return toResponse(
            getClinicalBox(id)
        );
    }

    public ClinicalBoxResponse update(
            Long id,
            ClinicalBoxRequest request) {

        ClinicalBox box =
            getClinicalBox(id);

        box.setName(request.name());
        box.setCenterName(request.centerName());

        if (request.active() != null) {
            box.setActive(request.active());
        }

        return toResponse(
            repository.save(box)
        );
    }

    public void delete(Long id) {

        ClinicalBox box =
            getClinicalBox(id);

        repository.delete(box);
    }

    private ClinicalBox getClinicalBox(Long id) {

        return repository.findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "No existe un box clínico con ID "
                    + id
                )
            );
    }

    private ClinicalBoxResponse toResponse(
            ClinicalBox box) {

        return new ClinicalBoxResponse(
            box.getId(),
            box.getName(),
            box.getCenterName(),
            box.getActive()
        );
    }
}