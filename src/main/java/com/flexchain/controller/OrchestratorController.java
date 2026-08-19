package com.flexchain.controller;

import com.flexchain.orchestrator.OrchestratorStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orchestrator")
@RequiredArgsConstructor
@CrossOrigin("*")
public class OrchestratorController {

    private final OrchestratorStateService state;

    @GetMapping("/status")
    public OrchestratorStateService.Status status() {
        return state.status();
    }

    @PostMapping("/toggle")
    public OrchestratorStateService.Status toggle() {
        state.toggle();
        return state.status();
    }

    @PostMapping("/probability")
    public OrchestratorStateService.Status setProbability(@RequestParam double value) {
        state.setProbability(value);
        return state.status();
    }
}
