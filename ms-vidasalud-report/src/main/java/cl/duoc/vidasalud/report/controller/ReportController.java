package cl.duoc.vidasalud.report.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.vidasalud.report.dto.KpiResponse;
import cl.duoc.vidasalud.report.dto.ReportRange;
import cl.duoc.vidasalud.report.dto.TopServiceResponse;
import cl.duoc.vidasalud.report.service.KpiService;

/**
 * Indicadores calculados desde Kafka. Solo es accesible a traves del BFF
 * (/api/bff/report/** con rol ADMIN).
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final KpiService kpiService;

    public ReportController(KpiService kpiService) {
        this.kpiService = kpiService;
    }

    /** GET /api/report/kpis?range=last24h|last7d|last30d */
    @GetMapping("/kpis")
    public KpiResponse kpis(@RequestParam(defaultValue = "last24h") String range) {
        return kpiService.kpis(ReportRange.from(range));
    }

    /** GET /api/report/top-services?range=last24h|last7d|last30d */
    @GetMapping("/top-services")
    public List<TopServiceResponse> topServices(@RequestParam(defaultValue = "last7d") String range) {
        return kpiService.topServices(ReportRange.from(range));
    }
}
