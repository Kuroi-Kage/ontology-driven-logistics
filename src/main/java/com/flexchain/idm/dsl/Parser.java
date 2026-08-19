package com.flexchain.idm.dsl;

import com.flexchain.idm.dsl.ast.NetworkModel;
import com.flexchain.idm.dsl.ast.OrderDecl;
import com.flexchain.idm.dsl.ast.TruckDecl;
import com.flexchain.idm.dsl.ast.WarehouseDecl;

import java.util.List;
import java.util.Set;


public class Parser {

    private static final Set<String> TRUCK_STATUSES = Set.of("AVAILABLE", "BUSY", "BROKEN");
    private static final Set<String> ORDER_STATUSES = Set.of("PENDING", "IN_PROGRESS", "DELIVERED");

    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public NetworkModel parse() {
        expectKeyword("network");
        String name = expect(TokenType.STRING).text();
        expect(TokenType.LBRACE);

        NetworkModel model = new NetworkModel(name);

        while (!check(TokenType.RBRACE)) {
            parseDeclaration(model);
        }
        expect(TokenType.RBRACE);
        expect(TokenType.EOF);
        return model;
    }

    private void parseDeclaration(NetworkModel model) {
        Token t = peek();
        if (t.type() != TokenType.IDENT) {
            throw error(t, "attendu 'warehouse', 'truck' ou 'order'");
        }
        switch (t.text()) {
            case "warehouse" -> model.warehouses.add(parseWarehouse());
            case "truck" -> model.trucks.add(parseTruck());
            case "order" -> model.orders.add(parseOrder());
            default -> throw error(t, "declaration inconnue '" + t.text() +
                    "' (attendu 'warehouse', 'truck' ou 'order')");
        }
    }

    private WarehouseDecl parseWarehouse() {
        Token kw = expectKeyword("warehouse");
        String id = expect(TokenType.IDENT).text();
        WarehouseDecl decl = new WarehouseDecl(id, kw.line());
        expect(TokenType.LBRACE);
        while (!check(TokenType.RBRACE)) {
            Token prop = expect(TokenType.IDENT);
            expect(TokenType.COLON);
            switch (prop.text()) {
                case "name" -> decl.name = expect(TokenType.STRING).text();
                case "location" -> decl.location = expect(TokenType.STRING).text();
                case "latitude" -> decl.latitude = Double.parseDouble(expect(TokenType.NUMBER).text());
                case "longitude" -> decl.longitude = Double.parseDouble(expect(TokenType.NUMBER).text());
                default -> throw error(prop, "propriete inconnue pour warehouse : '" + prop.text() +
                        "' (attendu 'name', 'location', 'latitude' ou 'longitude')");
            }
        }
        expect(TokenType.RBRACE);
        return decl;
    }

    private TruckDecl parseTruck() {
        Token kw = expectKeyword("truck");
        String id = expect(TokenType.IDENT).text();
        TruckDecl decl = new TruckDecl(id, kw.line());
        expect(TokenType.LBRACE);
        while (!check(TokenType.RBRACE)) {
            Token prop = expect(TokenType.IDENT);
            expect(TokenType.COLON);
            switch (prop.text()) {
                case "code" -> decl.code = expect(TokenType.STRING).text();
                case "driver" -> decl.driver = expect(TokenType.STRING).text();
                case "capacity" -> decl.capacity = Double.parseDouble(expect(TokenType.NUMBER).text());
                case "status" -> decl.status = expectEnum(TRUCK_STATUSES, "truck.status");
                default -> throw error(prop, "propriete inconnue pour truck : '" + prop.text() +
                        "' (attendu 'code', 'driver', 'capacity' ou 'status')");
            }
        }
        expect(TokenType.RBRACE);
        return decl;
    }

    private OrderDecl parseOrder() {
        Token kw = expectKeyword("order");
        String id = expect(TokenType.IDENT).text();
        OrderDecl decl = new OrderDecl(id, kw.line());
        expect(TokenType.LBRACE);
        while (!check(TokenType.RBRACE)) {
            Token prop = expect(TokenType.IDENT);
            expect(TokenType.COLON);
            switch (prop.text()) {
                case "reference" -> decl.reference = expect(TokenType.STRING).text();
                case "destination" -> decl.destination = expect(TokenType.STRING).text();
                case "warehouse" -> decl.warehouseRef = expect(TokenType.IDENT).text();
                case "status" -> decl.status = expectEnum(ORDER_STATUSES, "order.status");
                default -> throw error(prop, "propriete inconnue pour order : '" + prop.text() +
                        "' (attendu 'reference', 'destination', 'warehouse' ou 'status')");
            }
        }
        expect(TokenType.RBRACE);
        return decl;
    }

    private String expectEnum(Set<String> allowed, String fieldName) {
        Token t = expect(TokenType.IDENT);
        if (!allowed.contains(t.text())) {
            throw error(t, "valeur invalide pour " + fieldName + " : '" + t.text() +
                    "' (attendu une valeur parmi " + allowed + ")");
        }
        return t.text();
    }

    private Token expectKeyword(String keyword) {
        Token t = expect(TokenType.IDENT);
        if (!t.text().equals(keyword)) {
            throw error(t, "attendu le mot-cle '" + keyword + "' mais trouve '" + t.text() + "'");
        }
        return t;
    }

    private Token expect(TokenType type) {
        Token t = peek();
        if (t.type() != type) {
            throw error(t, "attendu " + type + " mais trouve " + t.type() + " ('" + t.text() + "')");
        }
        return advance();
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token advance() {
        return tokens.get(pos++);
    }

    private DslSyntaxException error(Token t, String detail) {
        return new DslSyntaxException("Ligne " + t.line() + ", colonne " + t.column() + " : " + detail);
    }
}
