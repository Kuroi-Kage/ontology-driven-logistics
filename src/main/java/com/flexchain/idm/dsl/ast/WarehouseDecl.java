package com.flexchain.idm.dsl.ast;

public class WarehouseDecl {
    public final String id;
    public String name;
    public String location;
    public Double latitude;
    public Double longitude;
    public final int line;

    public WarehouseDecl(String id, int line) {
        this.id = id;
        this.line = line;
    }
}
