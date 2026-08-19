package com.flexchain.controller;

import com.flexchain.entity.Truck;
import com.flexchain.entity.TruckStatus;
import com.flexchain.service.TruckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trucks")
@RequiredArgsConstructor
@CrossOrigin("*")
public class TruckController {

    private final TruckService truckService;

    @GetMapping
    public List<Truck> all() {
        return truckService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Truck> findById(@PathVariable Long id) {
        return ResponseEntity.ok(truckService.findById(id));
    }

    @PostMapping
    public Truck save(@RequestBody Truck truck) {
        return truckService.save(truck);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Truck> update(@PathVariable Long id, @RequestBody Truck truck) {
        return ResponseEntity.ok(truckService.update(id, truck));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Truck> updateStatus(
            @PathVariable Long id,
            @RequestParam TruckStatus status
    ) {
        return ResponseEntity.ok(truckService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/position")
    public ResponseEntity<Truck> updatePosition(
            @PathVariable Long id,
            @RequestParam Double latitude,
            @RequestParam Double longitude
    ) {
        return ResponseEntity.ok(truckService.updatePosition(id, latitude, longitude));
    }
}