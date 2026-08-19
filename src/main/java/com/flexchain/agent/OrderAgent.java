package com.flexchain.agent;

import com.flexchain.entity.Order;
import com.flexchain.entity.Truck;
import com.flexchain.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderAgent {

    public static final String NAME = "OrderAgent";

    private final OrderService orderService;

    public void reassignOrders(Truck failedTruck, Truck replacementTruck) {

        List<Order> orders = orderService.truckOrders(failedTruck);

        for (Order order : orders) {

            orderService.assignTruck(order, replacementTruck);

        }

    }

}