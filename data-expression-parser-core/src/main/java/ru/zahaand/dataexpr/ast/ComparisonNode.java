package ru.zahaand.dataexpr.ast;

/**
 * AST node representing a comparison: {@code left op right}.
 *
 * <p>Ordering operators ({@code >}, {@code <}, {@code >=}, {@code <=}) require numeric operands.
 * Equality operators ({@code ==}, {@code !=}) support both numeric and string operands;
 * mixed-type comparisons always return {@code false} for {@code ==} and {@code true} for {@code !=}.
 *
 * @param left  the left operand
 * @param op    the comparison operator
 * @param right the right operand
 */
public record ComparisonNode(Expression left, ComparisonOperator op, Expression right) implements Expression {}
