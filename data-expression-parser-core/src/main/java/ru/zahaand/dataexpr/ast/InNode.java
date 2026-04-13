package ru.zahaand.dataexpr.ast;

import java.util.List;

public record InNode(Expression field, List<Expression> values, boolean negated) implements Expression {}
