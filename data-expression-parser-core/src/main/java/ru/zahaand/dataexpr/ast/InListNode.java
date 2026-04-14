package ru.zahaand.dataexpr.ast;

import java.util.List;

public record InListNode(List<Expression> values) implements Expression {}
