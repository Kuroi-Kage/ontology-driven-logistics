package com.flexchain.routing;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Noeud fixe du reseau routier interne (ville ou carrefour d'une route
 * nationale malgache). Le reseau est un graphe statique, construit en
 * memoire par {@link RoadNetwork} : il ne s'agit pas d'une entite JPA
 * puisqu'il ne depend pas des donnees metier (camions, entrepots) qui,
 * elles, peuvent avoir des coordonnees arbitraires en dehors du graphe.
 */
@Getter
@AllArgsConstructor
public class RoadNode {

    private final String id;
    private final String name;
    private final double latitude;
    private final double longitude;
}
