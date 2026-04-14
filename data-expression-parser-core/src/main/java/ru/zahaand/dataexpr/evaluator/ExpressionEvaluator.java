package ru.zahaand.dataexpr.evaluator;

import ru.zahaand.dataexpr.ast.Expression;
import ru.zahaand.dataexpr.function.CustomFunctionRegistry;
import ru.zahaand.dataexpr.visitor.EvaluatingVisitor;

public final class ExpressionEvaluator {

    private final CustomFunctionRegistry customFunctionRegistry;

    public ExpressionEvaluator() {
        this(CustomFunctionRegistry.empty());
    }

    public ExpressionEvaluator(CustomFunctionRegistry customFunctionRegistry) {
        this.customFunctionRegistry = customFunctionRegistry;
    }

    public EvaluationResult evaluate(Expression expression, EvaluationContext context) {
        return new EvaluatingVisitor(context, customFunctionRegistry).evaluate(expression);
    }
}
