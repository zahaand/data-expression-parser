package ru.zahaand.dataexpr.evaluator;

/**
 * An {@link EvaluationResult} carrying a {@code double} value.
 *
 * <p>Produced by arithmetic expressions, built-in function calls, and custom function calls.
 *
 * @param value the numeric result
 */
public record DoubleResult(double value) implements EvaluationResult {}
