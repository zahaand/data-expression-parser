package ru.zahaand.dataexpr.ast;

public record LogicalNode(Expression left, LogicalOperator op, Expression right) implements Expression {}
