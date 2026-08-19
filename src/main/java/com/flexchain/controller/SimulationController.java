package com.flexchain.controller;

import com.flexchain.service.SimulationService;
import com.flexchain.simulation.SimulationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/simulation")
@RequiredArgsConstructor
@CrossOrigin("*")
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping("/breakdown/{truckId}")
    public SimulationResult simulate(@PathVariable Long truckId) {

        return simulationService.simulateTruckBreakdown(truckId);

    }

}