package com.flexchain.idm.dsl;

import com.flexchain.idm.dsl.ast.NetworkModel;
import com.flexchain.idm.dsl.ast.OrderDecl;
import com.flexchain.idm.dsl.ast.TruckDecl;
import com.flexchain.idm.dsl.ast.WarehouseDecl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class SemanticValidator {

    public void validate(NetworkModel model) {
        List<String> errors = new ArrayList<>();

        Set<String> warehouseIds = uniqueIds(model.warehouses.stream().map(w -> w.id).toList(),
                "warehouse", errors);
        uniqueIds(model.trucks.stream().map(t -> t.id).toList(), "truck", errors);
        uniqueIds(model.orders.stream().map(o -> o.id).toList(), "order", errors);

        Set<String> truckCodes = new HashSet<>();
        for (WarehouseDecl w : model.warehouses) {
            requireNonBlank(w.name, "warehouse " + w.id + " (ligne " + w.line + ") : 'name' obligatoire", errors);
            requireNonBlank(w.location, "warehouse " + w.id + " (ligne " + w.line + ") : 'location' obligatoire", errors);

            if (w.latitude == null) {
                errors.add("warehouse " + w.id + " (ligne " + w.line + ") : 'latitude' obligatoire");
            } else if (w.latitude < -90 || w.latitude > 90) {
                errors.add("warehouse " + w.id + " (ligne " + w.line + ") : 'latitude' doit etre comprise entre -90 et 90 (valeur=" + w.latitude + ")");
            }

            if (w.longitude == null) {
                errors.add("warehouse " + w.id + " (ligne " + w.line + ") : 'longitude' obligatoire");
            } else if (w.longitude < -180 || w.longitude > 180) {
                errors.add("warehouse " + w.id + " (ligne " + w.line + ") : 'longitude' doit etre comprise entre -180 et 180 (valeur=" + w.longitude + ")");
            }
        }

        for (TruckDecl t : model.trucks) {
            requireNonBlank(t.code, "truck " + t.id + " (ligne " + t.line + ") : 'code' obligatoire", errors);
            requireNonBlank(t.driver, "truck " + t.id + " (ligne " + t.line + ") : 'driver' obligatoire", errors);
            requireNonBlank(t.status, "truck " + t.id + " (ligne " + t.line + ") : 'status' obligatoire", errors);

            if (t.capacity == null) {
                errors.add("truck " + t.id + " (ligne " + t.line + ") : 'capacity' obligatoire");
            } else if (t.capacity <= 0) {
                errors.add("truck " + t.id + " (ligne " + t.line + ") : 'capacity' doit etre strictement positive (valeur=" + t.capacity + ")");
            }

            if (t.code != null) {
                if (!truckCodes.add(t.code)) {
                    errors.add("truck " + t.id + " (ligne " + t.line + ") : code '" + t.code + "' deja utilise par un autre camion");
                }
            }
        }

        for (OrderDecl o : model.orders) {
            requireNonBlank(o.reference, "order " + o.id + " (ligne " + o.line + ") : 'reference' obligatoire", errors);
            requireNonBlank(o.destination, "order " + o.id + " (ligne " + o.line + ") : 'destination' obligatoire", errors);
            requireNonBlank(o.status, "order " + o.id + " (ligne " + o.line + ") : 'status' obligatoire", errors);

            if (o.warehouseRef == null) {
                errors.add("order " + o.id + " (ligne " + o.line + ") : 'warehouse' obligatoire");
            } else if (!warehouseIds.contains(o.warehouseRef)) {
                errors.add("order " + o.id + " (ligne " + o.line + ") : reference a un warehouse inconnu '" +
                        o.warehouseRef + "' (integrite referentielle violee)");
            }
        }

        if (!errors.isEmpty()) {
            throw new DslValidationException(errors);
        }
    }

    private Set<String> uniqueIds(List<String> ids, String kind, List<String> errors) {
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) {
                errors.add("identifiant " + kind + " '" + id + "' declare plusieurs fois");
            }
        }
        return seen;
    }

    private void requireNonBlank(String value, String errorMessage, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add(errorMessage);
        }
    }
}
