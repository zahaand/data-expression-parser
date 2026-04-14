package ru.zahaand.dataexpr.evaluator;

/**
 * An {@link EvaluationResult} carrying a {@code boolean} value.
 *
 * <p>Produced by comparison expressions, logical operators ({@code AND}, {@code OR}, {@code NOT}),
 * and {@code IN} / {@code NOT IN} checks.
 *
 * @param value the boolean result
 */
public record BooleanResult(boolean value) implements EvaluationResult {}
