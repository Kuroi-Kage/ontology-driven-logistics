package com.flexchain.agent;

import com.flexchain.entity.Truck;

import java.math.BigDecimal;


public record TruckProposal(Truck truck, BigDecimal estimatedCost) {
}
