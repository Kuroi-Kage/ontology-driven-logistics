package com.flexchain.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class CapitalAgentService {

    public static final String NAME = "CapitalAgent";

    private final AtomicReference<BigDecimal> budget;

    public CapitalAgentService(@Value("${flexchain.capital.budget:5000}") double initialBudget) {
        this.budget = new AtomicReference<>(BigDecimal.valueOf(initialBudget));
    }

    public boolean canAfford(BigDecimal cost) {
        return budget.get().compareTo(cost) >= 0;
    }

    public BigDecimal debit(BigDecimal cost) {
        return budget.updateAndGet(current -> current.subtract(cost));
    }

    public BigDecimal getCurrentBudget() {
        return budget.get();
    }
}
