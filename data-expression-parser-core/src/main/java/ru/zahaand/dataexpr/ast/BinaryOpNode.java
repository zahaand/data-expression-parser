package ru.zahaand.dataexpr.ast;

public record BinaryOpNode(Expression left, ArithmeticOperator op, Expression right) implements Expression {}
