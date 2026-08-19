package com.flexchain.agent;

import com.flexchain.agent.protocol.ACLMessage;
import com.flexchain.entity.Truck;

import java.math.BigDecimal;
import java.util.List;


public record NegotiationOutcome(
        Truck selectedTruck,
        BigDecimal negotiatedCost,
        boolean accepted,
        List<ACLMessage> transcript
) {
}
