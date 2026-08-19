package com.flexchain.repository;

import com.flexchain.entity.Order;
import com.flexchain.entity.OrderStatus;
import com.flexchain.entity.Truck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByTruck(Truck truck);

    List<Order> findByStatus(OrderStatus status);

}