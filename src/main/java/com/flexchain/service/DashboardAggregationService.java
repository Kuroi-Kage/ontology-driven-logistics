package com.flexchain.service;

import com.flexchain.dto.dashboard.AgentStatusDto;
import com.flexchain.dto.dashboard.DashboardOverviewDto;
import com.flexchain.dto.dashboard.MissionResponseDto;
import com.flexchain.entity.OrderStatus;
import com.flexchain.entity.TruckStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardAggregationService {

    private final TruckService truckService;
    private final OrderService orderService;
    private final WarehouseService warehouseService;
    private final IncidentService incidentService;

    public DashboardOverviewDto overview() {
        var trucks = truckService.findAll();
        var orders = orderService.findAll();
        var warehouses = warehouseService.findAll();
        var incidents = incidentService.findAll();

        long totalTrucks = trucks.size();
        long availableTrucks = trucks.stream()
                .filter(t -> t.getStatus() == TruckStatus.AVAILABLE)
                .count();
        long busyTrucks = trucks.stream()
                .filter(t -> t.getStatus() == TruckStatus.BUSY)
                .count();
        long brokenTrucks = trucks.stream()
                .filter(t -> t.getStatus() == TruckStatus.BROKEN)
                .count();

        long totalOrders = orders.size();
        long pendingOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .count();
        long assignedOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS)
                .count();
        long deliveredOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .count();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        List<AgentStatusDto> agents = List.of(
                AgentStatusDto.builder()
                        .name("Agent Commande")
                        .status("RUNNING")
                        .progress(85)
                        .build(),
                AgentStatusDto.builder()
                        .name("Agent Camion")
                        .status("RUNNING")
                        .progress(72)
                        .build(),
                AgentStatusDto.builder()
                        .name("Agent Entrepôt")
                        .status("IDLE")
                        .progress(45)
                        .build(),
                AgentStatusDto.builder()
                        .name("Agent Capital")
                        .status("RUNNING")
                        .progress(60)
                        .build()
        );

        List<MissionResponseDto> responses = List.of(
                MissionResponseDto.builder()
                        .agentName("Agent Commande")
                        .message("Commande réaffectée automatiquement.")
                        .timestamp(LocalDateTime.now().format(formatter))
                        .build(),
                MissionResponseDto.builder()
                        .agentName("Agent Camion")
                        .message("Panne détectée, camion de secours proposé.")
                        .timestamp(LocalDateTime.now().format(formatter))
                        .build()
        );

        return DashboardOverviewDto.builder()
                .totalTrucks(totalTrucks)
                .availableTrucks(availableTrucks)
                .busyTrucks(busyTrucks)
                .brokenTrucks(brokenTrucks)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .assignedOrders(assignedOrders)
                .deliveredOrders(deliveredOrders)
                .totalWarehouses(warehouses.size())
                .totalIncidents(incidents.size())
                .agentsRunning(true)
                .agents(agents)
                .missionResponses(responses)
                .build();
    }
}