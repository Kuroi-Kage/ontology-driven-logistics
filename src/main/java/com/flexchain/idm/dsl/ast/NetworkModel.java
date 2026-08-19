package com.flexchain.idm.dsl.ast;

import java.util.ArrayList;
import java.util.List;


public class NetworkModel {
    public final String name;
    public final List<WarehouseDecl> warehouses = new ArrayList<>();
    public final List<TruckDecl> trucks = new ArrayList<>();
    public final List<OrderDecl> orders = new ArrayList<>();

    public NetworkModel(String name) {
        this.name = name;
    }
}
