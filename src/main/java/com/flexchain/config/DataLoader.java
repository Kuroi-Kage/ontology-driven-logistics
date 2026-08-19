package com.flexchain.config;

import com.flexchain.entity.*;
import com.flexchain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final TruckRepository truckRepository;
    private final WarehouseRepository warehouseRepository;
    private final OrderRepository orderRepository;

    @Override
    public void run(String... args) {

        createWarehouses();
        createTrucks();
        createOrders();
    }

    private void createWarehouses() {

        if (warehouseRepository.count() > 0) {
            return;
        }

        warehouseRepository.save(Warehouse.builder()
                .name("Antananarivo")
                .location("Antananarivo")
                .latitude(-18.8792)
                .longitude(47.5079)
                .build());

        warehouseRepository.save(Warehouse.builder()
                .name("Antsirabe")
                .location("Antsirabe")
                .latitude(-19.8659)
                .longitude(47.0333)
                .build());

        warehouseRepository.save(Warehouse.builder()
                .name("Fianarantsoa")
                .location("Fianarantsoa")
                .latitude(-21.4527)
                .longitude(47.0857)
                .build());

        warehouseRepository.save(Warehouse.builder()
                .name("Toamasina")
                .location("Toamasina")
                .latitude(-18.1492)
                .longitude(49.4023)
                .build());

        warehouseRepository.save(Warehouse.builder()
                .name("Mahajanga")
                .location("Mahajanga")
                .latitude(-15.7167)
                .longitude(46.3167)
                .build());

        warehouseRepository.save(Warehouse.builder()
                .name("Toliara")
                .location("Toliara")
                .latitude(-23.3500)
                .longitude(43.6667)
                .build());

        warehouseRepository.save(Warehouse.builder()
                .name("Taolagnaro")
                .location("Taolagnaro")
                .latitude(-25.0300)
                .longitude(46.9900)
                .build());

        warehouseRepository.save(Warehouse.builder()
                .name("Antsiranana")
                .location("Antsiranana")
                .latitude(-12.2765)
                .longitude(49.2917)
                .build());

        warehouseRepository.save(Warehouse.builder()
                .name("Morondava")
                .location("Morondava")
                .latitude(-20.2833)
                .longitude(44.2833)
                .build());
    }

    private void createTrucks() {

        if (truckRepository.count() > 0) {
            return;
        }

    truckRepository.save(Truck.builder()
            .code("TRUCK-01")
            .driver("Jean")
            .capacity(12.0)
            .status(TruckStatus.AVAILABLE)
            .refrigerated(true)
            .latitude(-18.8792)
            .longitude(47.5079)
            .build());

    truckRepository.save(Truck.builder()
            .code("TRUCK-02")
            .driver("Paul")
            .capacity(10.0)
            .status(TruckStatus.AVAILABLE)
            .refrigerated(false)
            .latitude(-19.8659)
            .longitude(47.0333)
            .build());

    truckRepository.save(Truck.builder()
            .code("TRUCK-03")
            .driver("Marie")
            .capacity(15.0)
            .status(TruckStatus.AVAILABLE)
            .refrigerated(true)
            .latitude(-18.1492)
            .longitude(49.4023)
            .build());

    truckRepository.save(Truck.builder()
            .code("TRUCK-04")
            .driver("David")
            .capacity(10.0)
            .status(TruckStatus.AVAILABLE)
            .refrigerated(false)
            .latitude(-21.4527)
            .longitude(47.0857)
            .build());

    truckRepository.save(Truck.builder()
            .code("TRUCK-05")
            .driver("Luc")
            .capacity(12.0)
            .status(TruckStatus.AVAILABLE)
            .refrigerated(true)
            .latitude(-15.7167)
            .longitude(46.3167)
            .build());

    truckRepository.save(Truck.builder()
            .code("TRUCK-06")
            .driver("Andry")
            .capacity(8.0)
            .status(TruckStatus.AVAILABLE)
            .refrigerated(false)
            .latitude(-23.3500)
            .longitude(43.6667)
            .build());
        }

    private void createOrders() {

        if (orderRepository.count() > 0) {
            return;
        }

        Warehouse tana = warehouseRepository
                .findByLocation("Antananarivo")
                .orElseThrow();

        orderRepository.save(Order.builder()
                .reference("ORD-001")
                .origin("Antananarivo")
                .destination("Toamasina")
                .warehouse(tana)
                .status(OrderStatus.PENDING)
                .priority("NORMAL")
                .fragile(false)
                .build());

        orderRepository.save(Order.builder()
                .reference("ORD-002")
                .origin("Antananarivo")
                .destination("Toliara")
                .warehouse(tana)
                .status(OrderStatus.PENDING)
                .priority("URGENT")
                .fragile(true)
                .currentTemperatureCelsius(28.0)
                .build());
    }
}