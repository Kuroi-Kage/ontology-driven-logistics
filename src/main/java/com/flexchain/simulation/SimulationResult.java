package com.flexchain.simulation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResult {

    private String incident;

    private String failedTruck;

    private String replacementTruck;

    private int reassignedOrders;

    private String message;

    /**
     * Coût négocié et validé par le CapitalAgent pour le remplacement retenu.
     */
    private BigDecimal negotiatedCost;

    /**
     * Transcription complète des messages ACL échangés entre agents
     * (CoordinatorAgent, TruckAgent, CapitalAgent) durant la négociation (Contract Net).
     */
    private List<String> negotiationLog;

}