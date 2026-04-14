package ru.zahaand.dataexpr.function;

import ru.zahaand.dataexpr.evaluator.EvaluationContext;

@FunctionalInterface
public interface ExpressionFunction {

    double apply(double[] args, EvaluationContext context);
}
