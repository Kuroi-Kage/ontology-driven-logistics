package com.flexchain.idm.dsl.ast;

public class OrderDecl {
    public final String id;
    public String reference;
    public String destination;
    public String warehouseRef; 
    public String status; 
    public final int line;

    public OrderDecl(String id, int line) {
        this.id = id;
        this.line = line;
    }
}
