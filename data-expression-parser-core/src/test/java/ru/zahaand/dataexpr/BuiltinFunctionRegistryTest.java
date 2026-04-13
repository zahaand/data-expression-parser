package ru.zahaand.dataexpr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.zahaand.dataexpr.exception.ExpressionEvaluationException;
import ru.zahaand.dataexpr.function.BuiltinFunctionRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class BuiltinFunctionRegistryTest {

    @Nested
    @DisplayName("abs")
    class Abs {
        @Test
        @DisplayName("should return absolute value")
        void shouldReturnAbsoluteValue() {
            assertThat(BuiltinFunctionRegistry.invoke("abs", new double[]{-5.0})).isEqualTo(5.0);
        }
    }

    @Nested
    @DisplayName("round")
    class Round {
        @Test
        @DisplayName("should round to nearest integer")
        void shouldRoundToNearestInteger() {
            assertThat(BuiltinFunctionRegistry.invoke("round", new double[]{2.7})).isEqualTo(3.0);
        }
    }

    @Nested
    @DisplayName("floor")
    class Floor {
        @Test
        @DisplayName("should floor value")
        void shouldFloorValue() {
            assertThat(BuiltinFunctionRegistry.invoke("floor", new double[]{2.7})).isEqualTo(2.0);
        }
    }

    @Nested
    @DisplayName("ceil")
    class Ceil {
        @Test
        @DisplayName("should ceil value")
        void shouldCeilValue() {
            assertThat(BuiltinFunctionRegistry.invoke("ceil", new double[]{2.1})).isEqualTo(3.0);
        }
    }

    @Nested
    @DisplayName("min")
    class Min {
        @Test
        @DisplayName("should return minimum of two values")
        void shouldReturnMinimum() {
            assertThat(BuiltinFunctionRegistry.invoke("min", new double[]{3.0, 5.0})).isEqualTo(3.0);
        }
    }

    @Nested
    @DisplayName("max")
    class Max {
        @Test
        @DisplayName("should return maximum of two values")
        void shouldReturnMaximum() {
            assertThat(BuiltinFunctionRegistry.invoke("max", new double[]{3.0, 5.0})).isEqualTo(5.0);
        }
    }

    @Nested
    @DisplayName("pow")
    class Pow {
        @Test
        @DisplayName("should return power of two values")
        void shouldReturnPower() {
            assertThat(BuiltinFunctionRegistry.invoke("pow", new double[]{2.0, 3.0})).isEqualTo(8.0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABS", "Abs", "abs"})
    @DisplayName("should be case-insensitive for function names")
    void shouldBeCaseInsensitiveForFunctionNames(String name) {
        assertThat(BuiltinFunctionRegistry.invoke(name, new double[]{-5.0})).isEqualTo(5.0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abs", "round", "floor", "ceil", "min", "max", "pow"})
    @DisplayName("should throw when wrong argument count")
    void shouldThrowWhenWrongArgumentCount(String name) {
        assertThatThrownBy(() -> BuiltinFunctionRegistry.invoke(name, new double[]{}))
                .isInstanceOf(ExpressionEvaluationException.class)
                .hasMessageContaining("expects")
                .hasMessageContaining("but got");
    }

    @Test
    @DisplayName("should throw for unknown function")
    void shouldThrowForUnknownFunction() {
        assertThatThrownBy(() -> BuiltinFunctionRegistry.invoke("unknown", new double[]{1.0}))
                .isInstanceOf(ExpressionEvaluationException.class)
                .hasMessageContaining("Unknown function");
    }
}
