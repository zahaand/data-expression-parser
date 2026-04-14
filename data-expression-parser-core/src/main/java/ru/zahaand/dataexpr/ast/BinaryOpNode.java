package ru.zahaand.dataexpr.ast;

/**
 * AST node representing a binary arithmetic operation: {@code left op right}.
 *
 * @param left  the left operand
 * @param op    the arithmetic operator
 * @param right the right operand
 */
public record BinaryOpNode(Expression left, ArithmeticOperator op, Expression right) implements Expression {}
