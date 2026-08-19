package com.flexchain.routing;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResult {

    /**
     * Chemin complet, dans l'ordre de parcours : point de depart exact,
     * puis noeuds du reseau routier traverses, puis point d'arrivee exact.
     */
    private List<WaypointDto> waypoints;

    private double distanceKm;

    /**
     * Noms des routes nationales empruntees, dans l'ordre (pour affichage /
     * tracabilite), ex. ["RN7", "RN7", "RN34"].
     */
    private List<String> roadsUsed;
}
