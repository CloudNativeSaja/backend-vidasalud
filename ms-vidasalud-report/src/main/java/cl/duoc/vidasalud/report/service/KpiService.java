package cl.duoc.vidasalud.report.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import cl.duoc.vidasalud.report.dto.KpiResponse;
import cl.duoc.vidasalud.report.dto.ReportRange;
import cl.duoc.vidasalud.report.dto.TopServiceResponse;
import cl.duoc.vidasalud.report.event.AppointmentEvent;
import cl.duoc.vidasalud.report.event.AppointmentStatus;

/**
 * Logica de negocio de la reporteria: no sabe nada de Kafka.
 *
 * Mantiene en memoria el ultimo estado de cada cita a partir de los eventos.
 * El estado no se guarda en base de datos: al reiniciar, el consumer group
 * relee el topico desde el inicio (auto.offset.reset=earliest) y los KPIs se
 * reconstruyen solos. Es la ventaja de que Kafka conserve los eventos.
 */
@Service
public class KpiService {

    /** Eventos ya aplicados, para descartar duplicados (entrega at-least-once). */
    private static final int MAX_PROCESSED_IDS = 10_000;
    private static final int TOP_SERVICES = 5;

    private final Clock clock;
    private final ServiceNameResolver serviceNames;

    private final Map<Long, AppointmentSummary> appointments = new HashMap<>();
    private final Set<String> processedEventIds = new LinkedHashSet<>();
    private Instant lastEventAt;

    public KpiService(Clock clock, ServiceNameResolver serviceNames) {
        this.clock = clock;
        this.serviceNames = serviceNames;
    }

    /**
     * Aplica un evento al estado de las citas.
     *
     * @return false si el evento ya se habia procesado (duplicado)
     */
    public synchronized boolean apply(AppointmentEvent event) {
        if (!processedEventIds.add(event.eventId())) {
            return false;
        }
        if (processedEventIds.size() > MAX_PROCESSED_IDS) {
            processedEventIds.remove(processedEventIds.iterator().next());
        }

        AppointmentSummary summary = appointments.computeIfAbsent(event.appointmentId(),
                id -> new AppointmentSummary(event.serviceId(), event.occurredAt()));

        if (event.type() == AppointmentEvent.Type.APPOINTMENT_CREATED) {
            summary.createdAt = event.occurredAt();
        }
        summary.serviceId = event.serviceId();
        summary.status = event.newStatus();
        if (event.newStatus() == AppointmentStatus.EN_ESPERA) {
            summary.waitingSince = event.occurredAt();
        } else if (event.newStatus() == AppointmentStatus.EN_ATENCION) {
            summary.attendedAt = event.occurredAt();
        }

        if (lastEventAt == null || event.occurredAt().isAfter(lastEventAt)) {
            lastEventAt = event.occurredAt();
        }
        return true;
    }

    public synchronized KpiResponse kpis(ReportRange range) {
        List<AppointmentSummary> inRange = inRange(range);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        Map<AppointmentStatus, Long> counts = new EnumMap<>(AppointmentStatus.class);
        for (AppointmentStatus status : AppointmentStatus.values()) {
            counts.put(status, 0L);
        }
        inRange.forEach(a -> counts.merge(a.status, 1L, Long::sum));
        counts.forEach((status, total) -> byStatus.put(status.name(), total));

        long total = inRange.size();
        double cancellationRate = total == 0 ? 0 : (double) counts.get(AppointmentStatus.CANCELADA) / total;

        OptionalDouble averageWait = inRange.stream()
                .filter(a -> a.waitingSince != null && a.attendedAt != null)
                .mapToDouble(a -> Duration.between(a.waitingSince, a.attendedAt).toSeconds() / 60.0)
                .average();

        return new KpiResponse(
                range.value(),
                total,
                byStatus,
                round(cancellationRate, 4),
                round(averageWait.orElse(0), 1),
                lastEventAt);
    }

    public synchronized List<TopServiceResponse> topServices(ReportRange range) {
        Map<Long, Long> byService = inRange(range).stream()
                .collect(Collectors.groupingBy(a -> a.serviceId, Collectors.counting()));

        return byService.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_SERVICES)
                .map(e -> new TopServiceResponse(e.getKey(), serviceNames.nameOf(e.getKey()), e.getValue()))
                .toList();
    }

    private List<AppointmentSummary> inRange(ReportRange range) {
        Instant since = clock.instant().minus(range.duration());
        return appointments.values().stream()
                .filter(a -> !a.createdAt.isBefore(since))
                .toList();
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    /** Ultimo estado conocido de una cita. */
    private static final class AppointmentSummary {
        private Long serviceId;
        private Instant createdAt;
        private AppointmentStatus status = AppointmentStatus.SOLICITADA;
        private Instant waitingSince;
        private Instant attendedAt;

        private AppointmentSummary(Long serviceId, Instant createdAt) {
            this.serviceId = serviceId;
            this.createdAt = createdAt;
        }
    }
}
