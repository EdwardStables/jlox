package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

class Scanner{
    //Store raw source code here
    private final String source;
    //Fill list with tokens once generated
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static{
        keywords = new HashMap<>();
        keywords.put("and",AND);
        keywords.put("class",CLASS);
        keywords.put("false",FALSE);
        keywords.put("else",ELSE);
        keywords.put("fun",FUN);
        keywords.put("for",FOR);
        keywords.put("if",IF);
        keywords.put("nil",NIL);
        keywords.put("or",OR);
        keywords.put("print",PRINT);
        keywords.put("return",RETURN);
        keywords.put("super",SUPER);
        keywords.put("this",THIS);
        keywords.put("true",TRUE);
        keywords.put("var",VAR);
        keywords.put("while",WHILE);
        
    }

    Scanner(String source){
        this.source = source;
    }

    List<Token> scanTokens(){
        while (!isAtEnd()){
            //we are at the start of the next lexeme
            start = current;
            //reads and adds the next token
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken(){
        //get next char
        char c = advance();

        switch (c) {
            //Single character tokens
            case '(' : addToken(LEFT_PAREN); break;
            case ')' : addToken(RIGHT_PAREN); break;
            case '{' : addToken(LEFT_BRACE); break;
            case '}' : addToken(RIGHT_BRACE); break;
            case ',' : addToken(COMMA); break;
            case '.' : addToken(DOT); break;
            case '-' : addToken(MINUS); break;
            case '+' : addToken(PLUS); break;
            case ';' : addToken(SEMICOLON); break;
            case '*' : addToken(STAR); break;

            //Single or double character tokens
            case '!' : addToken(!match('=') ? BANG : BANGEQUAL); break;
            case '=' : addToken(!match('=') ? EQUAL : EQUAL_EQUAL); break;
            case '>' : addToken(!match('=') ? GREATER : GREATER_EQUAL); break;
            case '<' : addToken(!match('=') ? LESS : LESS_EQUAL); break;

            //Special case, can be a divide or a comment
            case '/' : 
                if (match('/')){//is a comment
                    //goes until the end of the line
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                } 
                break;

            //whitespace
            case ' ':
            case '\t':
            case '\r':
                break;

            //newline
            case '\n':
                line++;
                break;

            //literals
            case '"': string(); break;

            default:
                //put number literals in default to avoid matching annoyances
                if (isDigit(c)){
                    number();
                }
                //match to reserved words and variable names
                else if (isAlpha(c)){    
                    identifier();
                }else{
                    Lox.error(line, "Unexpected character");
                }
                break;
        }
    }

    //see if the current char matches expected, 
    //return true and advance current if so
    private boolean match(char expected){
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private boolean isAtEnd(){
        return current >= source.length();
    }

    private char advance(){
        current++;
        return source.charAt(current-1);
    }

    private char peek(){
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext(){
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    //addToken for when literals aren't needed
    private void addToken(TokenType type){
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal){
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private void string(){
        while (peek() != '"' && !isAtEnd()){
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()){
            Lox.error(line, "Unterminated string");
            return;
        }

        //If reached here then current is the closing " char
        advance();

        //split the quotes off the string
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private boolean isDigit(char c){
        return c >= '0' && c <= '9';
    }

    //matches lowercase, uppercase, and underscore
    private boolean isAlpha(char c){
        return ((c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z')) ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c){
        return isAlpha(c) || isDigit(c);
    }

    private void number(){
        while (isDigit(peek())) advance();

        //find fractional part

        if (peek() == '.' && isDigit(peekNext())){
            advance(); //consume the '.'

            while (isDigit(peek())) advance(); //get rest of literal
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void identifier(){
        while( isAlphaNumeric(peek()) ) advance();

        String text = source.substring(start, current);

        TokenType type = keywords.get(text);

        if (type == null)  type = IDENTIFIER;

        addToken(type);
    }
}