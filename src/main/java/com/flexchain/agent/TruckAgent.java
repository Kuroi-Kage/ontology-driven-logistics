package com.flexchain.agent;

import com.flexchain.entity.Truck;
import com.flexchain.service.TruckService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;


@Component
@RequiredArgsConstructor
public class TruckAgent {

    public static final String NAME = "TruckAgent";

    private static final BigDecimal DISPATCH_COST = BigDecimal.valueOf(200);
    private static final BigDecimal CAPACITY_SURCHARGE_PER_UNIT = BigDecimal.valueOf(10);

    private final TruckService truckService;

    /**
     * Conservé pour compatibilité : sélection directe du meilleur candidat, sans négociation.
     */
    public Truck findReplacementTruck(Truck failedTruck) {
        return truckService.availableTrucks()
                .stream()
                .filter(truck -> truck.getCapacity() >= failedTruck.getCapacity())
                .sorted(Comparator.comparingInt(truck -> truck.getCapacity().intValue()))
                .findFirst()
                .orElse(null);
    }

    public List<TruckProposal> proposeCandidates(Truck failedTruck) {
        return proposeCandidates(failedTruck, false);
    }

    
    public List<TruckProposal> proposeCandidates(Truck failedTruck, boolean requiresRefrigerated) {
        return truckService.availableTrucks()
                .stream()
                .filter(truck -> truck.getCapacity() >= failedTruck.getCapacity())
                .filter(truck -> !requiresRefrigerated || Boolean.TRUE.equals(truck.getRefrigerated()))
                .sorted(Comparator.comparingDouble(Truck::getCapacity))
                .map(truck -> new TruckProposal(truck, estimateCost(failedTruck, truck)))
                .toList();
    }

    private BigDecimal estimateCost(Truck failedTruck, Truck candidate) {
        double capacityGap = candidate.getCapacity() - failedTruck.getCapacity();
        return DISPATCH_COST.add(CAPACITY_SURCHARGE_PER_UNIT.multiply(BigDecimal.valueOf(capacityGap)));
    }
}