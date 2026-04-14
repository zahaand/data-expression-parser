package ru.zahaand.dataexpr.ast;

import java.util.List;

public record FunctionCallNode(String name, List<Expression> args) implements Expression {}
