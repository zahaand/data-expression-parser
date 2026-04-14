package ru.zahaand.dataexpr.ast;

public sealed interface Expression
        permits FieldNode, NumberNode, StringNode, BooleanNode,
                BinaryOpNode, UnaryMinusNode, FunctionCallNode,
                ComparisonNode, LogicalNode, NotNode, InNode, InListNode {}
