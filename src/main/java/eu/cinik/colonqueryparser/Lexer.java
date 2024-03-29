package eu.cinik.colonqueryparser;

import eu.cinik.colonqueryparser.Lexer.LexerException;

import java.io.IOException;
import java.io.Reader;
import java.util.*;


class Lexer {
    private List<LexerState> allStates = new ArrayList<LexerState>();
    private LexerState currentState = null;
    private Integer currentChar;


    Lexer() {
        allStates.add(new EOFState());
        allStates.add(new SingleCharState(':', TokenType.COLON));
        allStates.add(new SingleCharState('-', TokenType.NEG));
        allStates.add(new SingleCharState('(', TokenType.OBRACKET));
        allStates.add(new SingleCharState(')', TokenType.CBRACKET));
        allStates.add(new SingleCharState('>', TokenType.HT));
        allStates.add(new SingleCharState('<', TokenType.LT));
        allStates.add(new SingleCharState('=', TokenType.EQ));
        allStates.add(new QuotedState());
        allStates.add(new TextState(true));
        allStates.add(new TextState(false));
    }

    Token next(Reader reader) {
        if (currentChar == null) {
            try {
                currentChar = reader.read();
            } catch (IOException e) {
                throw new LexerException(e);
            }
        }
        if (currentState == null) {
            this.currentState = this.allStates.stream().filter(lexerState -> lexerState.accept(currentChar))
                    .findFirst().orElse(null);
            if (this.currentState == null) {
                throw new LexerException(String.format("Unexpected character [%d] '%c'", currentChar, currentChar));
            }
        }
        while (true) {
            try {
                if (!currentState.accept(currentChar = reader.read())) break;
            } catch (IOException e) {
                throw new LexerException(e);
            }
            ; //do nothing
        }
        Token token = currentState.token();
        currentState.reset();
        currentState = null;
        return token;
    }

    /**
     * Exception represented all token parsing (lexing) problems
     */
    static class LexerException extends RuntimeException {
        public LexerException(String message) {
            super(message);
        }

        public LexerException(Throwable cause) {
            super(cause);
        }
    }

}


interface LexerState {
    boolean accept(int c);

    void reset();

    Token token();
}


class QuotedState implements LexerState {
    private StringBuffer buffer = new StringBuffer();

    private enum State {NoState, Accepted, SlashQuote, Terminate}

    private State state = State.NoState;

    @Override
    public boolean accept(int c) {
        switch (state) {
            case NoState:
                if (c == '\"') {
                    this.state = State.Accepted;
                    return true;
                } else return false;
            case Accepted:
                if (c == -1) {
                    throw new LexerException("Unexpected EOF");
                }
                if (c == '\"') this.state = State.Terminate;
                else if (c == '\\') {
                    this.state = State.SlashQuote;
                } else {
                    buffer.append((char) c);
                    return true;
                }
                return true;
            case SlashQuote:
                if (c == -1) {
                    throw new LexerException("Unexpected EOF");
                }
                buffer.append((char) c);
                this.state = State.Accepted;
                return true;
            case Terminate:
                return false;
            default:
                return false;
        }
    }

    @Override
    public void reset() {
        buffer = new StringBuffer();
        this.state = State.NoState;
    }

    @Override
    public Token token() {
        return new TextToken(TokenType.TEXTTOKEN, buffer.toString());
    }
}

class TextState implements LexerState {
    private StringBuffer buffer = new StringBuffer();
    private boolean accepted = false;
    private boolean whitespace;
    static private Set<Character> forbidden = new HashSet<>();

    static {
        forbidden.add(':');
        forbidden.add(')');
        forbidden.add('(');
        forbidden.add('>');
        forbidden.add('<');
        forbidden.add('=');
    }

    TextState(boolean whitespace) {
        this.whitespace = whitespace;
    }

    public boolean accept(int c) {
        if (c == -1) {
            this.accepted = false;
            return false;
        }
        boolean isAcceptableCharacter = (!this.whitespace && !Character.isWhitespace(c)) || (this.whitespace && Character.isWhitespace(c));
        if (isAcceptableCharacter && !forbidden.contains(Character.valueOf((char) c))) {
            buffer.append((char) c);
            accepted = true;
            return true;
        } else return false;
    }

    public void reset() {
        buffer = new StringBuffer();
        this.accepted = false;
    }

    public Token token() {
        if (!this.whitespace) return new TextToken(TokenType.TEXTTOKEN, buffer.toString());
        else if (this.whitespace) return new TextToken(TokenType.WHITESPACE, buffer.toString());
        else return null;
    }
}

class SingleCharState implements LexerState {
    private Integer searchChar;
    private TokenType tokenType;
    boolean accepted = false;

    SingleCharState(int c, TokenType tokenType) {
        this.searchChar = c;
        this.tokenType = tokenType;
    }

    public boolean accept(int c) {
        if (!this.accepted && c == this.searchChar) {
            this.accepted = true;
            return true;
        } else return false;
    }

    public void reset() {
        this.accepted = false;
    }

    public Token token() {
        return new Token(this.tokenType);
    }
}

class EOFState implements LexerState {

    private boolean accepted = false;

    @Override
    public boolean accept(int c) {
        if (c == -1 && !accepted) {
            this.accepted = true;
            return true;
        } else return false;
    }

    @Override
    public void reset() {
        this.accepted = false;
    }

    @Override
    public Token token() {
        return new Token(TokenType.EOF);
    }
}

enum TokenType {
    //    DOUBLE_QUOTE,
    COLON,
    WORD,
    NEG,
    OBRACKET,
    CBRACKET,
    HT,
    LT,
    EQ,
    TEXTTOKEN,
    WHITESPACE,
    EOF
}

class Token {
    private TokenType tokenType;

    Token(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return tokenType == token.tokenType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenType);
    }

    @Override
    public String toString() {
        return "Token{" + tokenType + '}';
    }
}


class TextToken extends Token {

    private String text;

    TextToken(TokenType tokenType, String text) {
        super(tokenType);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TextToken that = (TextToken) o;
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), text);
    }

    @Override
    public String toString() {
        return "TextToken{" + text + "}";
    }
}