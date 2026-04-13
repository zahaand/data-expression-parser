package ru.zahaand.dataexpr.function;

import ru.zahaand.dataexpr.exception.ExpressionEvaluationException;

import java.util.Map;
import java.util.function.Function;

public final class BuiltinFunctionRegistry {

    private BuiltinFunctionRegistry() {
    }

    private record FunctionDef(int arity, Function<double[], Double> impl) {}

    private static final Map<String, FunctionDef> FUNCTIONS = Map.of(
            "abs", new FunctionDef(1, args -> Math.abs(args[0])),
            "round", new FunctionDef(1, args -> (double) Math.round(args[0])),
            "floor", new FunctionDef(1, args -> Math.floor(args[0])),
            "ceil", new FunctionDef(1, args -> Math.ceil(args[0])),
            "min", new FunctionDef(2, args -> Math.min(args[0], args[1])),
            "max", new FunctionDef(2, args -> Math.max(args[0], args[1])),
            "pow", new FunctionDef(2, args -> Math.pow(args[0], args[1]))
    );

    public static double invoke(String name, double[] args) {
        FunctionDef def = FUNCTIONS.get(name.toLowerCase());
        if (def == null) {
            throw new ExpressionEvaluationException("Unknown function: '" + name + "'");
        }
        if (args.length != def.arity()) {
            throw new ExpressionEvaluationException(
                    "Function '" + name + "' expects " + def.arity()
                            + " argument(s) but got " + args.length);
        }
        return def.impl().apply(args);
    }
}
