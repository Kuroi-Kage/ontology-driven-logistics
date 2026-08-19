package com.flexchain.idm.dsl;

/**
 * Erreur de syntaxe dans un fichier .flexnet : jeton inattendu, chaine non
 * fermee, etc. Le message contient toujours la position (ligne/colonne)
 * pour permettre a l'utilisateur de localiser l'erreur dans le modele source.
 */
public class DslSyntaxException extends RuntimeException {
    public DslSyntaxException(String message) {
        super(message);
    }
}
