package com.flexchain.idm.codegen;

import com.flexchain.idm.dsl.ast.NetworkModel;
import com.flexchain.idm.dsl.ast.OrderDecl;
import com.flexchain.idm.dsl.ast.TruckDecl;
import com.flexchain.idm.dsl.ast.WarehouseDecl;

/**
 * Transformation Modele-vers-Code (Model-to-Text) : produit le code source
 * Java complet d'un CommandLineRunner Spring qui initialise la base de
 * donnees conformement au NetworkModel issu du parseur FlexNet.
 *
 * C'est le coeur du pipeline IDM : le code genere n'est jamais ecrit a la
 * main, il est entierement derive du modele .flexnet. Toute evolution du
 * reseau logistique (nouvel entrepot, nouveau camion...) se fait en
 * modifiant le modele puis en regenerant, jamais en editant le fichier
 * genere directement.
 */
public class JavaDataLoaderGenerator {

    public String generate(NetworkModel model) {
        String className = "Generated" + toPascalCase(model.name) + "DataLoader";
        StringBuilder sb = new StringBuilder();

        sb.append("package com.flexchain.config.generated;\n\n");
        sb.append("import com.flexchain.entity.Order;\n");
        sb.append("import com.flexchain.entity.OrderStatus;\n");
        sb.append("import com.flexchain.entity.Truck;\n");
        sb.append("import com.flexchain.entity.TruckStatus;\n");
        sb.append("import com.flexchain.entity.Warehouse;\n");
        sb.append("import com.flexchain.repository.OrderRepository;\n");
        sb.append("import com.flexchain.repository.TruckRepository;\n");
        sb.append("import com.flexchain.repository.WarehouseRepository;\n");
        sb.append("import lombok.RequiredArgsConstructor;\n");
        sb.append("import org.springframework.boot.CommandLineRunner;\n");
        sb.append("import org.springframework.stereotype.Component;\n\n");
        sb.append("import java.util.HashMap;\n");
        sb.append("import java.util.Map;\n\n");

        sb.append("/**\n");
        sb.append(" * GENERE AUTOMATIQUEMENT par le compilateur FlexNet DSL (pipeline IDM).\n");
        sb.append(" * Modele source : \"").append(model.name).append("\"\n");
        sb.append(" * NE PAS MODIFIER A LA MAIN : toute correction doit etre faite dans le\n");
        sb.append(" * fichier .flexnet source puis regeneree via FlexNetCompiler.\n");
        sb.append(" */\n");
        sb.append("@Component\n");
        sb.append("@RequiredArgsConstructor\n");
        sb.append("public class ").append(className).append(" implements CommandLineRunner {\n\n");

        sb.append("    private final WarehouseRepository warehouseRepository;\n");
        sb.append("    private final TruckRepository truckRepository;\n");
        sb.append("    private final OrderRepository orderRepository;\n\n");

        sb.append("    @Override\n");
        sb.append("    public void run(String... args) {\n");
        sb.append("        Map<String, Warehouse> warehousesByDslId = new HashMap<>();\n\n");

        for (WarehouseDecl w : model.warehouses) {
            String var = "warehouse_" + w.id;
            sb.append("        Warehouse ").append(var).append(" = Warehouse.builder()\n");
            sb.append("                .name(").append(quote(w.name)).append(")\n");
            sb.append("                .location(").append(quote(w.location)).append(")\n");
            sb.append("                .latitude(").append(w.latitude).append(")\n");
            sb.append("                .longitude(").append(w.longitude).append(")\n");
            sb.append("                .build();\n");
            sb.append("        warehouseRepository.save(").append(var).append(");\n");
            sb.append("        warehousesByDslId.put(").append(quote(w.id)).append(", ").append(var).append(");\n\n");
        }

        for (TruckDecl t : model.trucks) {
            String var = "truck_" + t.id;
            sb.append("        Truck ").append(var).append(" = Truck.builder()\n");
            sb.append("                .code(").append(quote(t.code)).append(")\n");
            sb.append("                .driver(").append(quote(t.driver)).append(")\n");
            sb.append("                .capacity(").append(t.capacity).append(")\n");
            sb.append("                .status(TruckStatus.").append(t.status).append(")\n");
            sb.append("                .build();\n");
            sb.append("        truckRepository.save(").append(var).append(");\n\n");
        }

        for (OrderDecl o : model.orders) {
            String var = "order_" + o.id;
            sb.append("        Order ").append(var).append(" = Order.builder()\n");
            sb.append("                .reference(").append(quote(o.reference)).append(")\n");
            sb.append("                .destination(").append(quote(o.destination)).append(")\n");
            sb.append("                .warehouse(warehousesByDslId.get(").append(quote(o.warehouseRef)).append("))\n");
            sb.append("                .status(OrderStatus.").append(o.status).append(")\n");
            sb.append("                .build();\n");
            sb.append("        orderRepository.save(").append(var).append(");\n\n");
        }

        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    public String suggestedFileName(NetworkModel model) {
        return "Generated" + toPascalCase(model.name) + "DataLoader.java";
    }

    private String quote(String value) {
        String safe = value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + safe + "\"";
    }

    private String toPascalCase(String raw) {
        StringBuilder sb = new StringBuilder();
        boolean upperNext = true;
        for (char c : raw.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(upperNext ? Character.toUpperCase(c) : c);
                upperNext = false;
            } else {
                upperNext = true;
            }
        }
        return sb.toString();
    }
}
