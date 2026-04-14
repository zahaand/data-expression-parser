package ru.zahaand.dataexpr.ast;

/**
 * AST node representing an {@code IN} or {@code NOT IN} membership check.
 *
 * <p>The {@code collection} is either:
 * <ul>
 *   <li>{@link InListNode} — a static literal list: {@code [status] IN ('a', 'b')}</li>
 *   <li>{@link FieldNode} — a dynamic context field: {@code [status] IN [allowed_statuses]}</li>
 * </ul>
 *
 * @param operand    the value being tested for membership
 * @param collection the collection to test against (InListNode or FieldNode)
 * @param negated    {@code false} for {@code IN}, {@code true} for {@code NOT IN}
 */
public record InNode(Expression operand, Expression collection, boolean negated) implements Expression {}
