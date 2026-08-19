package com.flexchain.agent;

import com.flexchain.agent.protocol.ACLMessage;
import com.flexchain.agent.protocol.Performative;
import com.flexchain.entity.Truck;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
@RequiredArgsConstructor
public class CoordinatorAgent {

    public static final String NAME = "CoordinatorAgent";

    private final TruckAgent truckAgent;
    private final CapitalAgentService capitalAgentService;

    public NegotiationOutcome negotiateReplacement(Truck failedTruck) {
        List<ACLMessage> transcript = new ArrayList<>();

        transcript.add(message(TruckAgent.NAME, NAME, Performative.INFORM,
                "Panne du camion " + failedTruck.getCode()));

        transcript.add(message(NAME, TruckAgent.NAME, Performative.CFP,
                "Rechercher un remplacement pour " + failedTruck.getCode()));

        List<TruckProposal> proposals = truckAgent.proposeCandidates(failedTruck);

        if (proposals.isEmpty()) {
            transcript.add(message(TruckAgent.NAME, NAME, Performative.REFUSE,
                    "Aucun camion disponible avec une capacité suffisante"));
            return new NegotiationOutcome(null, null, false, transcript);
        }

        for (TruckProposal proposal : proposals) {
            transcript.add(message(TruckAgent.NAME, NAME, Performative.PROPOSE,
                    proposal.truck().getCode() + " coût_estimé=" + proposal.estimatedCost()));

            transcript.add(message(NAME, CapitalAgentService.NAME, Performative.CFP,
                    "Valider le coût " + proposal.estimatedCost() + " pour " + proposal.truck().getCode()));

            if (capitalAgentService.canAfford(proposal.estimatedCost())) {
                capitalAgentService.debit(proposal.estimatedCost());

                transcript.add(message(CapitalAgentService.NAME, NAME, Performative.ACCEPT_PROPOSAL,
                        "Budget suffisant, solde_restant=" + capitalAgentService.getCurrentBudget()));

                transcript.add(message(NAME, TruckAgent.NAME, Performative.ACCEPT_PROPOSAL,
                        proposal.truck().getCode() + " retenu pour remplacement"));

                return new NegotiationOutcome(proposal.truck(), proposal.estimatedCost(), true, transcript);
            }

            transcript.add(message(CapitalAgentService.NAME, NAME, Performative.REJECT_PROPOSAL,
                    "Budget insuffisant, solde_actuel=" + capitalAgentService.getCurrentBudget()));

            transcript.add(message(NAME, TruckAgent.NAME, Performative.REJECT_PROPOSAL,
                    "Offre pour " + proposal.truck().getCode() + " rejetée, proposition suivante demandée"));
        }

        return new NegotiationOutcome(null, null, false, transcript);
    }

    private ACLMessage message(String sender, String receiver, Performative performative, String content) {
        return ACLMessage.builder()
                .sender(sender)
                .receiver(receiver)
                .performative(performative)
                .content(content)
                .build();
    }
}
