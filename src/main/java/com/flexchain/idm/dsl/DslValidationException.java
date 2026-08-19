package com.flexchain.idm.dsl;

import java.util.List;

/**
 * Regroupe toutes les violations de contraintes semantiques (integrite
 * referentielle, champs obligatoires, valeurs invalides) detectees sur un
 * NetworkModel deja syntaxiquement valide.
 */
public class DslValidationException extends RuntimeException {
    private final List<String> errors;

    public DslValidationException(List<String> errors) {
        super(errors.size() + " erreur(s) semantique(s) detectee(s) dans le modele :\n - " +
                String.join("\n - ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
