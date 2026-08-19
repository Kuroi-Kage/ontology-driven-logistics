package com.flexchain.idm.dsl;

public record Token(TokenType type, String text, int line, int column) {

    @Override
    public String toString() {
        return type + "('" + text + "') @L" + line + ":C" + column;
    }
}
