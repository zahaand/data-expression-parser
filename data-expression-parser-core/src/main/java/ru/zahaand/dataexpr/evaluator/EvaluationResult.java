package ru.zahaand.dataexpr.evaluator;

/**
 * Sealed result type returned by
 * {@link ru.zahaand.dataexpr.parser.DataExpressionParser#evaluate(String, EvaluationContext)}.
 *
 * <p>Permitted implementations: {@link DoubleResult} (numeric result) and
 * {@link BooleanResult} (boolean result).
 *
 * <p>Use pattern matching to unwrap:
 * <pre>{@code
 * EvaluationResult result = parser.evaluate(expression, ctx);
 * if (result instanceof DoubleResult d) {
 *     double value = d.value();
 * } else if (result instanceof BooleanResult b) {
 *     boolean flag = b.value();
 * }
 * }</pre>
 *
 * <p>Alternatively, use the convenience methods
 * {@link ru.zahaand.dataexpr.parser.DataExpressionParser#evaluateDouble}
 * and {@link ru.zahaand.dataexpr.parser.DataExpressionParser#evaluateBoolean}
 * when the result type is known in advance.
 */
public sealed interface EvaluationResult permits DoubleResult, BooleanResult {}
