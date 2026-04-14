# Phase 1 — Data Model (AST)

**Feature**: Dynamic IN / NOT IN Against Context Collection
**Branch**: `003-in-collection-context`

AST is the only data model for this library. All entities are immutable Java records in `ru.zahaand.dataexpr.ast`.

## Entity: `InListNode` (NEW)

```java
package ru.zahaand.dataexpr.ast;

import java.util.List;

public record InListNode(List<Expression> values) implements Expression {}
```

| Field    | Type                | Notes                                                       |
|----------|---------------------|-------------------------------------------------------------|
| `values` | `List<Expression>`  | Literal values from static `IN ('a', 'b', ...)` syntax. Each element is a literal AST node (`NumberNode`, `StringNode`, `BooleanNode`). Never null; empty is valid (reserved for future — grammar currently requires ≥1). |

Validation: none at construction (record-canonical).

## Entity: `InNode` (MODIFIED — record field rename)

```java
package ru.zahaand.dataexpr.ast;

public record InNode(
    Expression operand,
    Expression collection,
    boolean    negated
) implements Expression {}
```

| Field        | Type         | Notes                                                                 |
|--------------|--------------|-----------------------------------------------------------------------|
| `operand`    | `Expression` | Value being tested. Typically `FieldNode`, but any expression is legal per grammar (`additive IN ...`). |
| `collection` | `Expression` | Exactly one of `InListNode` (static) or `FieldNode` (dynamic). Enforced by grammar → visitor; not validated at record construction. |
| `negated`    | `boolean`    | `false` = `IN`, `true` = `NOT IN`.                                    |

### State transitions

None. Records are immutable.

### Invariants

- `operand != null` (grammar guarantees)
- `collection != null` (grammar guarantees)
- `collection instanceof InListNode` **or** `collection instanceof FieldNode` (visitor guarantees; not asserted)

## Entity: `Expression` (MODIFIED — sealed permits)

```java
public sealed interface Expression
    permits FieldNode, NumberNode, StringNode, BooleanNode,
            BinaryOpNode, UnaryMinusNode, FunctionCallNode,
            ComparisonNode, LogicalNode, NotNode, InNode, InListNode {}
```

Only change: `InListNode` appended to permits.

## Entity flow (read-only)

```text
  DataExpression.g4  →  ANTLR parse tree  →  AstBuildingVisitor  →  Expression tree
                                                                         │
                                                                         ▼
                                                                EvaluatingVisitor
                                                                         │
                                                                         ▼
                                                                 Result (Boolean)
```

`InNode` is produced only by `AstBuildingVisitor.visitComparison`; consumed only by `EvaluatingVisitor.visit(InNode)`.

## Evaluation context data

`EvaluationContext.of(Map<String, Object>)` — field values for `IN [field]` MUST be `List<Object>`. Elements MUST be `Number`, `String`, or `Boolean`. No change to `EvaluationContext` API surface.
