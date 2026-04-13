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
import ru.zahaand.dataexpr.visitor.AstBuildingVisitor;

public final class DataExpressionParser {

    private static final Logger log = LoggerFactory.getLogger(DataExpressionParser.class);

    private final ExpressionEvaluator evaluator;

    public DataExpressionParser(ExpressionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

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

    public EvaluationResult evaluate(String expression, EvaluationContext context) {
        Expression ast = parse(expression);
        return evaluator.evaluate(ast, context);
    }

    public boolean evaluateBoolean(String expression, EvaluationContext context) {
        EvaluationResult result = evaluate(expression, context);
        if (result instanceof BooleanResult booleanResult) {
            return booleanResult.value();
        }
        log.error("Expected boolean result but got {} for expression: {}", result.getClass().getSimpleName(), expression);
        throw new ExpressionEvaluationException(
                "Expected boolean result but got double result");
    }

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
