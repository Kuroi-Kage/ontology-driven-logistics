package com.flexchain.service;

import com.flexchain.entity.Truck;
import com.flexchain.entity.TruckStatus;
import com.flexchain.repository.TruckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TruckService {

    private final TruckRepository truckRepository;

    public List<Truck> findAll() {
        return truckRepository.findAll();
    }

    public Truck save(Truck truck) {
        return truckRepository.save(truck);
    }

    public Truck findById(Long id) {
        return truckRepository.findById(id).orElseThrow();
    }

    public List<Truck> availableTrucks() {
        return truckRepository.findByStatus(TruckStatus.AVAILABLE);
    }

    public Truck update(Long id, Truck updatedTruck) {
    Truck truck = findById(id);

    truck.setCode(updatedTruck.getCode());
    truck.setDriver(updatedTruck.getDriver());
    truck.setCapacity(updatedTruck.getCapacity());
    truck.setStatus(updatedTruck.getStatus());
    truck.setRefrigerated(updatedTruck.getRefrigerated());

    return truckRepository.save(truck);
}

    /**
     * Persiste la position reelle d'un camion apres un deplacement anime
     * cote frontend (ex. camion de remplacement arrive sur les lieux d'une
     * panne) : le terrain SMA et la base restent coherents.
     */
    public Truck updatePosition(Long id, Double latitude, Double longitude) {
        Truck truck = findById(id);
        truck.setLatitude(latitude);
        truck.setLongitude(longitude);
        return truckRepository.save(truck);
    }

    public Truck updateStatus(Long id, TruckStatus status) {
        Truck truck = findById(id);
        truck.setStatus(status);
        return truckRepository.save(truck);
    }
}