package com.flexchain.ontology;

/**
 * Vocabulaire de l'ontologie FlexChain (voir resources/ontology/flexchain-ontology.ttl).
 * Centralise les IRI pour eviter les erreurs de frappe entre le service de
 * raisonnement et l'ontologie elle-meme.
 */
public final class FlexOntologyVocabulary {

    public static final String NS = "http://flexchain.com/ontology#";

    public static final String ORDER_INDIVIDUAL_PREFIX = NS + "order/";

    public static final String IS_FRAGILE = NS + "isFragile";
    public static final String CURRENT_TEMPERATURE_CELSIUS = NS + "currentTemperatureCelsius";
    public static final String REQUIRES_TRUCK_CHANGE = NS + "requiresTruckChange";
    public static final String REQUIRES_REFRIGERATED_TRUCK = NS + "requiresRefrigeratedTruck";

    private FlexOntologyVocabulary() {
    }
}
