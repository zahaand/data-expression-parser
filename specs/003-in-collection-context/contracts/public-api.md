# Phase 1 — Public API Contracts

**Feature**: Dynamic IN / NOT IN Against Context Collection
**Branch**: `003-in-collection-context`

## Grammar contract (`DataExpression.g4`)

```antlr
comparison : additive ( ( '>=' | '<=' | '>' | '<' | '==' | '!=' ) additive )?
           | additive IN     '(' valueList ')'   // existing
           | additive NOT IN '(' valueList ')'   // existing
           | additive IN     FIELD               // NEW
           | additive NOT IN FIELD               // NEW
           ;
```

LL(1) disambiguation: `'('` vs `FIELD` lookahead. No ambiguity.

## Parser contract (`DataExpressionParser`) — UNCHANGED signatures

```java
public boolean           evaluateBoolean(String expression, EvaluationContext context);
public double            evaluateDouble (String expression, EvaluationContext context);
public ValidationResult  validate       (String expression);
public DataExpressionParser(ExpressionEvaluator evaluator);
public DataExpressionParser(ExpressionEvaluator evaluator, CustomFunctionRegistry registry);
```

New behavior is reachable only through expression strings containing `IN [field]` or `NOT IN [field]`. Parser API surface is untouched.

## EvaluationContext contract — UNCHANGED

```java
EvaluationContext.of(String, Object)
EvaluationContext.of(Map<String, Object>)
EvaluationContext.empty()
```

New supported value type for `of(Map)`: **`List<Object>`** (where elements are `Number`, `String`, or `Boolean`). This was already accepted by the `Object` parameter; v1.2.0 adds semantic meaning via the IN operator.

## Exception contract (`ExpressionEvaluationException`)

Three new throw sites in `EvaluatingVisitor` when evaluating an `InNode` whose `collection` is a `FieldNode`:

| Condition                                                   | Message                                                                             |
|-------------------------------------------------------------|-------------------------------------------------------------------------------------|
| RHS field absent from context                               | `Field '<name>' not found in context`                                               |
| RHS field resolves to non-`List` value                      | `Field '<name>' must be a List for IN operator, got: <simpleName>`                  |
| Any element of the list is not `Number`, `String`, `Boolean`| `Collection field '<name>' contains unsupported element type: <simpleName>`        |

Each throw is preceded by a `log.error(...)` call (Constitution V, FR-210).

## AST contract (internal; spec acknowledges breaking change)

```java
// BREAKING (v1.1.0 → v1.2.0):
record InNode(Expression operand, Expression collection, boolean negated)
  implements Expression {}

// NEW:
record InListNode(List<Expression> values) implements Expression {}

// Permits clause extended to include InListNode.
sealed interface Expression permits …, InNode, InListNode {}
```

Direct AST construction by consumers is unsupported. Any consumer impact is limited to those who reflect on AST node shapes outside the parser.

## Module coordinates — v1.2.0

- `ru.zahaand:data-expression-parser:1.2.0` (parent pom)
- `ru.zahaand:data-expression-parser-core:1.2.0`
- `ru.zahaand:data-expression-parser-spring-boot-starter:1.2.0`

No new external dependencies.
