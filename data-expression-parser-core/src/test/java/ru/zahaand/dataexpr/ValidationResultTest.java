package ru.zahaand.dataexpr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.zahaand.dataexpr.evaluator.ExpressionEvaluator;
import ru.zahaand.dataexpr.exception.ExpressionParseException;
import ru.zahaand.dataexpr.parser.DataExpressionParser;
import ru.zahaand.dataexpr.parser.ValidationResult;

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
}
