package com.flexchain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;

    private String origin;

    private String destination;

    @ManyToOne
    private Truck truck;

    @ManyToOne
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String priority;

    private Boolean fragile;

    private Double currentTemperatureCelsius;
}