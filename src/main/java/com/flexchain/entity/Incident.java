package com.flexchain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private IncidentType type;

    private String description;

    @ManyToOne
    private Truck truck;
    
    private Boolean resolved;

    @ManyToOne
    private Truck replacementTruck;

    private Integer reassignedOrders;

    private java.math.BigDecimal negotiatedCost;

    @Column(length = 1000)
    private String resultMessage;

    @Lob
    private String negotiationLog;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}