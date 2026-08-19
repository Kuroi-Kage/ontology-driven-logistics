package com.flexchain.routing;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Calcule un itineraire complet entre deux points geographiques
 * arbitraires (position reelle d'un camion, d'un entrepot, ...) en
 * s'appuyant sur le reseau routier interne :
 *
 * point de depart --[a vol d'oiseau]--> noeud le plus proche
 *                  --[Dijkstra sur le reseau routier]--> noeud le plus proche de l'arrivee
 *                  --[a vol d'oiseau]--> point d'arrivee
 *
 * Aucune dependance externe (pas d'appel a une API de routing en ligne) :
 * tout le calcul est fait en memoire, avec un graphe statique construit au
 * demarrage par {@link RoadNetwork}.
 */
@Service
public class RouteService {

    private final RoadNetwork network;
    private final DijkstraPathfinder pathfinder;

    public RouteService(RoadNetwork network, DijkstraPathfinder pathfinder) {
        this.network = network;
        this.pathfinder = pathfinder;
    }

    public RouteResult computeRoute(double fromLat, double fromLon, double toLat, double toLon) {

        RoadNode entryNode = network.nearestNode(fromLat, fromLon);
        RoadNode exitNode = network.nearestNode(toLat, toLon);

        List<RoadNode> nodePath = pathfinder.shortestPath(entryNode.getId(), exitNode.getId());

        if (nodePath.isEmpty()) {
            throw new IllegalStateException(
                    "Aucun chemin trouve entre " + entryNode.getName() + " et " + exitNode.getName() +
                            " (reseau routier non connexe).");
        }

        List<WaypointDto> waypoints = new ArrayList<>();
        List<String> roadsUsed = new ArrayList<>();
        double totalDistanceKm = 0.0;

        // 1. Point de depart exact (position reelle du camion/entrepot).
        waypoints.add(WaypointDto.builder()
                .latitude(fromLat)
                .longitude(fromLon)
                .label("Depart")
                .build());

        totalDistanceKm += GeoUtils.distanceKm(fromLat, fromLon, entryNode.getLatitude(), entryNode.getLongitude());

        // 2. Noeuds du reseau routier traverses (Dijkstra).
        for (int i = 0; i < nodePath.size(); i++) {

            RoadNode node = nodePath.get(i);

            waypoints.add(WaypointDto.builder()
                    .latitude(node.getLatitude())
                    .longitude(node.getLongitude())
                    .label(node.getName())
                    .build());

            if (i > 0) {
                RoadNode previous = nodePath.get(i - 1);
                totalDistanceKm += GeoUtils.distanceKm(
                        previous.getLatitude(), previous.getLongitude(),
                        node.getLatitude(), node.getLongitude());
                roadsUsed.add(network.roadNameBetween(previous.getId(), node.getId()));
            }
        }

        // 3. Point d'arrivee exact.
        waypoints.add(WaypointDto.builder()
                .latitude(toLat)
                .longitude(toLon)
                .label("Arrivee")
                .build());

        totalDistanceKm += GeoUtils.distanceKm(exitNode.getLatitude(), exitNode.getLongitude(), toLat, toLon);

        return RouteResult.builder()
                .waypoints(waypoints)
                .distanceKm(Math.round(totalDistanceKm * 10.0) / 10.0)
                .roadsUsed(roadsUsed)
                .build();
    }
}
