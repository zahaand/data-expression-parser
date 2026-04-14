package ru.zahaand.dataexpr.function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zahaand.dataexpr.exception.ExpressionEvaluationException;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class BuiltinFunctionRegistry {

    private static final Logger log = LoggerFactory.getLogger(BuiltinFunctionRegistry.class);

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

    static final Set<String> BUILTIN_NAMES = Set.of("abs", "round", "floor", "ceil", "min", "max", "pow");

    public static double invoke(String name, double[] args) {
        FunctionDef def = FUNCTIONS.get(name.toLowerCase(Locale.ROOT));
        if (def == null) {
            log.error("Unknown function called: '{}'", name);
            throw new ExpressionEvaluationException("Unknown function: '" + name + "'");
        }
        if (args.length != def.arity()) {
            log.error("Function '{}' called with wrong argument count: expected {}, got {}", name, def.arity(), args.length);
            throw new ExpressionEvaluationException(
                    "Function '" + name + "' expects " + def.arity()
                            + " argument(s) but got " + args.length);
        }
        return def.impl().apply(args);
    }
}
