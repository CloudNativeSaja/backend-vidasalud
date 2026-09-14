package cl.duoc.vidasalud.catalog.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.vidasalud.catalog.entity.Slot;

public interface SlotRepository
        extends JpaRepository<Slot, Long> {

    List<Slot> findByAvailable(Boolean available);

    List<Slot> findByClinicalBoxId(Long boxId);

    List<Slot> findByHealthServiceId(Long serviceId);

    List<Slot> findByStartTimeBetween(
        LocalDateTime from,
        LocalDateTime to
    );

    boolean existsByClinicalBoxIdAndStartTime(
        Long boxId,
        LocalDateTime startTime
    );
}