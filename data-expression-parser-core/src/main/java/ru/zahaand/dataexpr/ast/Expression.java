package ru.zahaand.dataexpr.ast;

/**
 * Root of the Abstract Syntax Tree (AST) node hierarchy.
 *
 * <p>Every node in a parsed expression tree implements this sealed interface.
 * Use pattern matching on the permitted subtypes to traverse or inspect the tree:
 * <pre>{@code
 * Expression ast = parser.parse("[age] > 18");
 * if (ast instanceof ComparisonNode c) {
 *     // inspect c.left(), c.op(), c.right()
 * }
 * }</pre>
 */
public sealed interface Expression
        permits FieldNode, NumberNode, StringNode, BooleanNode,
                BinaryOpNode, UnaryMinusNode, FunctionCallNode,
                ComparisonNode, LogicalNode, NotNode, InNode, InListNode {}
