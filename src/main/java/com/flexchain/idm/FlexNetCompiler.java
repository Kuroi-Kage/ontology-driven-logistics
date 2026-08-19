package com.flexchain.idm;

import com.flexchain.idm.codegen.JavaDataLoaderGenerator;
import com.flexchain.idm.dsl.DslSyntaxException;
import com.flexchain.idm.dsl.DslValidationException;
import com.flexchain.idm.dsl.Lexer;
import com.flexchain.idm.dsl.Parser;
import com.flexchain.idm.dsl.SemanticValidator;
import com.flexchain.idm.dsl.Token;
import com.flexchain.idm.dsl.ast.NetworkModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public class FlexNetCompiler {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java com.flexchain.idm.FlexNetCompiler <modele.flexnet> [dossier_sortie]");
            System.exit(2);
        }

        Path input = Path.of(args[0]);
        Path outputDir = Path.of(args.length > 1 ? args[1] : "generated");

        try {
            compile(input, outputDir);
        } catch (DslSyntaxException e) {
            System.err.println("Erreur de syntaxe dans " + input + " :");
            System.err.println("  " + e.getMessage());
            System.exit(1);
        } catch (DslValidationException e) {
            System.err.println("Erreur(s) semantique(s) dans " + input + " :");
            for (String err : e.getErrors()) {
                System.err.println("  - " + err);
            }
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Erreur d'entree/sortie : " + e.getMessage());
            System.exit(1);
        }
    }

    public static Path compile(Path input, Path outputDir) throws IOException {
        String source = Files.readString(input);

        System.out.println("=== [1/4] Analyse lexicale ===");
        List<Token> tokens = new Lexer(source).tokenize();
        System.out.println(tokens.size() + " jetons produits.");

        System.out.println("=== [2/4] Analyse syntaxique ===");
        NetworkModel model = new Parser(tokens).parse();
        System.out.println("Modele '" + model.name + "' : " +
                model.warehouses.size() + " entrepot(s), " +
                model.trucks.size() + " camion(s), " +
                model.orders.size() + " commande(s).");

        System.out.println("=== [3/4] Validation semantique ===");
        new SemanticValidator().validate(model);
        System.out.println("Modele valide (contraintes du meta-modele respectees).");

        System.out.println("=== [4/4] Generation de code (Model-to-Text) ===");
        JavaDataLoaderGenerator generator = new JavaDataLoaderGenerator();
        String javaCode = generator.generate(model);

        Files.createDirectories(outputDir);
        Path outputFile = outputDir.resolve(generator.suggestedFileName(model));
        Files.writeString(outputFile, javaCode);

        System.out.println("Code Java genere : " + outputFile.toAbsolutePath());
        return outputFile;
    }
}
