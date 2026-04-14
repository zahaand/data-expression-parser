package ru.zahaand.dataexpr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.zahaand.dataexpr.evaluator.EvaluationContext;
import ru.zahaand.dataexpr.evaluator.ExpressionEvaluator;
import ru.zahaand.dataexpr.exception.ExpressionEvaluationException;
import ru.zahaand.dataexpr.function.CustomFunctionRegistry;
import ru.zahaand.dataexpr.function.ExpressionFunction;
import ru.zahaand.dataexpr.parser.DataExpressionParser;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomFunctionRegistryTest {

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("should register custom function successfully")
        void shouldRegisterCustomFunctionSuccessfully() {
            CustomFunctionRegistry registry = CustomFunctionRegistry.builder()
                    .register("tax", (args, ctx) -> args[0] * 0.15)
                    .build();

            assertThat(registry.find("tax")).isNotNull();
        }

        @Test
        @DisplayName("should find registered function case-insensitively")
        void shouldFindRegisteredFunctionCaseInsensitively() {
            CustomFunctionRegistry registry = CustomFunctionRegistry.builder()
                    .register("TAX", (args, ctx) -> args[0] * 0.15)
                    .build();

            assertThat(registry.find("tax")).isNotNull();
            assertThat(registry.find("TAX")).isNotNull();
            assertThat(registry.find("Tax")).isNotNull();
        }

        @Test
        @DisplayName("should return null when function not found")
        void shouldReturnNullWhenFunctionNotFound() {
            CustomFunctionRegistry registry = CustomFunctionRegistry.empty();

            assertThat(registry.find("nope")).isNull();
        }

        @Test
        @DisplayName("should throw when registering null name")
        void shouldThrowWhenRegisteringNullName() {
            assertThatThrownBy(() -> CustomFunctionRegistry.builder()
                    .register(null, (args, ctx) -> 0.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("should throw when registering blank name")
        void shouldThrowWhenRegisteringBlankName() {
            assertThatThrownBy(() -> CustomFunctionRegistry.builder()
                    .register("   ", (args, ctx) -> 0.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @ParameterizedTest
        @ValueSource(strings = {"2pay", "my-func", "tax rate", "fn!"})
        @DisplayName("should throw when name does not match grammar-id pattern")
        void shouldThrowWhenNameDoesNotMatchGrammarIdPattern(String name) {
            assertThatThrownBy(() -> CustomFunctionRegistry.builder()
                    .register(name, (args, ctx) -> 0.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a valid identifier");
        }

        @ParameterizedTest
        @ValueSource(strings = {"abs", "ABS", "Abs", "round", "floor", "ceil", "min", "max", "pow"})
        @DisplayName("should throw when name conflicts with built-in")
        void shouldThrowWhenNameConflictsWithBuiltin(String name) {
            assertThatThrownBy(() -> CustomFunctionRegistry.builder()
                    .register(name, (args, ctx) -> 0.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("conflicts with built-in");
        }

        @Test
        @DisplayName("should throw when same name registered twice")
        void shouldThrowWhenSameNameRegisteredTwice() {
            CustomFunctionRegistry.Builder builder = CustomFunctionRegistry.builder()
                    .register("TAX", (args, ctx) -> args[0]);

            assertThatThrownBy(() -> builder.register("tax", (args, ctx) -> args[0]))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already registered");
        }

        @Test
        @DisplayName("should throw when function is null")
        void shouldThrowWhenFunctionIsNull() {
            assertThatThrownBy(() -> CustomFunctionRegistry.builder()
                    .register("myfn", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Function must not be null");
        }

        @Test
        @DisplayName("should create empty registry")
        void shouldCreateEmptyRegistry() {
            CustomFunctionRegistry registry = CustomFunctionRegistry.empty();

            assertThat(registry.find("anything")).isNull();
        }
    }

    @Nested
    @DisplayName("Evaluation")
    class Evaluation {

        private DataExpressionParser parserWith(CustomFunctionRegistry registry) {
            return new DataExpressionParser(new ExpressionEvaluator(registry), registry);
        }

        @Test
        @DisplayName("should evaluate custom function with args")
        void shouldEvaluateCustomFunctionWithArgs() {
            CustomFunctionRegistry registry = CustomFunctionRegistry.builder()
                    .register("TAX", (args, ctx) -> args[0] * 0.15)
                    .build();
            DataExpressionParser parser = parserWith(registry);

            double result = parser.evaluateDouble("TAX([price])",
                    EvaluationContext.of("price", 100.0));

            assertThat(result).isEqualTo(15.0);
        }

        @Test
        @DisplayName("should evaluate custom function with context access")
        void shouldEvaluateCustomFunctionWithContextAccess() {
            ExpressionFunction discount = (args, ctx) -> {
                Object tier = ctx.get("customer_tier");
                double rate = "premium".equals(tier) ? 0.8 : 0.95;
                return args[0] * rate;
            };
            CustomFunctionRegistry registry = CustomFunctionRegistry.builder()
                    .register("DISCOUNT", discount)
                    .build();
            DataExpressionParser parser = parserWith(registry);

            double result = parser.evaluateDouble("DISCOUNT([price])",
                    EvaluationContext.of(Map.of("price", 100.0, "customer_tier", "premium")));

            assertThat(result).isEqualTo(80.0);
        }

        @Test
        @DisplayName("should fall back to builtin when custom not found")
        void shouldFallBackToBuiltinWhenCustomNotFound() {
            CustomFunctionRegistry registry = CustomFunctionRegistry.builder()
                    .register("TAX", (args, ctx) -> args[0] * 0.15)
                    .build();
            DataExpressionParser parser = parserWith(registry);

            double result = parser.evaluateDouble("abs(-5)", EvaluationContext.empty());

            assertThat(result).isEqualTo(5.0);
        }

        @Test
        @DisplayName("should throw evaluation exception when custom function throws")
        void shouldThrowEvaluationExceptionWhenCustomFunctionThrows() {
            RuntimeException boom = new RuntimeException("boom");
            CustomFunctionRegistry registry = CustomFunctionRegistry.builder()
                    .register("BROKEN", (args, ctx) -> { throw boom; })
                    .build();
            DataExpressionParser parser = parserWith(registry);

            assertThatThrownBy(() -> parser.evaluateDouble("BROKEN(1)", EvaluationContext.empty()))
                    .isInstanceOf(ExpressionEvaluationException.class)
                    .hasMessageContaining("Error in custom function 'BROKEN': boom")
                    .hasCause(boom);
        }

        @Test
        @DisplayName("should allow custom function to validate own arity")
        void shouldAllowCustomFunctionToValidateOwnArity() {
            CustomFunctionRegistry registry = CustomFunctionRegistry.builder()
                    .register("STRICT", (args, ctx) -> {
                        if (args.length != 2) {
                            throw new IllegalArgumentException("Expected 2 args, got " + args.length);
                        }
                        return args[0] + args[1];
                    })
                    .build();
            DataExpressionParser parser = parserWith(registry);

            assertThatThrownBy(() -> parser.evaluateDouble("STRICT(1)", EvaluationContext.empty()))
                    .isInstanceOf(ExpressionEvaluationException.class)
                    .hasMessageContaining("Error in custom function 'STRICT'")
                    .hasCauseInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw when function not found in either registry")
        void shouldThrowWhenFunctionNotFoundInEitherRegistry() {
            DataExpressionParser parser = parserWith(CustomFunctionRegistry.empty());

            assertThatThrownBy(() -> parser.evaluateDouble("missing(1)", EvaluationContext.empty()))
                    .isInstanceOf(ExpressionEvaluationException.class)
                    .hasMessageContaining("Unknown function: 'missing'");
        }
    }
}
