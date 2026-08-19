package com.flexchain.idm.dsl.ast;

public class TruckDecl {
    public final String id;
    public String code;
    public String driver;
    public Double capacity;
    public String status; // AVAILABLE | BUSY | BROKEN
    public final int line;

    public TruckDecl(String id, int line) {
        this.id = id;
        this.line = line;
    }
}
