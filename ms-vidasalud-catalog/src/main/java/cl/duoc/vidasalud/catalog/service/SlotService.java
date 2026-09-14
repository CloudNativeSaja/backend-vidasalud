package cl.duoc.vidasalud.catalog.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.vidasalud.catalog.dto.SlotRequest;
import cl.duoc.vidasalud.catalog.dto.SlotResponse;
import cl.duoc.vidasalud.catalog.entity.ClinicalBox;
import cl.duoc.vidasalud.catalog.entity.HealthService;
import cl.duoc.vidasalud.catalog.entity.Slot;
import cl.duoc.vidasalud.catalog.exception.BusinessRuleException;
import cl.duoc.vidasalud.catalog.exception.ResourceNotFoundException;
import cl.duoc.vidasalud.catalog.repository.ClinicalBoxRepository;
import cl.duoc.vidasalud.catalog.repository.HealthServiceRepository;
import cl.duoc.vidasalud.catalog.repository.SlotRepository;

@Service
public class SlotService {

    private final SlotRepository slotRepository;
    private final ClinicalBoxRepository boxRepository;
    private final HealthServiceRepository serviceRepository;

    public SlotService(
            SlotRepository slotRepository,
            ClinicalBoxRepository boxRepository,
            HealthServiceRepository serviceRepository) {

        this.slotRepository = slotRepository;
        this.boxRepository = boxRepository;
        this.serviceRepository = serviceRepository;
    }

    public SlotResponse create(SlotRequest request) {

        ClinicalBox box = getBox(request.boxId());

        HealthService healthService =
            getHealthService(request.serviceId());

        if (!box.getActive()) {
            throw new BusinessRuleException(
                "No se pueden crear cupos para un box inactivo"
            );
        }

        if (!healthService.getActive()) {
            throw new BusinessRuleException(
                "No se pueden crear cupos para una prestación inactiva"
            );
        }

        boolean duplicate =
            slotRepository.existsByClinicalBoxIdAndStartTime(
                request.boxId(),
                request.startTime()
            );

        if (duplicate) {
            throw new BusinessRuleException(
                "Ya existe un cupo para ese box en la fecha y hora indicada"
            );
        }

        Slot slot = new Slot();

        slot.setClinicalBox(box);
        slot.setHealthService(healthService);
        slot.setStartTime(request.startTime());

        slot.setAvailable(
            request.available() != null
                ? request.available()
                : true
        );

        return toResponse(
            slotRepository.save(slot)
        );
    }

    public List<SlotResponse> findAll(
            Boolean available,
            Long boxId,
            Long serviceId,
            LocalDateTime from,
            LocalDateTime to) {

        List<Slot> slots;

        if (available != null) {

            slots =
                slotRepository.findByAvailable(available);

        } else if (boxId != null) {

            slots =
                slotRepository.findByClinicalBoxId(boxId);

        } else if (serviceId != null) {

            slots =
                slotRepository.findByHealthServiceId(serviceId);

        } else if (from != null && to != null) {

            slots =
                slotRepository.findByStartTimeBetween(
                    from,
                    to
                );

        } else {

            slots = slotRepository.findAll();
        }

        return slots.stream()
            .map(this::toResponse)
            .toList();
    }

    public SlotResponse findById(Long id) {

        return toResponse(
            getSlot(id)
        );
    }

    public SlotResponse update(
            Long id,
            SlotRequest request) {

        Slot slot = getSlot(id);

        ClinicalBox box =
            getBox(request.boxId());

        HealthService healthService =
            getHealthService(request.serviceId());

        slot.setClinicalBox(box);
        slot.setHealthService(healthService);
        slot.setStartTime(request.startTime());

        if (request.available() != null) {
            slot.setAvailable(request.available());
        }

        return toResponse(
            slotRepository.save(slot)
        );
    }

    public SlotResponse reserve(Long id) {

        Slot slot = getSlot(id);

        if (!slot.getAvailable()) {
            throw new BusinessRuleException(
                "El cupo ya se encuentra ocupado"
            );
        }

        slot.setAvailable(false);

        return toResponse(
            slotRepository.save(slot)
        );
    }

    public SlotResponse release(Long id) {

        Slot slot = getSlot(id);

        if (slot.getAvailable()) {
            throw new BusinessRuleException(
                "El cupo ya se encuentra disponible"
            );
        }

        slot.setAvailable(true);

        return toResponse(
            slotRepository.save(slot)
        );
    }

    public void delete(Long id) {

        Slot slot = getSlot(id);

        slotRepository.delete(slot);
    }

    private Slot getSlot(Long id) {

        return slotRepository.findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "No existe un cupo con ID " + id
                )
            );
    }

    private ClinicalBox getBox(Long id) {

        return boxRepository.findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "No existe un box clínico con ID " + id
                )
            );
    }

    private HealthService getHealthService(Long id) {

        return serviceRepository.findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "No existe una prestación con ID " + id
                )
            );
    }

    private SlotResponse toResponse(Slot slot) {

        return new SlotResponse(
            slot.getId(),
            slot.getClinicalBox().getId(),
            slot.getClinicalBox().getName(),
            slot.getHealthService().getId(),
            slot.getHealthService().getName(),
            slot.getStartTime(),
            slot.getAvailable()
        );
    }
}