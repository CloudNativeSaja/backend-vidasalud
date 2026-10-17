package cl.duoc.vidasalud.mqadmin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.vidasalud.mqadmin.dto.ClusterNodeResponse;
import cl.duoc.vidasalud.mqadmin.service.RabbitAdminService;

/**
 * GET /api/mq/cluster/nodes   nodos del cluster RabbitMQ y su estado.
 */
@RestController
@RequestMapping("/api/mq/cluster")
public class ClusterController {

    private final RabbitAdminService adminService;

    public ClusterController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/nodes")
    public List<ClusterNodeResponse> nodes() {
        return adminService.clusterNodes();
    }
}
