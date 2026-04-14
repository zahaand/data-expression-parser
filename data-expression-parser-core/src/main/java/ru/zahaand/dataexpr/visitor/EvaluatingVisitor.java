package ru.zahaand.dataexpr.visitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zahaand.dataexpr.ast.*;
import ru.zahaand.dataexpr.evaluator.BooleanResult;
import ru.zahaand.dataexpr.evaluator.DoubleResult;
import ru.zahaand.dataexpr.evaluator.EvaluationContext;
import ru.zahaand.dataexpr.evaluator.EvaluationResult;
import ru.zahaand.dataexpr.exception.ExpressionEvaluationException;
import ru.zahaand.dataexpr.function.BuiltinFunctionRegistry;
import ru.zahaand.dataexpr.function.CustomFunctionRegistry;
import ru.zahaand.dataexpr.function.ExpressionFunction;

import java.util.List;

public final class EvaluatingVisitor {

    private static final Logger log = LoggerFactory.getLogger(EvaluatingVisitor.class);

    private final EvaluationContext context;
    private final CustomFunctionRegistry customFunctionRegistry;

    public EvaluatingVisitor(EvaluationContext context) {
        this(context, CustomFunctionRegistry.empty());
    }

    public EvaluatingVisitor(EvaluationContext context, CustomFunctionRegistry customFunctionRegistry) {
        this.context = context;
        this.customFunctionRegistry = customFunctionRegistry;
    }

    public EvaluationResult evaluate(Expression expression) {
        return switch (expression) {
            case NumberNode node -> new DoubleResult(node.value());
            case StringNode node -> throw new ExpressionEvaluationException(
                    "Cannot evaluate string literal '" + node.value() + "' as a standalone result");
            case BooleanNode node -> new BooleanResult(node.value());
            case FieldNode node -> evaluateField(node);
            case BinaryOpNode node -> evaluateBinaryOp(node);
            case UnaryMinusNode node -> evaluateUnaryMinus(node);
            case FunctionCallNode node -> evaluateFunction(node);
            case ComparisonNode node -> evaluateComparison(node);
            case LogicalNode node -> evaluateLogical(node);
            case NotNode node -> evaluateNot(node);
            case InNode node -> evaluateIn(node);
        };
    }

    private EvaluationResult evaluateField(FieldNode node) {
        Object value = context.get(node.fieldName());
        if (value instanceof Number number) {
            return new DoubleResult(number.doubleValue());
        }
        if (value instanceof String s) {
            return new DoubleResult(Double.NaN); // placeholder — actual use determined by parent node
        }
        if (value instanceof Boolean b) {
            return new BooleanResult(b);
        }
        throw new ExpressionEvaluationException(
                "Unsupported field value type: " + value.getClass().getName());
    }

    private double toDouble(Expression expr) {
        Object rawValue = resolveRawValue(expr);
        if (rawValue instanceof Boolean) {
            log.error("Boolean value used in arithmetic context");
            throw new ExpressionEvaluationException(
                    "Cannot use boolean value in arithmetic context");
        }
        if (rawValue instanceof Number number) {
            return number.doubleValue();
        }
        if (rawValue instanceof String) {
            log.error("String value used in arithmetic context");
            throw new ExpressionEvaluationException(
                    "Cannot use string value in arithmetic context");
        }
        EvaluationResult result = evaluate(expr);
        if (result instanceof DoubleResult dr) {
            return dr.value();
        }
        log.error("Unexpected evaluation result type: {}", result.getClass());
        throw new ExpressionEvaluationException(
                "Expected numeric value in arithmetic context");
    }

    private Object resolveRawValue(Expression expr) {
        if (expr instanceof FieldNode fieldNode) {
            return context.get(fieldNode.fieldName());
        }
        if (expr instanceof BooleanNode booleanNode) {
            return booleanNode.value();
        }
        if (expr instanceof NumberNode numberNode) {
            return numberNode.value();
        }
        if (expr instanceof StringNode stringNode) {
            return stringNode.value();
        }
        return null; // complex expression — must evaluate
    }

    private EvaluationResult evaluateBinaryOp(BinaryOpNode node) {
        double left = toDouble(node.left());
        double right = toDouble(node.right());
        double result = switch (node.op()) {
            case ADD -> left + right;
            case SUBTRACT -> left - right;
            case MULTIPLY -> left * right;
            case DIVIDE -> {
                if (right == 0.0) {
                    log.error("Division by zero in expression");
                    throw new ExpressionEvaluationException("Division by zero");
                }
                yield left / right;
            }
            case MODULO -> {
                if (right == 0.0) {
                    log.error("Division by zero in expression");
                    throw new ExpressionEvaluationException("Division by zero");
                }
                yield left % right;
            }
            case POWER -> Math.pow(left, right);
        };
        return new DoubleResult(result);
    }

    private EvaluationResult evaluateUnaryMinus(UnaryMinusNode node) {
        double value = toDouble(node.operand());
        return new DoubleResult(-value);
    }

    private EvaluationResult evaluateFunction(FunctionCallNode node) {
        double[] args = new double[node.args().size()];
        for (int i = 0; i < node.args().size(); i++) {
            args[i] = toDouble(node.args().get(i));
        }
        ExpressionFunction custom = customFunctionRegistry.find(node.name());
        if (custom != null) {
            try {
                return new DoubleResult(custom.apply(args, context));
            } catch (RuntimeException ex) {
                log.warn("Custom function '{}' threw {}", node.name(), ex.toString());
                throw new ExpressionEvaluationException(
                        "Error in custom function '" + node.name() + "': " + ex.getMessage(), ex);
            }
        }
        double result = BuiltinFunctionRegistry.invoke(node.name(), args);
        return new DoubleResult(result);
    }

    private EvaluationResult evaluateComparison(ComparisonNode node) {
        Object leftRaw = resolveValue(node.left());
        Object rightRaw = resolveValue(node.right());

        return switch (node.op()) {
            case EQ -> new BooleanResult(isEqual(leftRaw, rightRaw));
            case NEQ -> new BooleanResult(!isEqual(leftRaw, rightRaw));
            case GT, LT, GTE, LTE -> {
                double leftNum = requireNumericForOrdering(leftRaw);
                double rightNum = requireNumericForOrdering(rightRaw);
                yield new BooleanResult(switch (node.op()) {
                    case GT -> leftNum > rightNum;
                    case LT -> leftNum < rightNum;
                    case GTE -> leftNum >= rightNum;
                    case LTE -> leftNum <= rightNum;
                    default -> throw new IllegalStateException();
                });
            }
        };
    }

    private Object resolveValue(Expression expr) {
        if (expr instanceof FieldNode fieldNode) {
            return context.get(fieldNode.fieldName());
        }
        if (expr instanceof NumberNode numberNode) {
            return numberNode.value();
        }
        if (expr instanceof StringNode stringNode) {
            return stringNode.value();
        }
        if (expr instanceof BooleanNode booleanNode) {
            return booleanNode.value();
        }
        EvaluationResult result = evaluate(expr);
        if (result instanceof DoubleResult dr) {
            return dr.value();
        }
        if (result instanceof BooleanResult br) {
            return br.value();
        }
        throw new ExpressionEvaluationException("Unexpected evaluation result");
    }

    private boolean isEqual(Object left, Object right) {
        if (left instanceof Number leftNum && right instanceof Number rightNum) {
            return leftNum.doubleValue() == rightNum.doubleValue();
        }
        if (left instanceof String && right instanceof String) {
            return left.equals(right);
        }
        if (left instanceof Boolean && right instanceof Boolean) {
            return left.equals(right);
        }
        // Mixed types: not equal
        return false;
    }

    private double requireNumericForOrdering(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        log.error("Ordering operator applied to non-numeric operand: {}", value);
        throw new ExpressionEvaluationException(
                "Ordering operators (>, <, >=, <=) require numeric operands, got: " + value.getClass().getSimpleName());
    }

    private EvaluationResult evaluateLogical(LogicalNode node) {
        boolean left = toBoolean(node.left());
        boolean right = toBoolean(node.right());
        boolean result = switch (node.op()) {
            case AND -> left && right;
            case OR -> left || right;
        };
        return new BooleanResult(result);
    }

    private EvaluationResult evaluateNot(NotNode node) {
        boolean value = toBoolean(node.operand());
        return new BooleanResult(!value);
    }

    private boolean toBoolean(Expression expr) {
        EvaluationResult result = evaluate(expr);
        if (result instanceof BooleanResult br) {
            return br.value();
        }
        throw new ExpressionEvaluationException(
                "Expected boolean value in logical context");
    }

    private EvaluationResult evaluateIn(InNode node) {
        Object fieldValue = resolveValue(node.field());
        List<Expression> valueExprs = node.values();

        boolean found = false;
        for (Expression valueExpr : valueExprs) {
            Object listValue = resolveValue(valueExpr);
            if (isEqual(fieldValue, listValue)) {
                found = true;
                break;
            }
        }

        return new BooleanResult(node.negated() != found);
    }
}
