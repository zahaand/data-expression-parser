package ru.zahaand.dataexpr.exception;

/**
 * Thrown when an expression string cannot be parsed due to a syntax error or invalid input.
 *
 * <p>Common causes:
 * <ul>
 *   <li>The expression string is {@code null} or blank.</li>
 *   <li>The expression contains a syntax error (e.g. incomplete operator, unmatched parenthesis).</li>
 * </ul>
 *
 * <p>The exception message includes the ANTLR line and column of the first syntax error
 * in the format: {@code "Parse error at line <L>:<C>: <description>"}.
 */
public class ExpressionParseException extends RuntimeException {

    public ExpressionParseException(String message) {
        super(message);
    }

    public ExpressionParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
