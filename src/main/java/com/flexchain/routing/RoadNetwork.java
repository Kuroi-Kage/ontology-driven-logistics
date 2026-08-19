package com.flexchain.routing;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Reseau routier interne, construit une fois au demarrage (graphe statique,
 * pas de dependance externe / pas d'appel a une API de routing en ligne).
 *
 * Les noeuds correspondent aux villes malgaches deja utilisees par
 * {@link com.flexchain.config.DataLoader} pour les entrepots de demo, et les
 * aretes representent le trace approximatif des routes nationales (RN) qui
 * les relient reellement sur le terrain.
 *
 * Les camions/entrepots peuvent avoir des coordonnees arbitraires (pas
 * exactement sur un noeud) : {@link #nearestNode(double, double)} rattache
 * tout point au noeud du graphe le plus proche a vol d'oiseau.
 */
@Component
public class RoadNetwork {

    private final Map<String, RoadNode> nodes = new LinkedHashMap<>();
    private final Map<String, List<RoadEdge>> adjacency = new HashMap<>();

    @PostConstruct
    void build() {

        addNode("TANA", "Antananarivo", -18.8792, 47.5079);
        addNode("ANTSIRABE", "Antsirabe", -19.8659, 47.0333);
        addNode("FIANARANTSOA", "Fianarantsoa", -21.4527, 47.0857);
        addNode("TOAMASINA", "Toamasina", -18.1492, 49.4023);
        addNode("MAHAJANGA", "Mahajanga", -15.7167, 46.3167);
        addNode("TOLIARA", "Toliara", -23.3500, 43.6667);
        addNode("TAOLAGNARO", "Taolagnaro", -25.0300, 46.9900);
        addNode("ANTSIRANANA", "Antsiranana", -12.2765, 49.2917);
        addNode("MORONDAVA", "Morondava", -20.2833, 44.2833);

        // Trace approximatif des routes nationales (RN) reliant ces villes.
        addEdge("TANA", "ANTSIRABE", "RN7");
        addEdge("ANTSIRABE", "FIANARANTSOA", "RN7");
        addEdge("FIANARANTSOA", "TOLIARA", "RN7");
        addEdge("TOLIARA", "TAOLAGNARO", "RN10");
        addEdge("TANA", "TOAMASINA", "RN2");
        addEdge("TANA", "MAHAJANGA", "RN4");
        addEdge("MAHAJANGA", "ANTSIRANANA", "RN6");
        addEdge("ANTSIRABE", "MORONDAVA", "RN34");
    }

    private void addNode(String id, String name, double lat, double lon) {
        nodes.put(id, new RoadNode(id, name, lat, lon));
        adjacency.put(id, new ArrayList<>());
    }

    private void addEdge(String fromId, String toId, String roadName) {
        adjacency.get(fromId).add(new RoadEdge(fromId, toId, roadName));
        adjacency.get(toId).add(new RoadEdge(toId, fromId, roadName));
    }

    public Collection<RoadNode> allNodes() {
        return nodes.values();
    }

    public RoadNode node(String id) {
        return nodes.get(id);
    }

    List<RoadEdge> edgesFrom(String nodeId) {
        return adjacency.getOrDefault(nodeId, List.of());
    }

    /**
     * Nom de la route reelle empruntee entre deux noeuds adjacents du
     * graphe (ex. "RN7"), ou "?" si les deux noeuds ne sont pas relies
     * directement par une arete (ne devrait pas arriver pour un chemin
     * issu de {@link DijkstraPathfinder}).
     */
    String roadNameBetween(String fromNodeId, String toNodeId) {
        return edgesFrom(fromNodeId).stream()
                .filter(edge -> edge.getToNodeId().equals(toNodeId))
                .map(RoadEdge::getRoadName)
                .findFirst()
                .orElse("?");
    }

    /**
     * Noeud du reseau le plus proche (a vol d'oiseau) d'un point donne.
     * Sert de point d'entree/sortie du graphe pour un camion ou un entrepot
     * dont la position exacte n'est pas forcement un noeud.
     */
    public RoadNode nearestNode(double latitude, double longitude) {

        return nodes.values().stream()
                .min(Comparator.comparingDouble(node ->
                        GeoUtils.distanceKm(latitude, longitude, node.getLatitude(), node.getLongitude())))
                .orElseThrow(() -> new IllegalStateException("Reseau routier vide."));
    }
}
