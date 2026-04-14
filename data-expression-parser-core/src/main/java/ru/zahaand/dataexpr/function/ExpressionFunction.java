package ru.zahaand.dataexpr.function;

import ru.zahaand.dataexpr.evaluator.EvaluationContext;

/**
 * A consumer-defined function that can be registered in a {@link CustomFunctionRegistry}
 * and invoked from expressions by name.
 *
 * <p>Functions receive pre-evaluated numeric arguments and the active {@link EvaluationContext},
 * allowing context-aware business logic:
 * <pre>{@code
 * CustomFunctionRegistry.builder()
 *     .register("DISCOUNT", (args, ctx) -> {
 *         String tier = (String) ctx.get("customer_tier");
 *         return args[0] * ("premium".equals(tier) ? 0.8 : 0.95);
 *     })
 *     .build();
 * }</pre>
 *
 * <p>Custom functions always return a {@code double}. Functions that produce boolean or string
 * results are not supported in v1.x.
 *
 * <p>Argument count is not declared at registration. The function is responsible for validating
 * {@code args.length} and throwing {@link IllegalArgumentException} if the count is incorrect.
 * Any {@link RuntimeException} thrown by this function is caught by the evaluator and wrapped
 * in {@link ru.zahaand.dataexpr.exception.ExpressionEvaluationException}.
 */
@FunctionalInterface
public interface ExpressionFunction {

    /**
     * Applies this function to the given arguments and evaluation context.
     *
     * @param args    the pre-evaluated numeric arguments; never {@code null}
     * @param context the active evaluation context; never {@code null}
     * @return the numeric result
     */
    double apply(double[] args, EvaluationContext context);
}
