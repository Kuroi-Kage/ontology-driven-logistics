package com.flexchain.repository;

import com.flexchain.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByIdGreaterThanOrderByIdAsc(Long id);
}