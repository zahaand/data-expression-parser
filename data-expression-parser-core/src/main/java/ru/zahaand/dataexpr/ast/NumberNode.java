package ru.zahaand.dataexpr.ast;

/**
 * AST node representing a numeric literal (e.g. {@code 42}, {@code 3.14}).
 *
 * @param value the numeric value
 */
public record NumberNode(double value) implements Expression {}
