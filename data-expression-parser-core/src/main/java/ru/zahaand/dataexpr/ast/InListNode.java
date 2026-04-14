package ru.zahaand.dataexpr.ast;

import java.util.List;

/**
 * AST node representing a static literal list used as the right-hand side of {@code IN}.
 *
 * <p>Example: the {@code ('active', 'trial')} part of {@code [status] IN ('active', 'trial')}.
 *
 * @param values the list of literal expressions
 */
public record InListNode(List<Expression> values) implements Expression {}
