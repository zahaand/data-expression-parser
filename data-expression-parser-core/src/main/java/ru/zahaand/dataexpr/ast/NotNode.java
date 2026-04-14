package ru.zahaand.dataexpr.ast;

/**
 * AST node representing logical negation: {@code NOT operand}.
 *
 * @param operand the expression to negate (must evaluate to boolean)
 */
public record NotNode(Expression operand) implements Expression {}
