package com.flexchain.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CapitalDecisionRequest {

    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    @NotNull(message = "initialCapital est obligatoire")
    @DecimalMin(value = "0.0", inclusive = true, message = "initialCapital doit être positif ou nul")
    private BigDecimal initialCapital;

    @NotNull(message = "currentCapital est obligatoire")
    @DecimalMin(value = "0.0", inclusive = true, message = "currentCapital doit être positif ou nul")
    private BigDecimal currentCapital;

    @NotNull(message = "truckPurchaseCost est obligatoire")
    @DecimalMin(value = "0.0", inclusive = true, message = "truckPurchaseCost doit être positif ou nul")
    private BigDecimal truckPurchaseCost;

    @NotNull(message = "truckRepairCost est obligatoire")
    @DecimalMin(value = "0.0", inclusive = true, message = "truckRepairCost doit être positif ou nul")
    private BigDecimal truckRepairCost;

    @NotNull(message = "missionAssignmentCost est obligatoire")
    @DecimalMin(value = "0.0", inclusive = true, message = "missionAssignmentCost doit être positif ou nul")
    private BigDecimal missionAssignmentCost;

    @NotNull(message = "missionRevenue est obligatoire")
    @DecimalMin(value = "0.0", inclusive = true, message = "missionRevenue doit être positif ou nul")
    private BigDecimal missionRevenue;

    @DecimalMin(value = "0.0", inclusive = true, message = "delayPenalty doit être positif ou nul")
    private BigDecimal delayPenalty;

    @DecimalMin(value = "0.0", inclusive = true, message = "stockShortagePenalty doit être positif ou nul")
    private BigDecimal stockShortagePenalty;

    @PositiveOrZero(message = "ownedTrucks doit être positif ou nul")
    private Integer ownedTrucks;

    @PositiveOrZero(message = "rentedTrucks doit être positif ou nul")
    private Integer rentedTrucks;

    @PositiveOrZero(message = "completedMissions doit être positif ou nul")
    private Integer completedMissions;

    @PositiveOrZero(message = "failedMissions doit être positif ou nul")
    private Integer failedMissions;

    @NotNull(message = "canPurchaseTruck est obligatoire")
    private Boolean canPurchaseTruck;

    @NotNull(message = "canRepairTruck est obligatoire")
    private Boolean canRepairTruck;

    @NotNull(message = "canAssignMission est obligatoire")
    private Boolean canAssignMission;

    @NotNull(message = "truckBroken est obligatoire")
    private Boolean truckBroken;

    @NotNull(message = "transferAvailable est obligatoire")
    private Boolean transferAvailable;

    @NotNull(message = "repairSuggested est obligatoire")
    private Boolean repairSuggested;
}