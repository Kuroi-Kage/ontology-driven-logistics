package com.flexchain.service;

import com.flexchain.entity.Warehouse;
import com.flexchain.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository repository;

    public List<Warehouse> findAll() {
        return repository.findAll();
    }

    public Warehouse save(Warehouse warehouse) {
        return repository.save(warehouse);
    }

    public Warehouse findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse introuvable avec l'id " + id));
    }
}