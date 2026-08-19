package com.flexchain.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class CapitalDecisionResult {
    private String action;
    private String message;
    private BigDecimal capitalBeforeAction;
    private BigDecimal actionCost;
    private BigDecimal remainingCapital;

    @JsonProperty("profitable")
    private boolean profitable;

    private String nextRoute;

    public CapitalDecisionResult(String action, String message, BigDecimal capitalBeforeAction,
                                 BigDecimal actionCost, BigDecimal remainingCapital,
                                 boolean profitable, String nextRoute) {
        this.action = action;
        this.message = message;
        this.capitalBeforeAction = capitalBeforeAction;
        this.actionCost = actionCost;
        this.remainingCapital = remainingCapital;
        this.profitable = profitable;
        this.nextRoute = nextRoute;
    }

    public String getAction() { return action; }
    public String getMessage() { return message; }
    public BigDecimal getCapitalBeforeAction() { return capitalBeforeAction; }
    public BigDecimal getActionCost() { return actionCost; }
    public BigDecimal getRemainingCapital() { return remainingCapital; }
    public boolean isProfitable() { return profitable; }
    public String getNextRoute() { return nextRoute; }
}