package com.flexchain.service;

import com.flexchain.dto.IncidentEventDto;
import com.flexchain.entity.Incident;
import com.flexchain.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentService {

    private final IncidentRepository repository;

    @Transactional
    public Incident save(Incident incident) {
        return repository.save(incident);
    }

    public List<Incident> findAll() {
        return repository.findAll();
    }

    /**
     * Renvoie les incidents crees apres afterId (exclu), tries par id
     * croissant : c'est ce qu'interroge le frontend en polling pour
     * detecter et rejouer les pannes declenchees automatiquement par
     * l'AutoIncidentOrchestrator (comme celles declenchees manuellement).
     *
     * @Transactional est indispensable ici : le champ negotiationLog est
     * annote @Lob, et sur PostgreSQL Hibernate le lit via un flux lie a la
     * transaction JDBC. Sans transaction active au moment de l'appel a
     * incident.getNegotiationLog() (dans toDto), la connexion peut deja
     * etre rendue au pool -> "Unable to access lob stream".
     */
    public List<IncidentEventDto> findEventsAfter(Long afterId) {
        return repository.findByIdGreaterThanOrderByIdAsc(afterId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public Long latestId() {
        return repository.findAll().stream()
                .map(Incident::getId)
                .max(Long::compareTo)
                .orElse(0L);
    }

    private IncidentEventDto toDto(Incident incident) {
        return IncidentEventDto.builder()
                .id(incident.getId())
                .type(incident.getType() != null ? incident.getType().name() : null)
                .description(incident.getDescription())
                .failedTruckId(incident.getTruck() != null ? incident.getTruck().getId() : null)
                .failedTruckCode(incident.getTruck() != null ? incident.getTruck().getCode() : null)
                .resolved(incident.getResolved())
                .replacementTruckId(incident.getReplacementTruck() != null ? incident.getReplacementTruck().getId() : null)
                .replacementTruckCode(incident.getReplacementTruck() != null ? incident.getReplacementTruck().getCode() : null)
                .reassignedOrders(incident.getReassignedOrders())
                .negotiatedCost(incident.getNegotiatedCost())
                .message(incident.getResultMessage())
                .negotiationLog(incident.getNegotiationLog() != null
                        ? Arrays.stream(incident.getNegotiationLog().split("\n")).filter(s -> !s.isBlank()).toList()
                        : List.of())
                .createdAt(incident.getCreatedAt())
                .build();
    }
}