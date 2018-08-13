package com.pikl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.pikl.TokenType.*;

public class Scanner {

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();

        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("fun",    FUN);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);

        // STANDARD LIBRARY FUNCTION DEFINITIONS

        keywords.put("print",  PRINT);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void identifier() {

        // SCAN UNTIL WE REACH A NON ALPHANUMERIC CHAR

        while (isAlphaNumeric(peek()))
            advance();

        // See if the identifier is a reserved word.
        String text = source.substring(start, current);

        TokenType type = keywords.get(text);

        if (type == null)
            type = IDENTIFIER;

        addToken(type);

    }

    private void number() {

        boolean isfloat = false;        // DEFAULT = INTEGER VALUE

        // SCAN UNTIL WE FIND A VALUE THAT ISNT A DIGIT

        while (isDigit(peek())) advance();

        // Look for a fractional part indicating floating point value
        if (peek() == '.' && isDigit(peekNext())) {

            // We have a floating point value
            isfloat = true;

            // Consume the "."
            advance();

            // CONTINUE SCANNING UNTIL WE RUN OUT OF DIGITS

            while (isDigit(peek())) advance();
        }

        // USE OUR isfloat VALUE TO DETERMINE WHETHER TO PUSH A FLOAT OR INT TOKEN
        if (isfloat) {
            addToken(FNUMBER, Double.parseDouble(source.substring(start, current)));
        } else {
            addToken(INUMBER, Integer.parseInt(source.substring(start, current)));
        }

    }

    private void string() {

        // SCAN UNTIL WE FIND A CLOSING "

        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        // IF WE REACH THE END WITHOUT FINDING A CLOSING "
        // REPORT AN ERROR

        if (isAtEnd()) {
            Main.error(line, "Unterminated string.");
            return;
        }

        // OTHERWISE, CONTINUE

        advance();

        // Trim the surrounding quotes.
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    // USE THIS METHOD TO CHECK FOR A SECOND CHARACTER IN A TOKEN
    // WE INPUT THE EXPECTED CHARACTER
    // IF THE NEXT CHARACTER IN THE SOURCE MATCHES
    // WE REPORT A MATCH AND INCREMENT current

    private boolean match(char expected) {

        if (isAtEnd())
            return false;

        if (source.charAt(current) != expected)
            return false;

        current++;
        return true;
    }

    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private void scanToken() {

        // CHECK NEXT CHARACTER

        char c = advance();

        // TRY MATCH c TO OUR TOKENS

        // DEFAULT BEHAVIOUR:
        // REPORT ERROR AND CONTINUE SCANNING

        switch (c) {

            // SINGLE CHARACTER TOKENS

            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;

            // MULTIPLE CHARACTER TOKENS
            // THESE CAN BE EITHER SINGLE CHARACTER OR 2
            // USE THE MATCH FUNCTION TO CHECK

            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;

            // SPECIAL CASE FOR /
            // THIS COULD POTENTIALLY BE A COMMENT
            // IF WE MATCH THE SECOND / THEN WE ADVANCE TO THE END OF THE LINE
            // WITHOUT TOKENIZING ANY OF IT
            // EFFECTIVELY IGNORING/COMMENTING THE LINE
            // OTHERWISE THIS IS A SLASH SYMBOL FOR DIVISION
            // AND WE MUST ADD THE TOKEN

            case '/':
                if (match('/')) {
                    // A comment goes until the end of the line.
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;

            // WHITESPACE CHARACTERS
            // DEFAULT BEHAVIOUR IS TO IGNORE

            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;

            // NEW LINE

            case '\n':
                line++;
                break;

            // STRING INPUT

            case '"': string(); break;

            // HERE WE CHECK FOR NUMBERS AND KEYWORDS
            // DEFAULT BEHAVIOUR FOR UNEXPECTED CHARACTERS:
            // REPORT ERROR AND CONTINUE

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Main.error(line, "Unexpected character.");
                }
                break;
        }
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    public Scanner(String source) {
        this.source = source;
    }

}
