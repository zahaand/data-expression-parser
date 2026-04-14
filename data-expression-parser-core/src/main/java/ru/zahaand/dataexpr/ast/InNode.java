package ru.zahaand.dataexpr.ast;

public record InNode(Expression operand, Expression collection, boolean negated) implements Expression {}
