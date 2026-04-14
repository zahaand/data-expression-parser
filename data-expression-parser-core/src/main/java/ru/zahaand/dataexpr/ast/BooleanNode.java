package ru.zahaand.dataexpr.ast;

/**
 * AST node representing a boolean literal ({@code true} or {@code false}, case-insensitive).
 *
 * @param value the boolean value
 */
public record BooleanNode(boolean value) implements Expression {}
