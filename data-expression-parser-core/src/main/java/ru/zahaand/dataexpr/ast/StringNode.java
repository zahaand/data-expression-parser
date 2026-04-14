package ru.zahaand.dataexpr.ast;

/**
 * AST node representing a string literal enclosed in single quotes (e.g. {@code 'active'}).
 *
 * @param value the string value with surrounding quotes stripped
 */
public record StringNode(String value) implements Expression {}
