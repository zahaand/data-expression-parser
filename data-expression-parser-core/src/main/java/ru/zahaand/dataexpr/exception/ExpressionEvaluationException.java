package ru.zahaand.dataexpr.exception;

/**
 * Thrown when an expression cannot be evaluated due to a runtime error.
 *
 * <p>Common causes:
 * <ul>
 *   <li>A referenced field is not present in
 *       {@link ru.zahaand.dataexpr.evaluator.EvaluationContext}.</li>
 *   <li>An arithmetic operation involves incompatible types (e.g. string in arithmetic context).</li>
 *   <li>Division by zero.</li>
 *   <li>An unknown function name is called.</li>
 *   <li>A built-in or custom function is called with the wrong number of arguments.</li>
 *   <li>The field referenced in {@code IN [field]} does not contain a {@link java.util.List}.</li>
 *   <li>A custom function throws a {@link RuntimeException} (wrapped as cause).</li>
 *   <li>The result type does not match the expected type in
 *       {@link ru.zahaand.dataexpr.parser.DataExpressionParser#evaluateBoolean}
 *       or {@link ru.zahaand.dataexpr.parser.DataExpressionParser#evaluateDouble}.</li>
 * </ul>
 */
public class ExpressionEvaluationException extends RuntimeException {

    public ExpressionEvaluationException(String message) {
        super(message);
    }

    public ExpressionEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
