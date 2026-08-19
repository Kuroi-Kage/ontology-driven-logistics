package com.flexchain.repository;

import com.flexchain.entity.Truck;
import com.flexchain.entity.TruckStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TruckRepository extends JpaRepository<Truck, Long> {

    List<Truck> findByStatus(TruckStatus status);

    Optional<Truck> findByCode(String code);

}