package ru.zahaand.dataexpr.ast;

/**
 * AST node representing a field reference: {@code [field_name]}.
 *
 * @param fieldName the field name with surrounding brackets stripped (e.g. {@code "age"} for {@code [age]})
 */
public record FieldNode(String fieldName) implements Expression {}
