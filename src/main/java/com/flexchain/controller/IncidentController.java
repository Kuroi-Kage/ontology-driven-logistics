package com.flexchain.controller;

import com.flexchain.dto.IncidentEventDto;
import com.flexchain.entity.Incident;
import com.flexchain.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/incidents")
@RequiredArgsConstructor
@CrossOrigin("*")
public class IncidentController {

    private final IncidentService service;

    @GetMapping
    public List<Incident> all() {
        return service.findAll();
    }

    @PostMapping
    public Incident create(@RequestBody Incident incident) {
        return service.save(incident);
    }

    @GetMapping("/events/after/{id}")
    public List<IncidentEventDto> eventsAfter(@PathVariable Long id) {
        return service.findEventsAfter(id);
    }

    @GetMapping("/events/latest-id")
    public Map<String, Long> latestId() {
        return Map.of("latestId", service.latestId());
    }
}