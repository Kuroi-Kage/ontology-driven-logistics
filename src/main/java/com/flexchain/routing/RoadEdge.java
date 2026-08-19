package com.flexchain.routing;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Arete bidirectionnelle du reseau routier interne, reliant deux
 * {@link RoadNode} (typiquement le trace approximatif d'une route
 * nationale malgache, ex. TANA-ANTSIRABE = RN7).
 */
@Getter
@AllArgsConstructor
public class RoadEdge {

    private final String fromNodeId;
    private final String toNodeId;

    /**
     * Nom de la route reelle representee (ex. "RN7"), utilise uniquement
     * pour l'affichage / la tracabilite, pas pour le calcul.
     */
    private final String roadName;
}
