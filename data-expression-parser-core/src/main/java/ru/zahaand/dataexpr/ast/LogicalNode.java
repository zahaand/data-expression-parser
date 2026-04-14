package ru.zahaand.dataexpr.ast;

/**
 * AST node representing a binary logical operation: {@code left AND right} or {@code left OR right}.
 *
 * @param left  the left operand (must evaluate to boolean)
 * @param op    the logical operator ({@link LogicalOperator#AND} or {@link LogicalOperator#OR})
 * @param right the right operand (must evaluate to boolean)
 */
public record LogicalNode(Expression left, LogicalOperator op, Expression right) implements Expression {}
