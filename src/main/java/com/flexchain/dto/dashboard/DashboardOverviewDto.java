package com.flexchain.dto.dashboard;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDto {
    private long totalTrucks;
    private long availableTrucks;
    private long busyTrucks;
    private long brokenTrucks;

    private long totalOrders;
    private long pendingOrders;
    private long assignedOrders;
    private long deliveredOrders;

    private long totalWarehouses;
    private long totalIncidents;

    private boolean agentsRunning;
    private List<AgentStatusDto> agents;
    private List<MissionResponseDto> missionResponses;
}