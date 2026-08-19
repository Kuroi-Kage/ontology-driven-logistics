package com.flexchain.ontology;

import jakarta.annotation.PostConstruct;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.flexchain.ontology.FlexOntologyVocabulary.CURRENT_TEMPERATURE_CELSIUS;
import static com.flexchain.ontology.FlexOntologyVocabulary.IS_FRAGILE;
import static com.flexchain.ontology.FlexOntologyVocabulary.ORDER_INDIVIDUAL_PREFIX;
import static com.flexchain.ontology.FlexOntologyVocabulary.REQUIRES_REFRIGERATED_TRUCK;
import static com.flexchain.ontology.FlexOntologyVocabulary.REQUIRES_TRUCK_CHANGE;

/**
 * Charge l'ontologie FlexChain (TBox, flexchain-ontology.ttl) et les règles
 * d'inférence associées (flexchain-rules.rules), puis exécute un
 * raisonnement par chaînage avant (forward chaining) via Apache Jena pour
 * déterminer, à partir des caractéristiques d'une commande (fragilité,
 * température), si un changement de camion et/ou un camion réfrigéré sont
 * nécessaires.
 *
 * L'ontologie et les règles sont chargées une seule fois au démarrage.
 * Chaque appel à evaluate() crée un petit modèle de faits (ABox) propre à
 * la commande évaluée, unifié dynamiquement avec la TBox via
 * ModelFactory.createUnion, afin de ne jamais mélanger les données entre
 * deux évaluations.
 */
@Service
public class OntologyReasoningService {

    private Model ontologySchema;
    private List<Rule> rules;

    @PostConstruct
    public void init() throws IOException {
        ontologySchema = ModelFactory.createDefaultModel();

        try (InputStream in = new ClassPathResource(
                "ontology/flexchain-ontology.ttl"
        ).getInputStream()) {

            RDFParser.source(in)
                    .lang(Lang.TURTLE)
                    .parse(ontologySchema);
        }

        try (InputStream in = new ClassPathResource(
                "ontology/flexchain-rules.rules"
        ).getInputStream()) {

            String rulesSource = new String(
                    in.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            rules = Rule.parseRules(rulesSource);
        }
    }

    /**
     * Évalue une commande selon ses caractéristiques.
     *
     * @param orderId identifiant de la commande
     * @param fragile indique si le produit est fragile
     * @param currentTemperatureCelsius température actuelle du produit
     * @return résultat du raisonnement ontologique
     */
    public OntologyEvaluationResult evaluate(
            String orderId,
            boolean fragile,
            Double currentTemperatureCelsius
    ) {

       
        Model facts = ModelFactory.createDefaultModel();

        Resource order = facts.createResource(
                ORDER_INDIVIDUAL_PREFIX + orderId
        );

        facts.add(
                order,
                ResourceFactory.createProperty(IS_FRAGILE),
                facts.createTypedLiteral(fragile)
        );

     
        if (currentTemperatureCelsius != null) {

            facts.add(
                    order,
                    ResourceFactory.createProperty(
                            CURRENT_TEMPERATURE_CELSIUS
                    ),
                    facts.createTypedLiteral(
                            currentTemperatureCelsius
                    )
            );
        }

       
        Model combined = ModelFactory.createUnion(
                ontologySchema,
                facts
        );

        Reasoner reasoner = new GenericRuleReasoner(rules);

        InfModel infModel = ModelFactory.createInfModel(
                reasoner,
                combined
        );

        /*
         * ============================================================
         * 4. Vérification des conclusions produites par les règles
         * ============================================================
         */
        boolean requiresTruckChange = infModel.contains(
                order,
                ResourceFactory.createProperty(
                        REQUIRES_TRUCK_CHANGE
                ),
                infModel.createTypedLiteral(true)
        );

        boolean requiresRefrigeratedTruck = infModel.contains(
                order,
                ResourceFactory.createProperty(
                        REQUIRES_REFRIGERATED_TRUCK
                ),
                infModel.createTypedLiteral(true)
        );

        /*
         * ============================================================
         * 5. Construction du résultat
         * ============================================================
         */
        return OntologyEvaluationResult.builder()
                .fragile(fragile)
                .currentTemperatureCelsius(
                        currentTemperatureCelsius
                )
                .requiresTruckChange(
                        requiresTruckChange
                )
                .requiresRefrigeratedTruck(
                        requiresRefrigeratedTruck
                )
                .explanation(
                        explain(
                                fragile,
                                currentTemperatureCelsius,
                                requiresTruckChange,
                                requiresRefrigeratedTruck
                        )
                )
                .build();
    }

    /**
     * Génère une explication lisible du résultat du raisonnement.
     */
    private String explain(
            boolean fragile,
            Double temperature,
            boolean requiresChange,
            boolean requiresRefrigerated
    ) {

        if (!fragile) {
            return "Produit non fragile : aucune règle de transport spéciale ne s'applique.";
        }

        if (temperature == null) {
            return "Produit fragile, mais température inconnue : impossible d'évaluer le risque thermique.";
        }

        if (!requiresChange) {
            return "Produit fragile à "
                    + temperature
                    + " degrés C, sous le seuil de 25 degrés : aucune action requise.";
        }

        return "Produit fragile exposé à "
                + temperature
                + " degrés C (> 25 degrés) : changement de camion requis"
                + (
                    requiresRefrigerated
                        ? ", camion réfrigéré obligatoire (règle chaînée sur la première)."
                        : "."
                );
    }
}