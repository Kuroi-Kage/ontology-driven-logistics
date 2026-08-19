package com.flexchain.agent.protocol;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Message échangé entre deux agents du SMA (émetteur, récepteur, acte de langage, contenu).
 * Chaque interaction du protocole de négociation (Contract Net) est matérialisée par une
 * instance de ce message, ce qui permet de tracer et d'auditer la communication inter-agents.
 */
@Getter
@Builder
public class ACLMessage {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private String sender;
    private String receiver;
    private Performative performative;
    private String content;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s : %s(%s)",
                timestamp.format(FORMATTER), sender, receiver, performative, content);
    }
}
