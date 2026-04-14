package ru.zahaand.dataexpr.visitor;

import org.antlr.v4.runtime.Token;
import ru.zahaand.dataexpr.DataExpressionBaseVisitor;
import ru.zahaand.dataexpr.DataExpressionParser;
import ru.zahaand.dataexpr.ast.*;

import java.util.ArrayList;
import java.util.List;

public final class AstBuildingVisitor extends DataExpressionBaseVisitor<Expression> {

    @Override
    public Expression visitProg(DataExpressionParser.ProgContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Expression visitExpr(DataExpressionParser.ExprContext ctx) {
        return visit(ctx.orExpr());
    }

    @Override
    public Expression visitOrExpr(DataExpressionParser.OrExprContext ctx) {
        List<DataExpressionParser.AndExprContext> children = ctx.andExpr();
        Expression result = visit(children.get(0));
        for (int i = 1; i < children.size(); i++) {
            result = new LogicalNode(result, LogicalOperator.OR, visit(children.get(i)));
        }
        return result;
    }

    @Override
    public Expression visitAndExpr(DataExpressionParser.AndExprContext ctx) {
        List<DataExpressionParser.NotExprContext> children = ctx.notExpr();
        Expression result = visit(children.get(0));
        for (int i = 1; i < children.size(); i++) {
            result = new LogicalNode(result, LogicalOperator.AND, visit(children.get(i)));
        }
        return result;
    }

    @Override
    public Expression visitNotExpr(DataExpressionParser.NotExprContext ctx) {
        if (ctx.NOT() != null) {
            return new NotNode(visit(ctx.notExpr()));
        }
        return visit(ctx.comparison());
    }

    @Override
    public Expression visitComparison(DataExpressionParser.ComparisonContext ctx) {
        List<DataExpressionParser.AdditiveContext> additives = ctx.additive();
        Expression left = visit(additives.get(0));

        if (ctx.IN() != null) {
            boolean negated = ctx.NOT() != null;
            if (ctx.FIELD() != null) {
                String raw = ctx.FIELD().getText();
                String fieldName = raw.substring(1, raw.length() - 1);
                return new InNode(left, new FieldNode(fieldName), negated);
            }
            List<Expression> values = new ArrayList<>();
            for (DataExpressionParser.LiteralContext litCtx : ctx.valueList().literal()) {
                values.add(visit(litCtx));
            }
            return new InNode(left, new InListNode(values), negated);
        }

        if (additives.size() == 2) {
            Expression right = visit(additives.get(1));
            ComparisonOperator op = resolveComparisonOperator(ctx);
            return new ComparisonNode(left, op, right);
        }

        return left;
    }

    private ComparisonOperator resolveComparisonOperator(DataExpressionParser.ComparisonContext ctx) {
        Token opToken = (Token) ctx.getChild(1).getPayload();
        return switch (opToken.getText()) {
            case ">" -> ComparisonOperator.GT;
            case "<" -> ComparisonOperator.LT;
            case ">=" -> ComparisonOperator.GTE;
            case "<=" -> ComparisonOperator.LTE;
            case "==" -> ComparisonOperator.EQ;
            case "!=" -> ComparisonOperator.NEQ;
            default -> throw new IllegalStateException("Unknown comparison operator: " + opToken.getText());
        };
    }

    @Override
    public Expression visitAdditive(DataExpressionParser.AdditiveContext ctx) {
        List<DataExpressionParser.MultiplicativeContext> children = ctx.multiplicative();
        Expression result = visit(children.get(0));
        for (int i = 1; i < children.size(); i++) {
            Token opToken = (Token) ctx.getChild(2 * i - 1).getPayload();
            ArithmeticOperator op = opToken.getText().equals("+")
                    ? ArithmeticOperator.ADD
                    : ArithmeticOperator.SUBTRACT;
            result = new BinaryOpNode(result, op, visit(children.get(i)));
        }
        return result;
    }

    @Override
    public Expression visitMultiplicative(DataExpressionParser.MultiplicativeContext ctx) {
        List<DataExpressionParser.PowerContext> children = ctx.power();
        Expression result = visit(children.get(0));
        for (int i = 1; i < children.size(); i++) {
            Token opToken = (Token) ctx.getChild(2 * i - 1).getPayload();
            ArithmeticOperator op = switch (opToken.getText()) {
                case "*" -> ArithmeticOperator.MULTIPLY;
                case "/" -> ArithmeticOperator.DIVIDE;
                case "%" -> ArithmeticOperator.MODULO;
                default -> throw new IllegalStateException("Unknown multiplicative operator: " + opToken.getText());
            };
            result = new BinaryOpNode(result, op, visit(children.get(i)));
        }
        return result;
    }

    @Override
    public Expression visitPower(DataExpressionParser.PowerContext ctx) {
        List<DataExpressionParser.UnaryContext> children = ctx.unary();
        // Right-associative: a ^ b ^ c → BinaryOpNode(a, POWER, BinaryOpNode(b, POWER, c))
        Expression result = visit(children.get(children.size() - 1));
        for (int i = children.size() - 2; i >= 0; i--) {
            result = new BinaryOpNode(visit(children.get(i)), ArithmeticOperator.POWER, result);
        }
        return result;
    }

    @Override
    public Expression visitUnary(DataExpressionParser.UnaryContext ctx) {
        if (ctx.unary() != null) {
            return new UnaryMinusNode(visit(ctx.unary()));
        }
        return visit(ctx.primary());
    }

    @Override
    public Expression visitPrimary(DataExpressionParser.PrimaryContext ctx) {
        if (ctx.FIELD() != null) {
            String raw = ctx.FIELD().getText();
            String fieldName = raw.substring(1, raw.length() - 1);
            return new FieldNode(fieldName);
        }
        if (ctx.literal() != null) {
            return visit(ctx.literal());
        }
        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            List<Expression> args = new ArrayList<>();
            if (ctx.argList() != null) {
                for (DataExpressionParser.ExprContext exprCtx : ctx.argList().expr()) {
                    args.add(visit(exprCtx));
                }
            }
            return new FunctionCallNode(name, args);
        }
        // Parenthesized expression
        return visit(ctx.expr());
    }

    @Override
    public Expression visitLiteral(DataExpressionParser.LiteralContext ctx) {
        if (ctx.NUMBER() != null) {
            return new NumberNode(Double.parseDouble(ctx.NUMBER().getText()));
        }
        if (ctx.STRING() != null) {
            String raw = ctx.STRING().getText();
            String value = raw.substring(1, raw.length() - 1);
            return new StringNode(value);
        }
        if (ctx.TRUE() != null) {
            return new BooleanNode(true);
        }
        if (ctx.FALSE() != null) {
            return new BooleanNode(false);
        }
        throw new IllegalStateException("Unknown literal type");
    }
}
