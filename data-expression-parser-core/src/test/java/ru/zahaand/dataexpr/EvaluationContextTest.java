package ru.zahaand.dataexpr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.zahaand.dataexpr.evaluator.EvaluationContext;
import ru.zahaand.dataexpr.exception.ExpressionEvaluationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class EvaluationContextTest {

    @Test
    @DisplayName("should return value when field is present")
    void shouldReturnValueWhenFieldIsPresent() {
        EvaluationContext ctx = EvaluationContext.of("age", 25.0);

        assertThat(ctx.get("age")).isEqualTo(25.0);
    }

    @Test
    @DisplayName("should throw when field is absent")
    void shouldThrowWhenFieldIsAbsent() {
        EvaluationContext ctx = EvaluationContext.of("age", 25.0);

        assertThatThrownBy(() -> ctx.get("name"))
                .isInstanceOf(ExpressionEvaluationException.class)
                .hasMessageContaining("name");
    }

    @Test
    @DisplayName("should be case-sensitive for field names")
    void shouldBeCaseSensitiveForFieldNames() {
        EvaluationContext ctx = EvaluationContext.of("Age", 25.0);

        assertThatThrownBy(() -> ctx.get("age"))
                .isInstanceOf(ExpressionEvaluationException.class);
    }

    @Test
    @DisplayName("should create empty context")
    void shouldCreateEmptyContext() {
        EvaluationContext ctx = EvaluationContext.empty();

        assertThatThrownBy(() -> ctx.get("anything"))
                .isInstanceOf(ExpressionEvaluationException.class);
    }
}
