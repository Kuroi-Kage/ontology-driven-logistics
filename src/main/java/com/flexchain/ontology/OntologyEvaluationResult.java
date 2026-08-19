package com.flexchain.ontology;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OntologyEvaluationResult {

    private boolean fragile;
    private Double currentTemperatureCelsius;
    private boolean requiresTruckChange;
    private boolean requiresRefrigeratedTruck;
    private String explanation;
}
