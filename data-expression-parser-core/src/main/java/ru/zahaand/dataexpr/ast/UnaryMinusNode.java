package ru.zahaand.dataexpr.ast;

/**
 * AST node representing a unary negation: {@code -operand}.
 *
 * <p>Supports both simple negation ({@code -[x]}) and negation of parenthesized
 * expressions ({@code -([a] + [b])}).
 *
 * @param operand the expression being negated
 */
public record UnaryMinusNode(Expression operand) implements Expression {}
