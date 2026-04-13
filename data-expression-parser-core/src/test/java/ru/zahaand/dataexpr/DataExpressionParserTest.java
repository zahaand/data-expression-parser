package ru.zahaand.dataexpr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.zahaand.dataexpr.ast.*;
import ru.zahaand.dataexpr.evaluator.BooleanResult;
import ru.zahaand.dataexpr.evaluator.DoubleResult;
import ru.zahaand.dataexpr.evaluator.EvaluationContext;
import ru.zahaand.dataexpr.evaluator.EvaluationResult;
import ru.zahaand.dataexpr.evaluator.ExpressionEvaluator;
import ru.zahaand.dataexpr.exception.ExpressionEvaluationException;
import ru.zahaand.dataexpr.exception.ExpressionParseException;
import ru.zahaand.dataexpr.parser.DataExpressionParser;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
class DataExpressionParserTest {

    private DataExpressionParser parser;

    @BeforeEach
    void setUp() {
        parser = new DataExpressionParser(new ExpressionEvaluator());
    }

    @Nested
    @DisplayName("Parse")
    class Parse {

        @Test
        @DisplayName("should return FieldNode when expression is field reference")
        void shouldReturnFieldNodeWhenExpressionIsFieldReference() {
            Expression result = parser.parse("[age]");

            assertThat(result).isInstanceOf(FieldNode.class);
            assertThat(((FieldNode) result).fieldName()).isEqualTo("age");
        }

        @Test
        @DisplayName("should return NumberNode when expression is numeric literal")
        void shouldReturnNumberNodeWhenExpressionIsNumericLiteral() {
            Expression result = parser.parse("42.5");

            assertThat(result).isInstanceOf(NumberNode.class);
            assertThat(((NumberNode) result).value()).isEqualTo(42.5);
        }

        @Test
        @DisplayName("should return StringNode when expression is string literal")
        void shouldReturnStringNodeWhenExpressionIsStringLiteral() {
            Expression result = parser.parse("'active'");

            assertThat(result).isInstanceOf(StringNode.class);
            assertThat(((StringNode) result).value()).isEqualTo("active");
        }

        @Test
        @DisplayName("should return BooleanNode when expression is true literal")
        void shouldReturnBooleanNodeWhenExpressionIsTrueLiteral() {
            Expression result = parser.parse("true");

            assertThat(result).isInstanceOf(BooleanNode.class);
            assertThat(((BooleanNode) result).value()).isTrue();
        }

        @Test
        @DisplayName("should return BooleanNode when expression is false literal")
        void shouldReturnBooleanNodeWhenExpressionIsFalseLiteral() {
            Expression result = parser.parse("false");

            assertThat(result).isInstanceOf(BooleanNode.class);
            assertThat(((BooleanNode) result).value()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"TRUE", "True", "true", "FALSE", "False", "false"})
        @DisplayName("should parse true and false case-insensitively")
        void shouldParseTrueAndFalseCaseInsensitively(String input) {
            Expression result = parser.parse(input);

            assertThat(result).isInstanceOf(BooleanNode.class);
        }

        @Test
        @DisplayName("should return BinaryOpNode for arithmetic")
        void shouldReturnBinaryOpNodeForArithmetic() {
            Expression result = parser.parse("[price] * 1.2");

            assertThat(result).isInstanceOf(BinaryOpNode.class);
            BinaryOpNode node = (BinaryOpNode) result;
            assertThat(node.op()).isEqualTo(ArithmeticOperator.MULTIPLY);
            assertThat(node.left()).isInstanceOf(FieldNode.class);
            assertThat(node.right()).isInstanceOf(NumberNode.class);
        }

        @Test
        @DisplayName("should respect arithmetic precedence")
        void shouldRespectArithmeticPrecedence() {
            // [a] + [b] * [c] should be parsed as [a] + ([b] * [c])
            Expression result = parser.parse("[a] + [b] * [c]");

            assertThat(result).isInstanceOf(BinaryOpNode.class);
            BinaryOpNode node = (BinaryOpNode) result;
            assertThat(node.op()).isEqualTo(ArithmeticOperator.ADD);
            assertThat(node.left()).isInstanceOf(FieldNode.class);
            assertThat(node.right()).isInstanceOf(BinaryOpNode.class);
            BinaryOpNode right = (BinaryOpNode) node.right();
            assertThat(right.op()).isEqualTo(ArithmeticOperator.MULTIPLY);
        }

        @Test
        @DisplayName("should return ComparisonNode for greater than")
        void shouldReturnComparisonNodeForGreaterThan() {
            Expression result = parser.parse("[age] > 18");

            assertThat(result).isInstanceOf(ComparisonNode.class);
            ComparisonNode node = (ComparisonNode) result;
            assertThat(node.op()).isEqualTo(ComparisonOperator.GT);
            assertThat(node.left()).isInstanceOf(FieldNode.class);
            assertThat(node.right()).isInstanceOf(NumberNode.class);
        }

        @Test
        @DisplayName("should return LogicalNode for AND expression")
        void shouldReturnLogicalNodeForAndExpression() {
            Expression result = parser.parse("[age] > 18 AND [status] == 'active'");

            assertThat(result).isInstanceOf(LogicalNode.class);
            LogicalNode node = (LogicalNode) result;
            assertThat(node.op()).isEqualTo(LogicalOperator.AND);
            assertThat(node.left()).isInstanceOf(ComparisonNode.class);
            assertThat(node.right()).isInstanceOf(ComparisonNode.class);
        }

        @Test
        @DisplayName("should return NotNode for NOT expression")
        void shouldReturnNotNodeForNotExpression() {
            Expression result = parser.parse("NOT [disabled]");

            assertThat(result).isInstanceOf(NotNode.class);
            NotNode node = (NotNode) result;
            assertThat(node.operand()).isInstanceOf(FieldNode.class);
        }

        @Test
        @DisplayName("should return InNode for IN expression")
        void shouldReturnInNodeForInExpression() {
            Expression result = parser.parse("[role] IN ('admin', 'moderator')");

            assertThat(result).isInstanceOf(InNode.class);
            InNode node = (InNode) result;
            assertThat(node.negated()).isFalse();
            assertThat(node.field()).isInstanceOf(FieldNode.class);
            assertThat(node.values()).hasSize(2);
        }

        @Test
        @DisplayName("should return InNode with negated true for NOT IN")
        void shouldReturnInNodeWithNegatedTrueForNotIn() {
            Expression result = parser.parse("[role] NOT IN ('admin', 'moderator')");

            assertThat(result).isInstanceOf(InNode.class);
            InNode node = (InNode) result;
            assertThat(node.negated()).isTrue();
        }

        @Test
        @DisplayName("should handle field names with spaces")
        void shouldHandleFieldNamesWithSpaces() {
            Expression result = parser.parse("[first name]");

            assertThat(result).isInstanceOf(FieldNode.class);
            assertThat(((FieldNode) result).fieldName()).isEqualTo("first name");
        }

        @Test
        @DisplayName("should handle right-associativity for power")
        void shouldHandleRightAssociativityForPower() {
            // 2 ^ 3 ^ 2 should be parsed as 2 ^ (3 ^ 2)
            Expression result = parser.parse("2 ^ 3 ^ 2");

            assertThat(result).isInstanceOf(BinaryOpNode.class);
            BinaryOpNode outer = (BinaryOpNode) result;
            assertThat(outer.op()).isEqualTo(ArithmeticOperator.POWER);
            assertThat(outer.left()).isInstanceOf(NumberNode.class);
            assertThat(((NumberNode) outer.left()).value()).isEqualTo(2.0);
            assertThat(outer.right()).isInstanceOf(BinaryOpNode.class);
            BinaryOpNode inner = (BinaryOpNode) outer.right();
            assertThat(inner.op()).isEqualTo(ArithmeticOperator.POWER);
            assertThat(((NumberNode) inner.left()).value()).isEqualTo(3.0);
            assertThat(((NumberNode) inner.right()).value()).isEqualTo(2.0);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("should throw ParseException when expression is malformed")
        void shouldThrowParseExceptionWhenExpressionIsMalformed() {
            assertThatThrownBy(() -> parser.parse("[age] >"))
                    .isInstanceOf(ExpressionParseException.class)
                    .hasMessageContaining("Parse error at line");
        }

        @Test
        @DisplayName("should throw ParseException when input is null")
        void shouldThrowParseExceptionWhenInputIsNull() {
            assertThatThrownBy(() -> parser.parse(null))
                    .isInstanceOf(ExpressionParseException.class)
                    .hasMessageContaining("null");
        }

        @Test
        @DisplayName("should throw ParseException when input is blank")
        void shouldThrowParseExceptionWhenInputIsBlank() {
            assertThatThrownBy(() -> parser.parse("   "))
                    .isInstanceOf(ExpressionParseException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("should throw EvaluationException when field not in context")
        void shouldThrowEvaluationExceptionWhenFieldNotInContext() {
            EvaluationContext ctx = EvaluationContext.empty();

            assertThatThrownBy(() -> parser.evaluateBoolean("[age] > 18", ctx))
                    .isInstanceOf(ExpressionEvaluationException.class)
                    .hasMessageContaining("age");
        }

        @Test
        @DisplayName("should throw EvaluationException when division by zero")
        void shouldThrowEvaluationExceptionWhenDivisionByZero() {
            EvaluationContext ctx = EvaluationContext.of("x", 10.0);

            assertThatThrownBy(() -> parser.evaluateDouble("[x] / 0", ctx))
                    .isInstanceOf(ExpressionEvaluationException.class)
                    .hasMessageContaining("Division by zero");
        }

        @Test
        @DisplayName("should throw EvaluationException when unknown function")
        void shouldThrowEvaluationExceptionWhenUnknownFunction() {
            EvaluationContext ctx = EvaluationContext.of("x", 10.0);

            assertThatThrownBy(() -> parser.evaluateDouble("unknown([x])", ctx))
                    .isInstanceOf(ExpressionEvaluationException.class)
                    .hasMessageContaining("Unknown function");
        }

        @Test
        @DisplayName("should throw EvaluationException when arithmetic on string field")
        void shouldThrowEvaluationExceptionWhenArithmeticOnStringField() {
            EvaluationContext ctx = EvaluationContext.of("name", "Alice");

            assertThatThrownBy(() -> parser.evaluateDouble("[name] + 1", ctx))
                    .isInstanceOf(ExpressionEvaluationException.class)
                    .hasMessageContaining("string");
        }

        @Test
        @DisplayName("should throw EvaluationException when evaluateBoolean called on double result")
        void shouldThrowEvaluationExceptionWhenEvaluateBooleanCalledOnDoubleResult() {
            EvaluationContext ctx = EvaluationContext.of("x", 10.0);

            assertThatThrownBy(() -> parser.evaluateBoolean("[x] + 1", ctx))
                    .isInstanceOf(ExpressionEvaluationException.class)
                    .hasMessageContaining("boolean");
        }

        @Test
        @DisplayName("should throw EvaluationException when ordering operator applied to string operands")
        void shouldThrowEvaluationExceptionWhenOrderingOperatorAppliedToStringOperands() {
            EvaluationContext ctx = EvaluationContext.of("name", "Bob");

            assertThatThrownBy(() -> parser.evaluateBoolean("[name] > 'Alice'", ctx))
                    .isInstanceOf(ExpressionEvaluationException.class)
                    .hasMessageContaining("numeric");
        }

        @Test
        @DisplayName("should throw EvaluationException when boolean used in arithmetic")
        void shouldThrowEvaluationExceptionWhenBooleanUsedInArithmetic() {
            EvaluationContext ctx = EvaluationContext.of("flag", true);

            assertThatThrownBy(() -> parser.evaluateDouble("[flag] + 1", ctx))
                    .isInstanceOf(ExpressionEvaluationException.class)
                    .hasMessageContaining("boolean");
        }

        @Test
        @DisplayName("should throw EvaluationException when null value in context")
        void shouldThrowEvaluationExceptionWhenNullValueInContext() {
            assertThatThrownBy(() -> EvaluationContext.of("age", null))
                    .isInstanceOf(ExpressionEvaluationException.class)
                    .hasMessageContaining("null");
        }

        @Test
        @DisplayName("should throw EvaluationException when evaluateDouble called on boolean result")
        void shouldThrowEvaluationExceptionWhenEvaluateDoubleCalledOnBooleanResult() {
            EvaluationContext ctx = EvaluationContext.of("x", 10.0);

            assertThatThrownBy(() -> parser.evaluateDouble("[x] > 5", ctx))
                    .isInstanceOf(ExpressionEvaluationException.class)
                    .hasMessageContaining("double");
        }
    }

    @Nested
    @DisplayName("EvaluateBoolean")
    class EvaluateBoolean {

        @Test
        @DisplayName("should return true when field greater than literal")
        void shouldReturnTrueWhenFieldGreaterThanLiteral() {
            EvaluationContext ctx = EvaluationContext.of("age", 25.0);

            assertThat(parser.evaluateBoolean("[age] > 18", ctx)).isTrue();
        }

        @Test
        @DisplayName("should return false when field not matching string")
        void shouldReturnFalseWhenFieldNotMatchingString() {
            EvaluationContext ctx = EvaluationContext.of("status", "inactive");

            assertThat(parser.evaluateBoolean("[status] == 'active'", ctx)).isFalse();
        }

        @Test
        @DisplayName("should evaluate AND expression")
        void shouldEvaluateAndExpression() {
            EvaluationContext ctx = EvaluationContext.of(Map.of("age", 25.0, "status", "active"));

            assertThat(parser.evaluateBoolean("[age] > 18 AND [status] == 'active'", ctx)).isTrue();
        }

        @Test
        @DisplayName("should evaluate OR expression")
        void shouldEvaluateOrExpression() {
            EvaluationContext ctx = EvaluationContext.of(Map.of("age", 15.0, "status", "active"));

            assertThat(parser.evaluateBoolean("[age] > 18 OR [status] == 'active'", ctx)).isTrue();
        }

        @Test
        @DisplayName("should evaluate NOT expression")
        void shouldEvaluateNotExpression() {
            EvaluationContext ctx = EvaluationContext.of("disabled", false);

            assertThat(parser.evaluateBoolean("NOT [disabled]", ctx)).isTrue();
        }

        @Test
        @DisplayName("should evaluate IN expression")
        void shouldEvaluateInExpression() {
            EvaluationContext ctx = EvaluationContext.of("role", "admin");

            assertThat(parser.evaluateBoolean("[role] IN ('admin', 'moderator')", ctx)).isTrue();
        }

        @Test
        @DisplayName("should evaluate NOT IN expression")
        void shouldEvaluateNotInExpression() {
            EvaluationContext ctx = EvaluationContext.of("role", "user");

            assertThat(parser.evaluateBoolean("[role] NOT IN ('admin', 'moderator')", ctx)).isTrue();
        }

        @Test
        @DisplayName("should evaluate complex condition")
        void shouldEvaluateComplexCondition() {
            EvaluationContext ctx = EvaluationContext.of(Map.of("age", 25.0, "status", "active"));

            assertThat(parser.evaluateBoolean("[age] > 18 AND [status] == 'active'", ctx)).isTrue();
        }

        @Test
        @DisplayName("should return false when equality compares fields of different types")
        void shouldReturnFalseWhenEqualityComparesFieldOfDifferentTypes() {
            EvaluationContext ctx = EvaluationContext.of("age", 25.0);

            assertThat(parser.evaluateBoolean("[age] == 'thirty'", ctx)).isFalse();
        }

        @Test
        @DisplayName("should return true when inequality compares fields of different types")
        void shouldReturnTrueWhenInequalityComparesFieldOfDifferentTypes() {
            EvaluationContext ctx = EvaluationContext.of("age", 25.0);

            assertThat(parser.evaluateBoolean("[age] != 'thirty'", ctx)).isTrue();
        }
    }

    @Nested
    @DisplayName("EvaluateDouble")
    class EvaluateDouble {

        @Test
        @DisplayName("should evaluate arithmetic over fields")
        void shouldEvaluateArithmeticOverFields() {
            EvaluationContext ctx = EvaluationContext.of(Map.of("price", 100.0, "tax_rate", 0.2));

            assertThat(parser.evaluateDouble("[price] * (1 + [tax_rate])", ctx)).isEqualTo(120.0);
        }

        @Test
        @DisplayName("should evaluate function call")
        void shouldEvaluateFunctionCall() {
            EvaluationContext ctx = EvaluationContext.of("balance", -50.0);

            assertThat(parser.evaluateDouble("abs([balance])", ctx)).isEqualTo(50.0);
        }

        @Test
        @DisplayName("should evaluate modulo operator")
        void shouldEvaluateModuloOperator() {
            EvaluationContext ctx = EvaluationContext.of("x", 10.0);

            assertThat(parser.evaluateDouble("[x] % 3", ctx)).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should coerce Integer field to double")
        void shouldCoerceIntegerFieldToDouble() {
            EvaluationContext ctx = EvaluationContext.of("count", Integer.valueOf(42));

            assertThat(parser.evaluateDouble("[count] + 1", ctx)).isEqualTo(43.0);
        }

        @Test
        @DisplayName("should coerce Long field to double")
        void shouldCoerceLongFieldToDouble() {
            EvaluationContext ctx = EvaluationContext.of("count", Long.valueOf(100L));

            assertThat(parser.evaluateDouble("[count] + 1", ctx)).isEqualTo(101.0);
        }

        @Test
        @DisplayName("should coerce BigDecimal field to double")
        void shouldCoerceBigDecimalFieldToDouble() {
            EvaluationContext ctx = EvaluationContext.of("amount", new BigDecimal("99.99"));

            assertThat(parser.evaluateDouble("[amount] + 0.01", ctx)).isEqualTo(100.0);
        }
    }

    @Nested
    class EvaluateBooleanParameterized {

        @ParameterizedTest(name = "{0}")
        @MethodSource("booleanExpressions")
        @DisplayName("should evaluate boolean expression correctly")
        void shouldEvaluateBooleanExpression(String expression, Map<String, Object> context, boolean expected) {
            EvaluationResult result = parser.evaluate(expression, EvaluationContext.of(context));
            assertThat(result).isInstanceOf(BooleanResult.class);
            assertThat(((BooleanResult) result).value()).isEqualTo(expected);
        }

        static Stream<Arguments> booleanExpressions() {
            return Stream.of(
                // ── Numeric comparisons ──────────────────────────────────────────
                Arguments.of("[age] > 18",        Map.of("age", 25.0),   true),
                Arguments.of("[age] > 18",        Map.of("age", 18.0),   false),
                Arguments.of("[age] >= 18",       Map.of("age", 18.0),   true),
                Arguments.of("[age] < 100",       Map.of("age", 25.0),   true),
                Arguments.of("[age] <= 25",       Map.of("age", 25.0),   true),
                Arguments.of("[age] == 25",       Map.of("age", 25.0),   true),
                Arguments.of("[age] != 25",       Map.of("age", 30.0),   true),

                // ── String comparisons ───────────────────────────────────────────
                Arguments.of("[status] == 'active'",  Map.of("status", "active"),  true),
                Arguments.of("[status] == 'active'",  Map.of("status", "blocked"), false),
                Arguments.of("[status] != 'blocked'", Map.of("status", "active"),  true),

                // ── Mixed-type equality (different types are never equal) ─────────
                Arguments.of("[age] == 'thirty'", Map.of("age", 25.0), false),
                Arguments.of("[age] != 'thirty'", Map.of("age", 25.0), true),

                // ── Logical AND / OR / NOT ────────────────────────────────────────
                Arguments.of("[a] > 1 AND [b] > 1", Map.of("a", 2.0, "b", 2.0), true),
                Arguments.of("[a] > 1 AND [b] > 1", Map.of("a", 2.0, "b", 0.0), false),
                Arguments.of("[a] > 1 OR [b] > 1",  Map.of("a", 0.0, "b", 2.0), true),
                Arguments.of("[a] > 1 OR [b] > 1",  Map.of("a", 0.0, "b", 0.0), false),
                Arguments.of("NOT [a] > 1",          Map.of("a", 0.0),            true),
                Arguments.of("NOT [a] > 1",          Map.of("a", 2.0),            false),

                // ── AND has higher precedence than OR ─────────────────────────────
                Arguments.of("[a] > 1 AND [b] > 1 OR [c] > 1",
                    Map.of("a", 0.0, "b", 0.0, "c", 2.0), true),
                Arguments.of("[a] > 1 OR [b] > 1 AND [c] > 1",
                    Map.of("a", 2.0, "b", 0.0, "c", 0.0), true),

                // ── IN / NOT IN ───────────────────────────────────────────────────
                Arguments.of("[s] IN ('a', 'b', 'c')",     Map.of("s", "a"), true),
                Arguments.of("[s] IN ('a', 'b', 'c')",     Map.of("s", "x"), false),
                Arguments.of("[s] NOT IN ('a', 'b', 'c')", Map.of("s", "x"), true),
                Arguments.of("[s] NOT IN ('a', 'b', 'c')", Map.of("s", "a"), false),
                Arguments.of("[s] in ('a', 'b')",          Map.of("s", "a"), true)  // lowercase IN
            );
        }
    }

    @Nested
    class EvaluateDoubleParameterized {

        @ParameterizedTest(name = "{0}")
        @MethodSource("doubleExpressions")
        @DisplayName("should evaluate double expression correctly")
        void shouldEvaluateDoubleExpression(String expression, Map<String, Object> context, double expected) {
            EvaluationResult result = parser.evaluate(expression, EvaluationContext.of(context));
            assertThat(result).isInstanceOf(DoubleResult.class);
            assertThat(((DoubleResult) result).value()).isEqualTo(expected, within(1e-9));
        }

        static Stream<Arguments> doubleExpressions() {
            return Stream.of(
                // ── Basic arithmetic ──────────────────────────────────────────────
                Arguments.of("[a] + [b]", Map.of("a", 3.0, "b", 2.0), 5.0),
                Arguments.of("[a] - [b]", Map.of("a", 3.0, "b", 2.0), 1.0),
                Arguments.of("[a] * [b]", Map.of("a", 3.0, "b", 2.0), 6.0),
                Arguments.of("[a] / [b]", Map.of("a", 6.0, "b", 2.0), 3.0),
                Arguments.of("[a] % [b]", Map.of("a", 7.0, "b", 3.0), 1.0),

                // ── Power operator ────────────────────────────────────────────────
                Arguments.of("[a] ^ 2",   Map.of("a", 3.0), 9.0),
                Arguments.of("[a] ** 2",  Map.of("a", 3.0), 9.0),
                Arguments.of("2 ^ 3 ^ 2", Map.of(),         512.0),  // right-associative: 2^(3^2) = 2^9

                // ── Operator precedence ───────────────────────────────────────────
                Arguments.of("[a] + [b] * [c]",   Map.of("a", 2.0, "b", 3.0, "c", 4.0), 14.0),
                Arguments.of("([a] + [b]) * [c]", Map.of("a", 2.0, "b", 3.0, "c", 4.0), 20.0),

                // ── Unary minus ───────────────────────────────────────────────────
                Arguments.of("-[a]",       Map.of("a", 5.0),        -5.0),
                Arguments.of("-[a] + [b]", Map.of("a", 3.0, "b", 10.0), 7.0),

                // ── Built-in functions ────────────────────────────────────────────
                Arguments.of("abs([a])",      Map.of("a", -5.0),        5.0),
                Arguments.of("abs([a])",      Map.of("a",  5.0),        5.0),
                Arguments.of("round([a])",    Map.of("a",  2.7),        3.0),
                Arguments.of("floor([a])",    Map.of("a",  2.7),        2.0),
                Arguments.of("ceil([a])",     Map.of("a",  2.1),        3.0),
                Arguments.of("min([a], [b])", Map.of("a",  3.0, "b", 7.0), 3.0),
                Arguments.of("max([a], [b])", Map.of("a",  3.0, "b", 7.0), 7.0),
                Arguments.of("pow([a], [b])", Map.of("a",  2.0, "b", 10.0), 1024.0),

                // ── Numeric type coercion ─────────────────────────────────────────
                Arguments.of("[a] + 1", Map.of("a", Integer.valueOf(5)),          6.0),
                Arguments.of("[a] + 1", Map.of("a", Long.valueOf(5L)),            6.0),
                Arguments.of("[a] + 1", Map.of("a", new java.math.BigDecimal("5.5")), 6.5),

                // ── Field names with spaces ───────────────────────────────────────
                Arguments.of("[first name] + [last name]",
                    Map.of("first name", 10.0, "last name", 5.0), 15.0)
            );
        }
    }

    @Nested
    class EvaluateErrorParameterized {

        @ParameterizedTest(name = "{0}")
        @MethodSource("errorExpressions")
        @DisplayName("should throw ExpressionEvaluationException for invalid evaluation")
        void shouldThrowOnInvalidEvaluation(String expression, Map<String, Object> context) {
            assertThatThrownBy(() ->
                parser.evaluate(expression, EvaluationContext.of(context))
            ).isInstanceOf(ExpressionEvaluationException.class);
        }

        static Stream<Arguments> errorExpressions() {
            return Stream.of(
                // ── Division by zero ──────────────────────────────────────────────
                Arguments.of("[a] / 0",            Map.of("a", 10.0)),

                // ── Ordering operator on string operand ───────────────────────────
                Arguments.of("[a] > 'hello'",      Map.of("a", 5.0)),

                // ── Arithmetic on string field ────────────────────────────────────
                Arguments.of("[a] + 1",            Map.of("a", "text")),

                // ── Arithmetic on boolean field ───────────────────────────────────
                Arguments.of("[a] + 1",            Map.of("a", true)),

                // ── Unknown field ─────────────────────────────────────────────────
                Arguments.of("[unknown] > 1",      Map.of()),

                // ── Unknown function ──────────────────────────────────────────────
                Arguments.of("unknown_func([a])",  Map.of("a", 1.0)),

                // ── Wrong argument count ──────────────────────────────────────────
                Arguments.of("abs([a], [b])",      Map.of("a", 1.0, "b", 2.0))
            );
        }
    }
}
