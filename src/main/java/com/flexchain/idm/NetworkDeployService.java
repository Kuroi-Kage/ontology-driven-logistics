package com.flexchain.idm;

import com.flexchain.entity.Order;
import com.flexchain.entity.OrderStatus;
import com.flexchain.entity.Truck;
import com.flexchain.entity.TruckStatus;
import com.flexchain.entity.Warehouse;
import com.flexchain.idm.dsl.ast.NetworkModel;
import com.flexchain.idm.dsl.ast.OrderDecl;
import com.flexchain.idm.dsl.ast.TruckDecl;
import com.flexchain.idm.dsl.ast.WarehouseDecl;
import com.flexchain.service.OrderService;
import com.flexchain.service.TruckService;
import com.flexchain.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class NetworkDeployService {

    private final WarehouseService warehouseService;
    private final TruckService truckService;
    private final OrderService orderService;

    @Transactional
    public DeployResult deploy(NetworkModel model) {
        Map<String, Warehouse> warehousesByDslId = new HashMap<>();

        for (WarehouseDecl w : model.warehouses) {
            Warehouse warehouse = Warehouse.builder()
                    .name(w.name)
                    .location(w.location)
                    .latitude(w.latitude)
                    .longitude(w.longitude)
                    .build();
            warehouse = warehouseService.save(warehouse);
            warehousesByDslId.put(w.id, warehouse);
        }

        int trucksCreated = 0;
        for (TruckDecl t : model.trucks) {
            Truck truck = Truck.builder()
                    .code(t.code)
                    .driver(t.driver)
                    .capacity(t.capacity)
                    .status(TruckStatus.valueOf(t.status))
                    .build();
            truckService.save(truck);
            trucksCreated++;
        }

        int ordersCreated = 0;
        for (OrderDecl o : model.orders) {
            Order order = Order.builder()
                    .reference(o.reference)
                    .destination(o.destination)
                    .warehouse(warehousesByDslId.get(o.warehouseRef))
                    .status(OrderStatus.valueOf(o.status))
                    .build();
            orderService.save(order);
            ordersCreated++;
        }

        return new DeployResult(model.name, warehousesByDslId.size(), trucksCreated, ordersCreated);
    }

    public record DeployResult(String networkName, int warehousesCreated, int trucksCreated, int ordersCreated) {
    }
}
