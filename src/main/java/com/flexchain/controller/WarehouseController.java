package com.flexchain.controller;

import com.flexchain.entity.Warehouse;
import com.flexchain.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
@CrossOrigin("*")
public class WarehouseController {

    private final WarehouseService service;

    @GetMapping
    public List<Warehouse> all() {
        return service.findAll();
    }

    @PostMapping
    public Warehouse save(@RequestBody Warehouse warehouse) {
        return service.save(warehouse);
    }

    @GetMapping("/{id}")
    public Warehouse findById(@PathVariable Long id) {
        return service.findById(id);
    }
}