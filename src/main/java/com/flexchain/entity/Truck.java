package com.flexchain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Truck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    private String driver;

    private Double capacity;

    @Enumerated(EnumType.STRING)
    private TruckStatus status;

    /**
     * Utilise par le pilier Ontologie : un camion refrigere est compatible
     * avec les commandes fragiles necessitant une chaine du froid (voir
     * com.flexchain.ontology.OntologyReasoningService).
     */
    private Boolean refrigerated;
    private String currentLocation;
     private Double latitude;

    private Double longitude;

}