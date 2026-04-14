package ru.zahaand.dataexpr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.zahaand.dataexpr.evaluator.EvaluationContext;
import ru.zahaand.dataexpr.evaluator.ExpressionEvaluator;
import ru.zahaand.dataexpr.exception.ExpressionEvaluationException;
import ru.zahaand.dataexpr.function.CustomFunctionRegistry;
import ru.zahaand.dataexpr.function.ExpressionFunction;
import ru.zahaand.dataexpr.parser.DataExpressionParser;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

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

    @Nested
    class RegistrationParameterized {

        @ParameterizedTest(name = "register(\"{0}\") succeeds")
        @MethodSource("validNames")
        @DisplayName("should register function successfully for valid name")
        void shouldRegisterForValidName(String name) {
            var registry = CustomFunctionRegistry.builder()
                    .register(name, (args, ctx) -> args[0])
                    .build();
            assertThat(registry.find(name.toLowerCase(Locale.ROOT))).isNotNull();
        }

        static Stream<Arguments> validNames() {
            return Stream.of(
                    Arguments.of("MY_FUNC"),
                    Arguments.of("_private"),
                    Arguments.of("fn2"),
                    Arguments.of("myFunc"),
                    Arguments.of("TAX")
            );
        }

        @ParameterizedTest(name = "find(\"{1}\") resolves function registered as \"{0}\"")
        @MethodSource("caseInsensitiveLookups")
        @DisplayName("should find registered function case-insensitively")
        void shouldFindCaseInsensitively(String registeredName, String lookupName) {
            ExpressionFunction fn = (args, ctx) -> args[0];
            var registry = CustomFunctionRegistry.builder()
                    .register(registeredName, fn)
                    .build();
            assertThat(registry.find(lookupName)).isSameAs(fn);
        }

        static Stream<Arguments> caseInsensitiveLookups() {
            return Stream.of(
                    Arguments.of("TAX", "tax"),
                    Arguments.of("TAX", "TAX"),
                    Arguments.of("TAX", "Tax"),
                    Arguments.of("myFunc", "myfunc"),
                    Arguments.of("myFunc", "MYFUNC")
            );
        }

        @ParameterizedTest(name = "register(\"{0}\") throws — invalid name")
        @MethodSource("invalidNames")
        @DisplayName("should throw IllegalArgumentException for invalid name")
        void shouldThrowForInvalidName(String name) {
            assertThatThrownBy(() ->
                    CustomFunctionRegistry.builder().register(name, (args, ctx) -> 0)
            ).isInstanceOf(IllegalArgumentException.class);
        }

        static Stream<Arguments> invalidNames() {
            return Stream.of(
                    Arguments.of((Object) null),
                    Arguments.of(""),
                    Arguments.of("   "),
                    Arguments.of("2pay"),
                    Arguments.of("my-func"),
                    Arguments.of("tax rate"),
                    Arguments.of("fn!"),
                    Arguments.of("123"),
                    Arguments.of("-start")
            );
        }

        @ParameterizedTest(name = "register(\"{0}\") conflicts with built-in")
        @MethodSource("builtinNames")
        @DisplayName("should throw IllegalArgumentException for built-in name conflict")
        void shouldThrowForBuiltinConflict(String name) {
            assertThatThrownBy(() ->
                    CustomFunctionRegistry.builder().register(name, (args, ctx) -> 0)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("conflicts with built-in");
        }

        static Stream<Arguments> builtinNames() {
            return Stream.of(
                    Arguments.of("abs"),   Arguments.of("ABS"),   Arguments.of("Abs"),
                    Arguments.of("round"), Arguments.of("ROUND"),
                    Arguments.of("floor"), Arguments.of("FLOOR"),
                    Arguments.of("ceil"),  Arguments.of("CEIL"),
                    Arguments.of("min"),   Arguments.of("MIN"),
                    Arguments.of("max"),   Arguments.of("MAX"),
                    Arguments.of("pow"),   Arguments.of("POW")
            );
        }

        @ParameterizedTest(name = "register \"{0}\" twice — duplicate detection")
        @MethodSource("duplicateRegistrations")
        @DisplayName("should throw IllegalArgumentException on duplicate registration")
        void shouldThrowOnDuplicate(String first, String second) {
            assertThatThrownBy(() ->
                    CustomFunctionRegistry.builder()
                            .register(first, (args, ctx) -> 0)
                            .register(second, (args, ctx) -> 1)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already registered");
        }

        static Stream<Arguments> duplicateRegistrations() {
            return Stream.of(
                    Arguments.of("TAX", "TAX"),
                    Arguments.of("TAX", "tax"),
                    Arguments.of("tax", "TAX"),
                    Arguments.of("MyFunc", "myfunc")
            );
        }
    }

    @Nested
    class EvaluationParameterized {

        private DataExpressionParser parserWith(CustomFunctionRegistry registry) {
            return new DataExpressionParser(new ExpressionEvaluator(registry), registry);
        }

        @ParameterizedTest(name = "{0} = {2}")
        @MethodSource("customFunctionEvaluations")
        @DisplayName("should evaluate custom function and return correct double")
        void shouldEvaluateCustomFunction(
                String expression,
                Map<String, Object> context,
                double expected) {
            var registry = CustomFunctionRegistry.builder()
                    .register("TAX",    (args, ctx) -> args[0] * 0.15)
                    .register("ROUND2", (args, ctx) -> Math.round(args[0] * 100.0) / 100.0)
                    .register("DISCOUNT", (args, ctx) -> {
                        String tier = (String) ctx.get("customer_tier");
                        return args[0] * ("premium".equals(tier) ? 0.8 : 0.95);
                    })
                    .build();
            double result = parserWith(registry).evaluateDouble(expression, EvaluationContext.of(context));
            assertThat(result).isEqualTo(expected, within(1e-9));
        }

        static Stream<Arguments> customFunctionEvaluations() {
            return Stream.of(
                    Arguments.of("TAX([price])",      Map.of("price", 100.0),  15.0),
                    Arguments.of("TAX([price])",      Map.of("price", 0.0),    0.0),
                    Arguments.of("TAX([price])",      Map.of("price", -100.0), -15.0),
                    Arguments.of("TAX([price])",      Map.of("price", 200.0),  30.0),
                    Arguments.of("ROUND2([x])",       Map.of("x", 1.235),      1.24),
                    Arguments.of("ROUND2([x])",       Map.of("x", 1.0),        1.0),
                    Arguments.of("DISCOUNT([price])", Map.of("price", 100.0, "customer_tier", "premium"),  80.0),
                    Arguments.of("DISCOUNT([price])", Map.of("price", 100.0, "customer_tier", "standard"), 95.0),
                    Arguments.of("DISCOUNT([price])", Map.of("price", 200.0, "customer_tier", "premium"),  160.0)
            );
        }

        @ParameterizedTest(name = "{0} wraps as ExpressionEvaluationException")
        @MethodSource("throwingFunctions")
        @DisplayName("should wrap RuntimeException from custom function into ExpressionEvaluationException")
        void shouldWrapRuntimeException(RuntimeException thrown) {
            var registry = CustomFunctionRegistry.builder()
                    .register("FAILING", (args, ctx) -> { throw thrown; })
                    .build();
            assertThatThrownBy(() ->
                    parserWith(registry).evaluateDouble("FAILING([x])", EvaluationContext.of("x", 1.0))
            ).isInstanceOf(ExpressionEvaluationException.class)
                    .hasMessageContaining("Error in custom function 'FAILING'")
                    .hasCause(thrown);
        }

        static Stream<Arguments> throwingFunctions() {
            return Stream.of(
                    Arguments.of(new NullPointerException("null value")),
                    Arguments.of(new IllegalArgumentException("bad arg")),
                    Arguments.of(new ArithmeticException("divide by zero")),
                    Arguments.of(new IllegalStateException("illegal state"))
            );
        }
    }
}
