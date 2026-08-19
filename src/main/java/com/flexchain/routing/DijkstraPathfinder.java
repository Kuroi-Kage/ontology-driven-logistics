package com.flexchain.routing;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Calcule le plus court chemin (en distance) entre deux noeuds du
 * {@link RoadNetwork} avec l'algorithme de Dijkstra. Le poids de chaque
 * arete est la distance orthodromique (km) entre ses deux noeuds.
 */
@Component
public class DijkstraPathfinder {

    private final RoadNetwork network;

    public DijkstraPathfinder(RoadNetwork network) {
        this.network = network;
    }

    /**
     * @return la liste ordonnee des noeuds du chemin le plus court
     * (bornes incluses), ou vide si aucun chemin n'existe entre les deux
     * noeuds (graphe non connexe).
     */
    public List<RoadNode> shortestPath(String startNodeId, String endNodeId) {

        if (startNodeId.equals(endNodeId)) {
            return List.of(network.node(startNodeId));
        }

        Map<String, Double> distance = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        Set<String> visited = new HashSet<>();

        PriorityQueue<String> queue =
                new PriorityQueue<>(Comparator.comparingDouble(id -> distance.getOrDefault(id, Double.MAX_VALUE)));

        distance.put(startNodeId, 0.0);
        queue.add(startNodeId);

        while (!queue.isEmpty()) {

            String current = queue.poll();

            if (!visited.add(current)) {
                continue;
            }

            if (current.equals(endNodeId)) {
                break;
            }

            RoadNode currentNode = network.node(current);

            for (RoadEdge edge : network.edgesFrom(current)) {

                if (visited.contains(edge.getToNodeId())) {
                    continue;
                }

                RoadNode neighbour = network.node(edge.getToNodeId());

                double weight = GeoUtils.distanceKm(
                        currentNode.getLatitude(), currentNode.getLongitude(),
                        neighbour.getLatitude(), neighbour.getLongitude());

                double candidate = distance.get(current) + weight;

                if (candidate < distance.getOrDefault(edge.getToNodeId(), Double.MAX_VALUE)) {
                    distance.put(edge.getToNodeId(), candidate);
                    previous.put(edge.getToNodeId(), current);
                    queue.add(edge.getToNodeId());
                }
            }
        }

        if (!distance.containsKey(endNodeId)) {
            return List.of();
        }

        LinkedList<RoadNode> path = new LinkedList<>();
        String step = endNodeId;

        while (step != null) {
            path.addFirst(network.node(step));
            step = previous.get(step);
        }

        return path;
    }
}
