package ru.zahaand.dataexpr.ast;

import java.util.List;

/**
 * AST node representing a function call: {@code name(arg1, arg2, ...)}.
 *
 * <p>The function name is stored as provided by the parser; case-insensitive resolution
 * occurs at evaluation time via {@link ru.zahaand.dataexpr.function.BuiltinFunctionRegistry}
 * and {@link ru.zahaand.dataexpr.function.CustomFunctionRegistry}.
 *
 * @param name the function name as written in the expression
 * @param args the list of argument expressions
 */
public record FunctionCallNode(String name, List<Expression> args) implements Expression {}
