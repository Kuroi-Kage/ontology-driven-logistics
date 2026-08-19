package com.flexchain.controller;

import com.flexchain.agent.CapitalAgent;
import com.flexchain.service.CapitalDecisionRequest;
import com.flexchain.service.CapitalDecisionResult;
import com.flexchain.service.CapitalDecisionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/capital")
public class CapitalDecisionController {

    private final CapitalDecisionService capitalDecisionService;

    public CapitalDecisionController(CapitalDecisionService capitalDecisionService) {
        this.capitalDecisionService = capitalDecisionService;
    }

    @PostMapping("/decide")
    public ResponseEntity<CapitalDecisionResult> decide(@Valid @RequestBody CapitalDecisionRequest request) {
        CapitalAgent agent = CapitalAgent.builder()
                .id(request.getId())
                .name(request.getName())
                .initialCapital(request.getInitialCapital())
                .currentCapital(request.getCurrentCapital())
                .truckPurchaseCost(request.getTruckPurchaseCost())
                .truckRepairCost(request.getTruckRepairCost())
                .missionAssignmentCost(request.getMissionAssignmentCost())
                .missionRevenue(request.getMissionRevenue())
                .delayPenalty(request.getDelayPenalty())
                .stockShortagePenalty(request.getStockShortagePenalty())
                .ownedTrucks(request.getOwnedTrucks() == null ? 0 : request.getOwnedTrucks())
                .rentedTrucks(request.getRentedTrucks() == null ? 0 : request.getRentedTrucks())
                .completedMissions(request.getCompletedMissions() == null ? 0 : request.getCompletedMissions())
                .failedMissions(request.getFailedMissions() == null ? 0 : request.getFailedMissions())
                .canPurchaseTruck(Boolean.TRUE.equals(request.getCanPurchaseTruck()))
                .canRepairTruck(Boolean.TRUE.equals(request.getCanRepairTruck()))
                .canAssignMission(Boolean.TRUE.equals(request.getCanAssignMission()))
                .truckBroken(Boolean.TRUE.equals(request.getTruckBroken()))
                .transferAvailable(Boolean.TRUE.equals(request.getTransferAvailable()))
                .repairSuggested(Boolean.TRUE.equals(request.getRepairSuggested()))
                .build();

        return ResponseEntity.ok(capitalDecisionService.decide(agent));
    }
}