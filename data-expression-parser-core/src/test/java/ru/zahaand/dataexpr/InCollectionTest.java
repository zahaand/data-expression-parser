package ru.zahaand.dataexpr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.zahaand.dataexpr.evaluator.EvaluationContext;
import ru.zahaand.dataexpr.evaluator.ExpressionEvaluator;
import ru.zahaand.dataexpr.exception.ExpressionEvaluationException;
import ru.zahaand.dataexpr.parser.DataExpressionParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InCollectionTest {

    private DataExpressionParser parser;

    @BeforeEach
    void setUp() {
        parser = new DataExpressionParser(new ExpressionEvaluator());
    }

    @Nested
    @DisplayName("EvaluateBoolean")
    class EvaluateBoolean {

        @Test
        @DisplayName("should return true when field value is in string collection")
        void shouldReturnTrueWhenFieldValueIsInStringCollection() {
            EvaluationContext ctx = EvaluationContext.of(Map.of(
                    "status", "active",
                    "allowed", List.of("active", "trial")));

            assertThat(parser.evaluateBoolean("[status] IN [allowed]", ctx)).isTrue();
        }

        @Test
        @DisplayName("should return false when field value is not in string collection")
        void shouldReturnFalseWhenFieldValueIsNotInStringCollection() {
            EvaluationContext ctx = EvaluationContext.of(Map.of(
                    "status", "blocked",
                    "allowed", List.of("active", "trial")));

            assertThat(parser.evaluateBoolean("[status] IN [allowed]", ctx)).isFalse();
        }

        @Test
        @DisplayName("should return true when field value is in numeric collection")
        void shouldReturnTrueWhenFieldValueIsInNumericCollection() {
            EvaluationContext ctx = EvaluationContext.of(Map.of(
                    "code", 2.0,
                    "valid_codes", List.of(1.0, 2.0, 3.0)));

            assertThat(parser.evaluateBoolean("[code] IN [valid_codes]", ctx)).isTrue();
        }

        @Test
        @DisplayName("should return false when field value is not in numeric collection")
        void shouldReturnFalseWhenFieldValueIsNotInNumericCollection() {
            EvaluationContext ctx = EvaluationContext.of(Map.of(
                    "code", 7.0,
                    "valid_codes", List.of(1.0, 2.0, 3.0)));

            assertThat(parser.evaluateBoolean("[code] IN [valid_codes]", ctx)).isFalse();
        }

        @Test
        @DisplayName("should return true for NOT IN when value absent")
        void shouldReturnTrueForNotInWhenValueAbsent() {
            EvaluationContext ctx = EvaluationContext.of(Map.of(
                    "status", "blocked",
                    "allowed", List.of("active", "trial")));

            assertThat(parser.evaluateBoolean("[status] NOT IN [allowed]", ctx)).isTrue();
        }

        @Test
        @DisplayName("should return false for NOT IN when value present")
        void shouldReturnFalseForNotInWhenValuePresent() {
            EvaluationContext ctx = EvaluationContext.of(Map.of(
                    "status", "active",
                    "allowed", List.of("active", "trial")));

            assertThat(parser.evaluateBoolean("[status] NOT IN [allowed]", ctx)).isFalse();
        }

        @Test
        @DisplayName("should handle mixed type collection with no match")
        void shouldHandleMixedTypeCollectionWithNoMatch() {
            EvaluationContext ctx = EvaluationContext.of(Map.of(
                    "value", "not-there",
                    "items", List.of("active", 1.0, true)));

            assertThat(parser.evaluateBoolean("[value] IN [items]", ctx)).isFalse();
        }

        @Test
        @DisplayName("should return false when collection is empty")
        void shouldReturnFalseWhenCollectionIsEmpty() {
            EvaluationContext ctx = EvaluationContext.of(Map.of(
                    "status", "active",
                    "allowed", List.of()));

            assertThat(parser.evaluateBoolean("[status] IN [allowed]", ctx)).isFalse();
        }
    }

    @Nested
    @DisplayName("EvaluateBooleanParameterized")
    class EvaluateBooleanParameterized {

        @ParameterizedTest(name = "{0}")
        @MethodSource("inCollectionExpressions")
        @DisplayName("should evaluate IN collection expression correctly")
        void shouldEvaluateInCollectionExpression(String expression, Map<String, Object> context, boolean expected) {
            assertThat(parser.evaluateBoolean(expression, EvaluationContext.of(context)))
                    .isEqualTo(expected);
        }

        static Stream<Arguments> inCollectionExpressions() {
            return Stream.of(
                    Arguments.of("[s] IN [allowed]",
                            Map.of("s", "a", "allowed", List.of("a", "b")), true),
                    Arguments.of("[s] IN [allowed]",
                            Map.of("s", "x", "allowed", List.of("a", "b")), false),
                    Arguments.of("[s] NOT IN [allowed]",
                            Map.of("s", "x", "allowed", List.of("a", "b")), true),
                    Arguments.of("[s] NOT IN [allowed]",
                            Map.of("s", "a", "allowed", List.of("a", "b")), false),
                    Arguments.of("[n] IN [nums]",
                            Map.of("n", 2.0, "nums", List.of(1.0, 2.0, 3.0)), true),
                    Arguments.of("[n] IN [nums]",
                            Map.of("n", 9.0, "nums", List.of(1.0, 2.0, 3.0)), false),
                    Arguments.of("[n] NOT IN [nums]",
                            Map.of("n", 9.0, "nums", List.of(1.0, 2.0, 3.0)), true),
                    Arguments.of("[n] NOT IN [nums]",
                            Map.of("n", 2.0, "nums", List.of(1.0, 2.0, 3.0)), false)
            );
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("should throw when collection field is not a list")
        void shouldThrowWhenCollectionFieldIsNotAList() {
            EvaluationContext ctx = EvaluationContext.of(Map.of(
                    "status", "active",
                    "allowed", "not-a-list"));

            assertThatThrownBy(() -> parser.evaluateBoolean("[status] IN [allowed]", ctx))
                    .isInstanceOf(ExpressionEvaluationException.class)
                    .hasMessageContaining("must be a List for IN operator, got:");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("unsupportedElements")
        @DisplayName("should throw when collection contains unsupported element type")
        void shouldThrowWhenCollectionContainsUnsupportedElementType(String caseName, List<Object> items) {
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("status", "active");
            data.put("allowed", items);
            EvaluationContext ctx = EvaluationContext.of(data);

            assertThatThrownBy(() -> parser.evaluateBoolean("[status] IN [allowed]", ctx))
                    .isInstanceOf(ExpressionEvaluationException.class)
                    .hasMessageContaining("contains unsupported element type:");
        }

        static Stream<Arguments> unsupportedElements() {
            List<Object> withNull = new ArrayList<>();
            withNull.add("active");
            withNull.add(null);
            return Stream.of(
                    Arguments.of("non-scalar object", Arrays.<Object>asList("active", new Object())),
                    Arguments.of("null element", withNull)
            );
        }

        @Test
        @DisplayName("should throw when collection field does not exist")
        void shouldThrowWhenCollectionFieldDoesNotExist() {
            EvaluationContext ctx = EvaluationContext.of("status", "active");

            assertThatThrownBy(() -> parser.evaluateBoolean("[status] IN [missing]", ctx))
                    .isInstanceOf(ExpressionEvaluationException.class)
                    .hasMessageContaining("not found in context");
        }
    }
}
