package com.flexchain.routing;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Expose le calcul d'itineraire (reseau routier interne + Dijkstra) afin
 * que le terrain SMA puisse afficher un vrai trace de route entre deux
 * points, plutot qu'une ligne droite.
 */
@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
@CrossOrigin("*")
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    public RouteResult route(
            @RequestParam double fromLat,
            @RequestParam double fromLon,
            @RequestParam double toLat,
            @RequestParam double toLon
    ) {
        return routeService.computeRoute(fromLat, fromLon, toLat, toLon);
    }
}
