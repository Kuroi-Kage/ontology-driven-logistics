package com.flexchain.service;

import com.flexchain.agent.CapitalAgent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CapitalDecisionService {

    public CapitalDecisionResult decide(CapitalAgent agent) {
        if (agent == null) {
            throw new IllegalArgumentException("Agent cannot be null");
        }

        BigDecimal capitalBeforeAction = safe(agent.getCurrentCapital());
        BigDecimal purchaseCost = safe(agent.getTruckPurchaseCost());
        BigDecimal repairCost = safe(agent.getTruckRepairCost());
        BigDecimal missionCost = safe(agent.getMissionAssignmentCost());
        BigDecimal revenue = safe(agent.getMissionRevenue());

        boolean truckBroken = agent.isTruckBroken();
        boolean canPurchaseTruck = agent.isCanPurchaseTruck();
        boolean canRepairTruck = agent.isCanRepairTruck();
        boolean canAssignMission = agent.isCanAssignMission();
        boolean transferAvailable = agent.isTransferAvailable();

        int ownedTrucks = safeInt(agent.getOwnedTrucks());
        int completedMissions = safeInt(agent.getCompletedMissions());

        if (truckBroken) {
            return decideBrokenTruck(agent, capitalBeforeAction, repairCost, transferAvailable);
        }

        if (canPurchaseTruck && capitalBeforeAction.compareTo(purchaseCost) >= 0) {
            BigDecimal remaining = capitalBeforeAction.subtract(purchaseCost);
            boolean profitable = revenue.subtract(purchaseCost).compareTo(BigDecimal.ZERO) > 0;

            agent.setCurrentCapital(remaining);
            agent.setOwnedTrucks(ownedTrucks + 1);

            return new CapitalDecisionResult(
                    "BUY_TRUCK",
                    "Le capital permet l'achat d'un truck.",
                    capitalBeforeAction,
                    purchaseCost,
                    remaining,
                    profitable,
                    "/orders/create");
        }

        if (canRepairTruck && capitalBeforeAction.compareTo(repairCost) >= 0) {
            BigDecimal remaining = capitalBeforeAction.subtract(repairCost);
            boolean profitable = revenue.subtract(repairCost).compareTo(BigDecimal.ZERO) > 0;

            agent.setCurrentCapital(remaining);

            return new CapitalDecisionResult(
                    "REPAIR_TRUCK",
                    "Le capital permet le dépannage du truck.",
                    capitalBeforeAction,
                    repairCost,
                    remaining,
                    profitable,
                    "/trucks");
        }

        if (canAssignMission && capitalBeforeAction.compareTo(missionCost) >= 0) {
            BigDecimal remaining = capitalBeforeAction.subtract(missionCost);
            boolean profitable = revenue.subtract(missionCost).compareTo(BigDecimal.ZERO) > 0;

            agent.setCurrentCapital(remaining);
            agent.setCompletedMissions(completedMissions + 1);

            return new CapitalDecisionResult(
                    "ASSIGN_MISSION",
                    "Le capital permet l'affectation de mission.",
                    capitalBeforeAction,
                    missionCost,
                    remaining,
                    profitable,
                    "/orders/create");
        }

        return new CapitalDecisionResult(
                "WAIT",
                "Capital insuffisant, il faut attendre ou générer plus de revenus.",
                capitalBeforeAction,
                BigDecimal.ZERO,
                capitalBeforeAction,
                false,
                "/capitals");
    }

    private CapitalDecisionResult decideBrokenTruck(
            CapitalAgent agent,
            BigDecimal capitalBeforeAction,
            BigDecimal repairCost,
            boolean transferAvailable) {
        if (transferAvailable) {
            return new CapitalDecisionResult(
                    "TRANSFER_TRUCK",
                    "Le transfert vers un autre truck est la solution la moins coûteuse.",
                    capitalBeforeAction,
                    BigDecimal.ZERO,
                    capitalBeforeAction,
                    true,
                    "/trucks");
        }

        if (capitalBeforeAction.compareTo(repairCost) >= 0) {
            BigDecimal remaining = capitalBeforeAction.subtract(repairCost);

            agent.setCurrentCapital(remaining);

            return new CapitalDecisionResult(
                    "REPAIR_TRUCK",
                    "Le truck est en panne. Le dépannage est recommandé.",
                    capitalBeforeAction,
                    repairCost,
                    remaining,
                    true,
                    "/trucks");
        }

        return new CapitalDecisionResult(
                "WAIT",
                "Truck en panne et capital insuffisant.",
                capitalBeforeAction,
                BigDecimal.ZERO,
                capitalBeforeAction,
                false,
                "/capitals");
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}