package ru.zahaand.dataexpr.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zahaand.dataexpr.DataExpressionLexer;
import ru.zahaand.dataexpr.ast.Expression;
import ru.zahaand.dataexpr.evaluator.BooleanResult;
import ru.zahaand.dataexpr.evaluator.DoubleResult;
import ru.zahaand.dataexpr.evaluator.EvaluationContext;
import ru.zahaand.dataexpr.evaluator.EvaluationResult;
import ru.zahaand.dataexpr.evaluator.ExpressionEvaluator;
import ru.zahaand.dataexpr.exception.ExpressionEvaluationException;
import ru.zahaand.dataexpr.exception.ExpressionParseException;
import ru.zahaand.dataexpr.function.CustomFunctionRegistry;
import ru.zahaand.dataexpr.visitor.AstBuildingVisitor;

/**
 * Primary entry point for parsing and evaluating business expressions over named data fields.
 *
 * <p>Expressions reference field values from an {@link EvaluationContext} using square-bracket
 * syntax: {@code [field_name]}. Supported constructs include arithmetic operators, comparisons,
 * logical operators ({@code AND}, {@code OR}, {@code NOT}), {@code IN} / {@code NOT IN} checks,
 * built-in math functions, and consumer-registered custom functions.
 *
 * <p>This class is stateless and thread-safe. It is safe to use as a Spring singleton bean.
 * A new {@link AstBuildingVisitor} is created on every {@link #parse(String)} call to ensure
 * thread safety.
 *
 * <p>Typical Spring Boot usage — add the starter dependency and inject:
 * <pre>{@code
 * @Autowired
 * DataExpressionParser parser;
 *
 * boolean result = parser.evaluateBoolean(
 *     "[age] > 18 AND [status] == 'active'",
 *     EvaluationContext.of(Map.of("age", 25.0, "status", "active"))
 * );
 * }</pre>
 *
 * <p>Without Spring Boot:
 * <pre>{@code
 * var parser = new DataExpressionParser(new ExpressionEvaluator());
 * }</pre>
 */
public final class DataExpressionParser {

    private static final Logger log = LoggerFactory.getLogger(DataExpressionParser.class);

    private final ExpressionEvaluator evaluator;
    private final CustomFunctionRegistry customFunctionRegistry;

    public DataExpressionParser(ExpressionEvaluator evaluator) {
        this(evaluator, CustomFunctionRegistry.empty());
    }

    public DataExpressionParser(ExpressionEvaluator evaluator, CustomFunctionRegistry customFunctionRegistry) {
        this.evaluator = evaluator;
        this.customFunctionRegistry = customFunctionRegistry;
    }

    /**
     * Validates the syntax of an expression without evaluating it.
     *
     * <p>Function names and field names are not checked — they are runtime concerns.
     * An expression that references an undefined function or field is still syntactically valid
     * and returns {@link ValidationResult#valid()}.
     *
     * <p>Useful for admin UIs where users author expressions and immediate feedback is needed:
     * <pre>{@code
     * ValidationResult result = parser.validate("[age] >");
     * if (!result.isValid()) {
     *     result.errorMessage().ifPresent(System.out::println);
     * }
     * }</pre>
     *
     * @param expression the expression string to validate; must not be {@code null} or blank
     * @return {@link ValidationResult#valid()} if syntax is correct;
     *         {@link ValidationResult#invalid(String)} with an error message otherwise
     * @throws ExpressionParseException if {@code expression} is {@code null} or blank
     */
    public ValidationResult validate(String expression) {
        if (StringUtils.isBlank(expression)) {
            throw new ExpressionParseException(
                    expression == null ? "Expression must not be null" : "Expression must not be blank");
        }

        DataExpressionLexer lexer = new DataExpressionLexer(CharStreams.fromString(expression));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ru.zahaand.dataexpr.DataExpressionParser parser =
                new ru.zahaand.dataexpr.DataExpressionParser(tokens);

        String[] firstError = new String[1];
        BaseErrorListener capturing = new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine, String msg,
                                    RecognitionException e) {
                if (firstError[0] == null) {
                    firstError[0] = "Parse error at line " + line + ":" + charPositionInLine + ": " + msg;
                }
            }
        };
        lexer.removeErrorListeners();
        lexer.addErrorListener(capturing);
        parser.removeErrorListeners();
        parser.addErrorListener(capturing);

        parser.prog();

        if (firstError[0] == null) {
            return ValidationResult.valid();
        }
        log.debug("Expression validation failed: {}", firstError[0]);
        return ValidationResult.invalid(firstError[0]);
    }

    /**
     * Parses an expression string into an AST ({@link Expression} tree).
     *
     * <p>The returned AST can be inspected or passed to {@link #evaluateBoolean(Expression, EvaluationContext)}
     * and {@link #evaluateDouble(Expression, EvaluationContext)} for repeated evaluation without
     * re-parsing. This is the recommended pattern when the same expression is evaluated
     * against many contexts.
     *
     * @param expression the expression string to parse; must not be {@code null} or blank
     * @return the root {@link Expression} node of the parsed AST
     * @throws ExpressionParseException if {@code expression} is {@code null}, blank, or syntactically invalid
     */
    public Expression parse(String expression) {
        if (StringUtils.isBlank(expression)) {
            log.error("Failed to parse expression: expression is null or blank");
            throw new ExpressionParseException(
                    expression == null ? "Expression must not be null" : "Expression must not be blank");
        }

        DataExpressionLexer lexer = new DataExpressionLexer(CharStreams.fromString(expression));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ThrowingErrorListener());

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ru.zahaand.dataexpr.DataExpressionParser parser =
                new ru.zahaand.dataexpr.DataExpressionParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ThrowingErrorListener());

        ru.zahaand.dataexpr.DataExpressionParser.ProgContext tree = parser.prog();
        return new AstBuildingVisitor().visit(tree);
    }

    /**
     * Parses and evaluates an expression string in one step.
     *
     * <p>Returns an {@link EvaluationResult} — either a {@link DoubleResult} or a {@link BooleanResult}.
     * Use {@link #evaluateBoolean} or {@link #evaluateDouble} when the result type is known.
     *
     * @param expression the expression string to parse and evaluate; must not be {@code null} or blank
     * @param context    the field values available during evaluation; must not be {@code null}
     * @return the evaluation result
     * @throws ExpressionParseException      if the expression is syntactically invalid
     * @throws ExpressionEvaluationException if a runtime error occurs (unknown field, type mismatch, etc.)
     */
    public EvaluationResult evaluate(String expression, EvaluationContext context) {
        Expression ast = parse(expression);
        return evaluator.evaluate(ast, context);
    }

    /**
     * Parses and evaluates an expression, unwrapping the result as a {@code boolean}.
     *
     * <p>Convenience method for expressions known to produce a boolean result
     * (comparisons, logical operators, {@code IN} checks).
     *
     * @param expression the expression string; must not be {@code null} or blank
     * @param context    the field values available during evaluation; must not be {@code null}
     * @return the boolean result
     * @throws ExpressionParseException      if the expression is syntactically invalid
     * @throws ExpressionEvaluationException if the result is not a boolean, or a runtime error occurs
     */
    public boolean evaluateBoolean(String expression, EvaluationContext context) {
        EvaluationResult result = evaluate(expression, context);
        if (result instanceof BooleanResult booleanResult) {
            return booleanResult.value();
        }
        log.error("Expected boolean result but got {} for expression: {}", result.getClass().getSimpleName(), expression);
        throw new ExpressionEvaluationException(
                "Expected boolean result but got double result");
    }

    /**
     * Parses and evaluates an expression, unwrapping the result as a {@code double}.
     *
     * <p>Convenience method for expressions known to produce a numeric result
     * (arithmetic, built-in functions, custom functions).
     *
     * @param expression the expression string; must not be {@code null} or blank
     * @param context    the field values available during evaluation; must not be {@code null}
     * @return the numeric result
     * @throws ExpressionParseException      if the expression is syntactically invalid
     * @throws ExpressionEvaluationException if the result is not a double, or a runtime error occurs
     */
    public double evaluateDouble(String expression, EvaluationContext context) {
        EvaluationResult result = evaluate(expression, context);
        if (result instanceof DoubleResult doubleResult) {
            return doubleResult.value();
        }
        log.error("Expected double result but got {} for expression: {}", result.getClass().getSimpleName(), expression);
        throw new ExpressionEvaluationException(
                "Expected double result but got boolean result");
    }

    private static final class ThrowingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg,
                                RecognitionException e) {
            throw new ExpressionParseException(
                    "Parse error at line " + line + ":" + charPositionInLine + ": " + msg);
        }
    }
}
