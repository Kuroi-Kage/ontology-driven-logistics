package com.flexchain.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
public class IncidentEventDto {

    private Long id;
    private String type;
    private String description;

    private Long failedTruckId;
    private String failedTruckCode;

    private Boolean resolved;
    private Long replacementTruckId;
    private String replacementTruckCode;
    private Integer reassignedOrders;
    private BigDecimal negotiatedCost;
    private String message;
    private List<String> negotiationLog;

    private LocalDateTime createdAt;
}
