package ru.zahaand.dataexpr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.zahaand.dataexpr.evaluator.ExpressionEvaluator;
import ru.zahaand.dataexpr.exception.ExpressionParseException;
import ru.zahaand.dataexpr.parser.DataExpressionParser;
import ru.zahaand.dataexpr.parser.ValidationResult;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationResultTest {

    private final DataExpressionParser parser = new DataExpressionParser(new ExpressionEvaluator());

    @Test
    @DisplayName("should return valid for correct expression")
    void shouldReturnValidForCorrectExpression() {
        ValidationResult result = parser.validate("[age] > 18 AND [status] == 'active'");

        assertThat(result.isValid()).isTrue();
        assertThat(result.errorMessage()).isEmpty();
    }

    @Test
    @DisplayName("should return invalid for malformed expression")
    void shouldReturnInvalidForMalformedExpression() {
        ValidationResult result = parser.validate("[age] >");

        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).isPresent();
    }

    @Test
    @DisplayName("should contain error message with line and column")
    void shouldContainErrorMessageWithLineAndColumn() {
        ValidationResult result = parser.validate("[age] >");

        assertThat(result.errorMessage().orElseThrow())
                .matches("Parse error at line \\d+:\\d+: .+");
    }

    @Test
    @DisplayName("should throw ParseException for null input")
    void shouldThrowParseExceptionForNullInput() {
        assertThatThrownBy(() -> parser.validate(null))
                .isInstanceOf(ExpressionParseException.class);
    }

    @Test
    @DisplayName("should throw ParseException for blank input")
    void shouldThrowParseExceptionForBlankInput() {
        assertThatThrownBy(() -> parser.validate("   "))
                .isInstanceOf(ExpressionParseException.class);
    }

    @Test
    @DisplayName("should return empty optional when valid")
    void shouldReturnEmptyOptionalWhenValid() {
        assertThat(ValidationResult.valid().errorMessage()).isEmpty();
    }

    @Test
    @DisplayName("should return present optional when invalid")
    void shouldReturnPresentOptionalWhenInvalid() {
        ValidationResult result = ValidationResult.invalid("something broke");

        assertThat(result.errorMessage()).contains("something broke");
    }

    @Test
    @DisplayName("should throw when invalid called with null or blank message")
    void shouldThrowWhenInvalidCalledWithNullOrBlankMessage() {
        assertThatThrownBy(() -> ValidationResult.invalid(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ValidationResult.invalid(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ValidationResult.invalid("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Nested
    class ValidExpressions {

        private final DataExpressionParser parser =
                new DataExpressionParser(new ExpressionEvaluator());

        @ParameterizedTest(name = "validate(\"{0}\") → valid")
        @MethodSource("syntacticallyValidExpressions")
        @DisplayName("should return valid result for syntactically correct expression")
        void shouldReturnValidForCorrectSyntax(String expression) {
            ValidationResult result = parser.validate(expression);
            assertThat(result.isValid()).isTrue();
            assertThat(result.errorMessage()).isEmpty();
        }

        static Stream<Arguments> syntacticallyValidExpressions() {
            return Stream.of(
                    Arguments.of("[age] > 18"),
                    Arguments.of("[age] > 18 AND [status] == 'active'"),
                    Arguments.of("[price] * [qty]"),
                    Arguments.of("abs([x])"),
                    Arguments.of("[s] IN ('a', 'b', 'c')"),
                    Arguments.of("[s] NOT IN ('a', 'b')"),
                    Arguments.of("NOT [flag]"),
                    Arguments.of("2 ^ 3 ^ 2"),
                    Arguments.of("2 ** 3"),
                    Arguments.of("-[value]"),
                    Arguments.of("unknown_func([x])"),
                    Arguments.of("[nonexistent_field] > 0"),
                    Arguments.of("([a] + [b]) * [c]"),
                    Arguments.of("[a] > 1 OR [b] > 1 AND [c] > 1"),
                    Arguments.of("max([a], [b])")
            );
        }
    }

    @Nested
    class InvalidExpressions {

        private final DataExpressionParser parser =
                new DataExpressionParser(new ExpressionEvaluator());

        @ParameterizedTest(name = "validate(\"{0}\") → invalid")
        @MethodSource("syntacticallyInvalidExpressions")
        @DisplayName("should return invalid result with line/column for malformed expression")
        void shouldReturnInvalidForBadSyntax(String expression) {
            ValidationResult result = parser.validate(expression);
            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage()).isPresent();
            assertThat(result.errorMessage().orElseThrow())
                    .matches("Parse error at line \\d+:\\d+: .+");
        }

        static Stream<Arguments> syntacticallyInvalidExpressions() {
            return Stream.of(
                    Arguments.of("[age] >"),
                    Arguments.of("[age] > 18 AND"),
                    Arguments.of("((([age] > 18)"),
                    Arguments.of("[age] > > 18"),
                    Arguments.of("IN ('a', 'b')"),
                    Arguments.of("[age] =="),
                    Arguments.of("+ [age]"),
                    Arguments.of("[age] > 18 OR")
            );
        }

        @ParameterizedTest(name = "validate(\"{0}\") → ExpressionParseException")
        @MethodSource("blankInputs")
        @DisplayName("should throw ExpressionParseException for null or blank input")
        void shouldThrowForBlankInput(String expression) {
            assertThatThrownBy(() -> parser.validate(expression))
                    .isInstanceOf(ExpressionParseException.class);
        }

        static Stream<Arguments> blankInputs() {
            return Stream.of(
                    Arguments.of((Object) null),
                    Arguments.of(""),
                    Arguments.of("   "),
                    Arguments.of("\t"),
                    Arguments.of("\n")
            );
        }
    }
}
