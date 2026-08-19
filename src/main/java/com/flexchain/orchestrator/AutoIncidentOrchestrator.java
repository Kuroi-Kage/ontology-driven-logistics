package com.flexchain.orchestrator;

import com.flexchain.entity.Truck;
import com.flexchain.entity.TruckStatus;
import com.flexchain.service.SimulationService;
import com.flexchain.service.TruckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Orchestrateur autonome du terrain SMA : declenche lui-meme, a intervalles
 * reguliers, des pannes de camion (avec une certaine probabilite) et laisse
 * le CoordinatorAgent negocier un remplacement (Contract Net Protocol), sans
 * aucune action manuelle. Le bouton "Simuler panne" reste disponible pour
 * forcer une panne a la demande ; l'orchestrateur couvre le cas general.
 *
 * Chaque execution reutilise exactement le meme chemin de code que le
 * bouton manuel (SimulationService.simulateTruckBreakdown), donc le
 * resultat (Incident + negotiationLog) est persiste de la meme facon et
 * peut etre rejoue par le frontend via /incidents/events/after/{id}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoIncidentOrchestrator {

    private final OrchestratorStateService state;
    private final TruckService truckService;
    private final SimulationService simulationService;

    @Scheduled(fixedDelayString = "${flexchain.orchestrator.interval-ms:12000}")
    public void tick() {
        if (!state.isEnabled()) {
            return;
        }

        recoverFleet();

        if (ThreadLocalRandom.current().nextDouble() > state.getProbability()) {
            return;
        }

        List<Truck> candidates = truckService.findAll().stream()
                .filter(truck -> truck.getStatus() != TruckStatus.BROKEN)
                .filter(truck -> truck.getLatitude() != null && truck.getLongitude() != null)
                .toList();

        if (candidates.isEmpty()) {
            state.recordRun("Aucun camion disponible à mettre en panne (tous BROKEN ou sans position).");
            return;
        }

        Truck target = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

        try {
            var result = simulationService.simulateTruckBreakdown(target.getId());
            state.recordRun("Panne auto de " + target.getCode() + " → " + result.getMessage());
            log.info("[Orchestrateur] Panne automatique déclenchée sur {} : {}", target.getCode(), result.getMessage());
        } catch (Exception e) {
            state.recordRun("Échec de la panne automatique sur " + target.getCode() + " : " + e.getMessage());
            log.warn("[Orchestrateur] Échec de la panne automatique sur {}", target.getCode(), e);
        }
    }

    /**
     * Sans ce mecanisme, un camion BROKEN reste BROKEN pour toujours (jamais
     * "repare") et un camion BUSY reste BUSY pour toujours (jamais remis en
     * service apres sa mission) : la flotte entiere finit par s'epuiser en
     * quelques minutes et plus rien ne peut bouger sur le terrain. On simule
     * donc, a chaque tick, une chance qu'un camion BROKEN soit repare ou
     * qu'un camion BUSY termine sa mission et redevienne AVAILABLE.
     */
    private void recoverFleet() {

        List<Truck> recoverable = truckService.findAll().stream()
                .filter(truck -> truck.getStatus() == TruckStatus.BROKEN
                        || truck.getStatus() == TruckStatus.BUSY)
                .toList();

        for (Truck truck : recoverable) {

            if (ThreadLocalRandom.current().nextDouble() > 0.4) {
                continue;
            }

            TruckStatus previous = truck.getStatus();
            truck.setStatus(TruckStatus.AVAILABLE);
            truckService.save(truck);

            log.info(
                    "[Orchestrateur] {} remis en service (était {}).",
                    truck.getCode(), previous
            );
        }
    }
}