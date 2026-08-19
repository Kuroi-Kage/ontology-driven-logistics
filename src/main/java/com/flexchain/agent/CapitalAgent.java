package com.flexchain.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapitalAgent {

    private Long id;
    private String name;

    @Builder.Default
    private BigDecimal initialCapital = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal currentCapital = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal truckPurchaseCost = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal truckRepairCost = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal missionAssignmentCost = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal missionRevenue = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal delayPenalty = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal stockShortagePenalty = BigDecimal.ZERO;

    @Builder.Default
    private Integer ownedTrucks = 0;

    @Builder.Default
    private Integer rentedTrucks = 0;

    @Builder.Default
    private Integer completedMissions = 0;

    @Builder.Default
    private Integer failedMissions = 0;

    @Builder.Default
    private boolean canPurchaseTruck = false;

    @Builder.Default
    private boolean canRepairTruck = false;

    @Builder.Default
    private boolean canAssignMission = false;

    @Builder.Default
    private boolean truckBroken = false;

    @Builder.Default
    private boolean transferAvailable = false;

    @Builder.Default
    private boolean repairSuggested = false;
}