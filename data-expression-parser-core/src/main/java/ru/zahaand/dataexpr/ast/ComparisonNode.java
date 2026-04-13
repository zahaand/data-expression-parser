package ru.zahaand.dataexpr.ast;

public record ComparisonNode(Expression left, ComparisonOperator op, Expression right) implements Expression {}
