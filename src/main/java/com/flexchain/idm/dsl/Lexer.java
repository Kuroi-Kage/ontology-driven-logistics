package com.flexchain.idm.dsl;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyseur lexical du DSL FlexNet : transforme le texte source (.flexnet)
 * en une liste de jetons (tokens). Ecrit a la main (sans generateur type
 * ANTLR) pour un pipeline IDM entierement autonome et transparent.
 */
public class Lexer {

    private final String source;
    private int pos = 0;
    private int line = 1;
    private int column = 1;

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        Token t;
        do {
            t = nextToken();
            tokens.add(t);
        } while (t.type() != TokenType.EOF);
        return tokens;
    }

    private Token nextToken() {
        skipWhitespaceAndComments();

        if (isAtEnd()) {
            return make(TokenType.EOF, "");
        }

        char c = peek();
        int startLine = line;
        int startCol = column;

        if (c == '{') {
            advance();
            return new Token(TokenType.LBRACE, "{", startLine, startCol);
        }
        if (c == '}') {
            advance();
            return new Token(TokenType.RBRACE, "}", startLine, startCol);
        }
        if (c == ':') {
            advance();
            return new Token(TokenType.COLON, ":", startLine, startCol);
        }
        if (c == '"') {
            return readString(startLine, startCol);
        }
        if (Character.isDigit(c)) {
            return readNumber(startLine, startCol);
        }
        if (c == '-' && pos + 1 < source.length() && Character.isDigit(source.charAt(pos + 1))) {
            return readNumber(startLine, startCol);
        }
        if (Character.isLetter(c) || c == '_') {
            return readIdent(startLine, startCol);
        }

        throw new DslSyntaxException("Ligne " + startLine + ", colonne " + startCol +
                " : caractere inattendu '" + c + "'");
    }

    private Token readString(int startLine, int startCol) {
        advance(); // consume opening quote
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && peek() != '"') {
            if (peek() == '\n') {
                throw new DslSyntaxException("Ligne " + startLine + ", colonne " + startCol +
                        " : chaine de caracteres non fermee avant fin de ligne");
            }
            sb.append(advance());
        }
        if (isAtEnd()) {
            throw new DslSyntaxException("Ligne " + startLine + ", colonne " + startCol +
                    " : chaine de caracteres non fermee avant fin de fichier");
        }
        advance(); // consume closing quote
        return new Token(TokenType.STRING, sb.toString(), startLine, startCol);
    }

    private Token readNumber(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();
        if (!isAtEnd() && peek() == '-') {
            sb.append(advance());
        }
        while (!isAtEnd() && Character.isDigit(peek())) {
            sb.append(advance());
        }
        if (!isAtEnd() && peek() == '.' && pos + 1 < source.length() && Character.isDigit(source.charAt(pos + 1))) {
            sb.append(advance()); // '.'
            while (!isAtEnd() && Character.isDigit(peek())) {
                sb.append(advance());
            }
        }
        return new Token(TokenType.NUMBER, sb.toString(), startLine, startCol);
    }

    private Token readIdent(int startLine, int startCol) {
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            sb.append(advance());
        }
        return new Token(TokenType.IDENT, sb.toString(), startLine, startCol);
    }

    private void skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
            } else if (c == '/' && pos + 1 < source.length() && source.charAt(pos + 1) == '/') {
                while (!isAtEnd() && peek() != '\n') {
                    advance();
                }
            } else {
                break;
            }
        }
    }

    private boolean isAtEnd() {
        return pos >= source.length();
    }

    private char peek() {
        return source.charAt(pos);
    }

    private char advance() {
        char c = source.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private Token make(TokenType type, String text) {
        return new Token(type, text, line, column);
    }
}
