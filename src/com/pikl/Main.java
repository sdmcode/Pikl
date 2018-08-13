package com.pikl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    private static final Interpreter interpreter = new Interpreter();

    private static void report(int line, String where, String message) {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
                "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    static void begin(String data) {

        System.out.println("Scanning...");

        Scanner scanner = new Scanner(data);
        List<Token> tokens = scanner.scanTokens();

        Parser parser = new Parser(tokens);

        List<Statement> statements = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) return;

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        // Stop if there was a semantic error.
        if (hadError) return;

        interpreter.interpret(statements);

    }

    static void begin(byte[] data) {
        begin(new String(data, Charset.defaultCharset()));
    }

    /*
        LOAD FILE TO BYTE ARRAY
        PASS LOADED DATA TO BEGIN FUNCTION FOR PROCESSING
        IF THE FILE CAN NOT BE READ, THIS WILL THROW AN IOException
    */

    static void load(String dir) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get(dir));
        begin(data);

        if (hadError) {
            System.out.println("Unable to compile file <" + dir + "> exiting...");
            System.exit(65);
        }
        if (hadRuntimeError) {
            System.out.println("RUNTIME ERROR <" + dir + "> exiting...");
            System.exit(70);
        }
    }

    /*
        ENTRY POINT

        FIRST ARGUMENT SHOULD BE DIRECTORY TO A PIKL FILE
        MAIN FUNCTION PASSES A STRING CONTAINING DIRECTORY TO LOAD FUNCTION
    */

    public static void main(String args[]) {
        System.out.println("Initialising...");

        if (args.length != 1) {
            System.out.println("Invalid number of args, exiting...");
            System.exit(64);
        } else {
            try {
                load(args[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
