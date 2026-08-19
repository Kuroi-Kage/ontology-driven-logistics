package com.flexchain.ontology;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OntologyEvaluateRequest {

    /**
     * Optionnel : identifiant metier pour rattacher l'evaluation a une commande
     * existante dans les traces/logs. Un identifiant technique est genere si absent.
     */
    private String orderId;

    @NotNull(message = "fragile est obligatoire")
    private Boolean fragile;

    private Double currentTemperatureCelsius;
}
