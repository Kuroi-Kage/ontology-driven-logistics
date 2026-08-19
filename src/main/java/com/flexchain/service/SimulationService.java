package com.flexchain.service;

import com.flexchain.agent.CoordinatorAgent;
import com.flexchain.agent.NegotiationOutcome;
import com.flexchain.agent.OrderAgent;
import com.flexchain.agent.protocol.ACLMessage;
import com.flexchain.entity.Incident;
import com.flexchain.entity.IncidentType;
import com.flexchain.entity.Truck;
import com.flexchain.entity.TruckStatus;
import com.flexchain.repository.OrderRepository;
import com.flexchain.simulation.SimulationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final TruckService truckService;
    private final OrderRepository orderRepository;
    private final IncidentService incidentService;
    private final CoordinatorAgent coordinatorAgent;
    private final OrderAgent orderAgent;

    /**
     * Simule une panne de camion et déclenche une négociation multi-agents
     * (Contract Net Protocol) entre le TruckAgent, le CapitalAgent et le
     * CoordinatorAgent afin de décider - de façon autonome et tracée - du
     * camion de remplacement à affecter, avant réaffectation effective des
     * commandes par l'OrderAgent.
     */
    @Transactional
    public SimulationResult simulateTruckBreakdown(Long truckId) {
        Truck failedTruck = truckService.findById(truckId);
        failedTruck.setStatus(TruckStatus.BROKEN);
        truckService.save(failedTruck);

        Incident incident = Incident.builder()
                .type(IncidentType.TRUCK_BREAKDOWN)
                .description("Truck " + failedTruck.getCode() + " broke down.")
                .truck(failedTruck)
                .build();

        incidentService.save(incident);

        NegotiationOutcome outcome = coordinatorAgent.negotiateReplacement(failedTruck);
        List<String> negotiationLog = outcome.transcript().stream()
                .map(ACLMessage::toString)
                .toList();

        if (!outcome.accepted() || outcome.selectedTruck() == null) {
            incident.setResolved(false);
            incident.setResultMessage("Négociation échouée : aucune offre acceptée (camion ou budget indisponible).");
            incident.setNegotiationLog(String.join("\n", negotiationLog));
            incidentService.save(incident);

            return SimulationResult.builder()
                    .incident("TRUCK_BREAKDOWN")
                    .failedTruck(failedTruck.getCode())
                    .replacementTruck("NONE")
                    .reassignedOrders(0)
                    .message("Négociation échouée : aucune offre acceptée (camion ou budget indisponible).")
                    .negotiationLog(negotiationLog)
                    .build();
        }

        Truck replacement = outcome.selectedTruck();
        replacement.setStatus(TruckStatus.BUSY);
        truckService.save(replacement);

        int orders = orderRepository.findByTruck(failedTruck).size();
        orderAgent.reassignOrders(failedTruck, replacement);

        String message = "Commandes réaffectées après négociation SMA (Contract Net Protocol).";

        incident.setResolved(true);
        incident.setReplacementTruck(replacement);
        incident.setReassignedOrders(orders);
        incident.setNegotiatedCost(outcome.negotiatedCost());
        incident.setResultMessage(message);
        incident.setNegotiationLog(String.join("\n", negotiationLog));
        incidentService.save(incident);

        return SimulationResult.builder()
                .incident("TRUCK_BREAKDOWN")
                .failedTruck(failedTruck.getCode())
                .replacementTruck(replacement.getCode())
                .reassignedOrders(orders)
                .negotiatedCost(outcome.negotiatedCost())
                .message(message)
                .negotiationLog(negotiationLog)
                .build();
    }
}