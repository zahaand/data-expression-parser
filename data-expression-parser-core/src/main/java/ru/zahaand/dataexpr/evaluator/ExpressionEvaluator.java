package ru.zahaand.dataexpr.evaluator;

import ru.zahaand.dataexpr.ast.Expression;
import ru.zahaand.dataexpr.visitor.EvaluatingVisitor;

public final class ExpressionEvaluator {

    public EvaluationResult evaluate(Expression expression, EvaluationContext context) {
        return new EvaluatingVisitor(context).evaluate(expression);
    }
}
