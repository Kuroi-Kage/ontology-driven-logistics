package com.flexchain.orchestrator;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Etat partage de l'orchestrateur autonome de pannes : lu par
 * {@link AutoIncidentOrchestrator} (tache planifiee) et pilote par
 * {@link com.flexchain.controller.OrchestratorController} (endpoints
 * activer/desactiver depuis l'UI). Volontairement en memoire (pas de
 * persistance) : c'est un interrupteur de demo, pas une donnee metier.
 */
@Component
public class OrchestratorStateService {

    private final AtomicBoolean enabled;
    private final AtomicReference<Double> probability;
    private final AtomicReference<LocalDateTime> lastRunAt = new AtomicReference<>();
    private final AtomicReference<String> lastAction = new AtomicReference<>("Aucune panne automatique déclenchée pour l'instant.");
    private final long intervalMs;

    public OrchestratorStateService(
            @Value("${flexchain.orchestrator.enabled-by-default:true}") boolean enabledByDefault,
            @Value("${flexchain.orchestrator.probability:0.5}") double defaultProbability,
            @Value("${flexchain.orchestrator.interval-ms:12000}") long intervalMs
    ) {
        this.enabled = new AtomicBoolean(enabledByDefault);
        this.probability = new AtomicReference<>(defaultProbability);
        this.intervalMs = intervalMs;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public boolean toggle() {
    boolean previous;
    boolean next;
    do {
        previous = enabled.get();
        next = !previous;
    } while (!enabled.compareAndSet(previous, next));
    return next;
}

    public void setEnabled(boolean value) {
        enabled.set(value);
    }

    public double getProbability() {
        return probability.get();
    }

    public void setProbability(double value) {
        probability.set(Math.max(0, Math.min(1, value)));
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public void recordRun(String action) {
        lastRunAt.set(LocalDateTime.now());
        lastAction.set(action);
    }

    public Status status() {
        return Status.builder()
                .enabled(enabled.get())
                .probability(probability.get())
                .intervalMs(intervalMs)
                .lastRunAt(lastRunAt.get())
                .lastAction(lastAction.get())
                .build();
    }

    @Data
    @Builder
    public static class Status {
        private boolean enabled;
        private double probability;
        private long intervalMs;
        private LocalDateTime lastRunAt;
        private String lastAction;
    }
}
