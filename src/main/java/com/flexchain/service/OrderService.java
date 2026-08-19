package com.flexchain.service;

import com.flexchain.entity.Order;
import com.flexchain.entity.Truck;
import com.flexchain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;

    public List<Order> findAll() {
        return repository.findAll();
    }

    public Order save(Order order) {
        return repository.save(order);
    }

    public List<Order> truckOrders(Truck truck) {
        return repository.findByTruck(truck);
    }

    public Order assignTruck(Order order, Truck truck) {

        order.setTruck(truck);

        return repository.save(order);

    }

    public Order findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande introuvable avec l'id " + id));
    }

}