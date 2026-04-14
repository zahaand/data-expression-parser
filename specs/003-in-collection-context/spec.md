# Feature Specification: Dynamic IN / NOT IN Against Context Collection

**Branch**: `003-in-collection-context`
**Version**: v1.2.0
**Status**: Draft

## Overview

v1.2.0 extends the `IN` / `NOT IN` operator to support a field reference as the
right-hand side operand. Instead of a static literal list, the consumer can pass a
`List<Object>` as a field value in `EvaluationContext` and reference it directly:

```java
// v1.0.0 — static list only:
[status] IN ('active', 'trial')

// v1.2.0 — dynamic collection from context:
[status] IN [allowed_statuses]
// context: {"status": "active", "allowed_statuses": List.of("active", "trial")}
```

Both `IN [field]` and `NOT IN [field]` are supported.
Static list syntax (`IN ('a', 'b')`) remains unchanged.

## Clarifications

### Session 2026-04-14

- Q: When `IN [field]` is evaluated and the field does not exist in the `EvaluationContext`, what should happen? → A: Throw `ExpressionEvaluationException` with generic message `"Field '<name>' not found in context"`.
- Q: When the operand (LHS) of `IN`/`NOT IN` resolves to null (missing context field), what should happen? → A: Throw `ExpressionEvaluationException` via the existing field-resolution path — same behavior as any other unresolved field reference in the expression.

## Maven Coordinates

Parent version bumps to `1.2.0`. Module coordinates:
- `ru.zahaand:data-expression-parser:1.2.0` (pom)
- `ru.zahaand:data-expression-parser-core:1.2.0` (jar)
- `ru.zahaand:data-expression-parser-spring-boot-starter:1.2.0` (jar)

No new external dependencies.

## Grammar Changes

File: `data-expression-parser-core/src/main/antlr4/ru/zahaand/dataexpr/DataExpression.g4`

Extend the `comparison` rule to add two new alternatives for field-based IN:

```antlr
comparison : additive ( ( '>=' | '<=' | '>' | '<' | '==' | '!=' ) additive )?
           | additive IN     '(' valueList ')'
           | additive NOT IN '(' valueList ')'
           | additive IN     FIELD
           | additive NOT IN FIELD
           ;
```

No other grammar rules change. The `FIELD` token already exists in the lexer.

## AST Changes

### New node: `InListNode`

```java
package ru.zahaand.dataexpr.ast;

public record InListNode(List<Expression> values) implements Expression {}
```

Add `InListNode` to the `permits` clause of `Expression`:

```java
public sealed interface Expression
    permits FieldNode, NumberNode, StringNode, BooleanNode,
            BinaryOpNode, UnaryMinusNode, FunctionCallNode,
            ComparisonNode, LogicalNode, NotNode, InNode, InListNode {}
```

### Modified node: `InNode`

Replace the existing `InNode` record with a unified structure:

```java
public record InNode(
    Expression operand,       // the value being tested (e.g. FieldNode("status"))
    Expression collection,    // either InListNode (static) or FieldNode (dynamic)
    boolean negated           // false = IN, true = NOT IN
) implements Expression {}
```

This is a BREAKING CHANGE to the `InNode` record — existing fields
`field` and `values` are replaced by `operand` and `collection`.
`AstBuildingVisitor` and `EvaluatingVisitor` MUST be updated accordingly.

## Package Structure

```
data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/
└── ast/
    ├── InListNode.java     ← NEW
    ├── InNode.java         ← MODIFIED (new field names)
    └── Expression.java     ← MODIFIED (InListNode added to permits)
```

No new packages.

## Visitor Changes

### `AstBuildingVisitor`

Update `visitComparison` to handle the two new grammar alternatives:

- For `additive IN '(' valueList ')'`:
  → `InNode(operand, InListNode(values), negated=false)`
- For `additive NOT IN '(' valueList ')'`:
  → `InNode(operand, InListNode(values), negated=true)`
- For `additive IN FIELD`:
  → strip brackets from FIELD token text
  → `InNode(operand, FieldNode(fieldName), negated=false)`
- For `additive NOT IN FIELD`:
  → strip brackets from FIELD token text
  → `InNode(operand, FieldNode(fieldName), negated=true)`

### `EvaluatingVisitor`

Update the `InNode` evaluation branch:

```
case InNode node:
  Object operandValue = resolveValue(node.operand())

  if node.collection() instanceof InListNode listNode:
    // existing static list logic — unchanged
    for each valueExpr in listNode.values():
      if isEqual(operandValue, resolveValue(valueExpr)): found = true

  else if node.collection() instanceof FieldNode fieldNode:
    // new dynamic collection logic
    Object raw = context.get(fieldNode.fieldName())
    if raw is not a List:
      throw ExpressionEvaluationException(
        "Field '" + fieldNode.fieldName() + "' must be a List for IN operator, got: " + raw.getClass().getSimpleName()
      )
    for each item in (List) raw:
      if item is not Number, String, or Boolean:
        throw ExpressionEvaluationException(
          "Collection field '" + fieldNode.fieldName() + "' contains unsupported element type: " + item.getClass().getSimpleName()
        )
      if isEqual(operandValue, item): found = true

  return BooleanResult(node.negated() != found)
```

Element comparison uses the existing `isEqual()` method — no changes needed there.
Numeric coercion via `Number.doubleValue()` already applies through `isEqual()`.

**Missing key handling**: `context.get(fieldName)` throws
`ExpressionEvaluationException("Unknown field: '<n>'")` for absent keys.
The dynamic IN path does NOT catch this exception — it propagates directly
to the caller (FR-204b). Only non-List type errors are caught and rethrown
with the FR-204 message.

**Logging format (Constitution V — NON-NEGOTIABLE):**
Before each `ExpressionEvaluationException` throw in the IN operator path,
log at ERROR with the same message as the exception:

```
log.error("IN operator error for field '{}': {}", fieldName, exceptionMessage)
```

This ensures log content and exception message never drift.

## EvaluationContext Changes

`EvaluationContext` MUST accept `List<Object>` as a valid field value.
Currently `of(String, Object)` accepts any non-null `Object` — this already includes
`List`. No code change required; document explicitly as supported.

Add to spec/javadoc: field values of type `List<Object>` are valid and used
by the `IN [field]` operator.

## Exception Contracts (Additions)

`ExpressionEvaluationException` — thrown by `EvaluatingVisitor` when:
- The field referenced in `IN [field]` is absent from the context:
  `"Field '<name>' not found in context"`
- The field referenced in `IN [field]` resolves to a non-`List` value:
  `"Field '<name>' must be a List for IN operator, got: <type>"`
- The collection contains an element that is `null` or not `Number`, `String`, or `Boolean`:
  `"Collection field '<name>' contains unsupported element type: null"` (for `null`)
  or `"Collection field '<name>' contains unsupported element type: <type>"` (for unsupported type).

## Functional Requirements

- **FR-201**: System MUST support `[operand] IN [field]` syntax where `[field]`
  resolves to a `List<Object>` from `EvaluationContext`.
- **FR-202**: System MUST support `[operand] NOT IN [field]` with the same semantics,
  returning the negated result.
- **FR-203**: Each element in the collection field is compared to the operand using
  the existing `isEqual()` semantics (numeric coercion, string equality, boolean equality,
  mixed-type returns false).
- **FR-204**: If the field referenced in `IN [field]` resolves to a non-`List` value,
  `ExpressionEvaluationException` MUST be thrown with message:
  `"Field '<name>' must be a List for IN operator, got: <type>"`.
- **FR-204a**: If the field referenced in `IN [field]` is absent from the context
  (`context.get(name) == null` with no entry), `ExpressionEvaluationException` MUST
  be thrown with message: `"Field '<name>' not found in context"`.
- **FR-204b**: If the operand (LHS) of `IN`/`NOT IN` is a field reference that
  resolves to null (missing in context), the existing field-resolution path MUST
  throw — no special-case handling is introduced for the IN operator.
- **FR-205**: If any element in the collection is not `Number`, `String`, or `Boolean`,
  `ExpressionEvaluationException` MUST be thrown with message:
  `"Collection field '<name>' contains unsupported element type: <type>"`.
- **FR-205a**: A `null` element in the collection field MUST be treated as an
  unsupported element type and throw `ExpressionEvaluationException` with message:
  `"Collection field '<name>' contains unsupported element type: null"`.
- **FR-206**: Static `IN ('a', 'b')` syntax MUST continue to work unchanged.
- **FR-207**: `InNode` is refactored to use `operand` and `collection` fields.
  `InListNode` is introduced as the collection type for static lists.
- **FR-208**: All v1.0.0 and v1.1.0 public API contracts MUST remain unchanged.
  `InNode` record field rename is an internal AST change — AST is not part of the
  public API contract.
- **FR-209**: `EvaluationContext.of()` already accepts `List<Object>` as a valid
  value (non-null `Object`). No API change required.
- **FR-210**: Logging: `EvaluatingVisitor` MUST log at ERROR before throwing
  `ExpressionEvaluationException` for collection-related errors (Constitution V).
- **FR-210a**: The ERROR log message in the IN operator path MUST use the format:
  `"IN operator error for field '<name>': <exception message>"`
  so that log content and exception message are always consistent.

## Success Criteria

- **SC-201**: `mvn compile` passes with no errors in both modules.
- **SC-202**: `mvn test` passes all existing tests plus new tests.
- **SC-203**: `evaluateBoolean("[status] IN [allowed]", ctx)` where
  `ctx = {status: "active", allowed: List.of("active", "trial")}` returns `true`.
- **SC-204**: `evaluateBoolean("[status] NOT IN [allowed]", ctx)` where
  `ctx = {status: "blocked", allowed: List.of("active", "trial")}` returns `true`.
- **SC-205**: `evaluateBoolean("[code] IN [valid_codes]", ctx)` where
  `ctx = {code: 2.0, valid_codes: List.of(1.0, 2.0, 3.0)}` returns `true`.
- **SC-206**: `IN [field]` where field resolves to a `String` (not a `List`) throws
  `ExpressionEvaluationException`.
- **SC-207**: Static `[status] IN ('active', 'trial')` continues to return correct
  results unchanged.

## Testing Requirements

All new tests in `data-expression-parser-core/src/test/java/ru/zahaand/dataexpr/`.

### `InCollectionTest` — new test class

`@Nested` group `EvaluateBoolean`:
- `shouldReturnTrueWhenFieldValueIsInStringCollection`
- `shouldReturnFalseWhenFieldValueIsNotInStringCollection`
- `shouldReturnTrueWhenFieldValueIsInNumericCollection`
- `shouldReturnFalseWhenFieldValueIsNotInNumericCollection`
- `shouldReturnTrueForNotInWhenValueAbsent`
- `shouldReturnFalseForNotInWhenValuePresent`
- `shouldHandleMixedTypeCollectionWithNoMatch` — collection contains strings and numbers; operand doesn't match any
- `shouldReturnFalseWhenCollectionIsEmpty`

`@Nested` group `EvaluateBooleanParameterized`:
- `shouldEvaluateInCollectionExpression` — `@ParameterizedTest` covering positive and negative cases for both `IN` and `NOT IN` with string and numeric collections

`@Nested` group `Errors`:
- `shouldThrowWhenCollectionFieldIsNotAList`
- `shouldThrowWhenCollectionContainsUnsupportedElementType`
- `shouldThrowWhenCollectionFieldDoesNotExist`

### `DataExpressionParserTest` — additions

Add to existing `Parse` group:
- `shouldReturnInNodeWithFieldNodeCollectionForDynamicIn`
- `shouldReturnInNodeWithFieldNodeCollectionForDynamicNotIn`
- `shouldReturnInNodeWithInListNodeCollectionForStaticIn` — verify static IN still produces `InListNode`

## Edge Cases

- Self-referential expressions (`[a] IN [a]` where `a = "active"`) are not
  special-cased. The right-hand field is expected to be a `List`; if it resolves
  to a non-`List` value, `ExpressionEvaluationException` is thrown per FR-204.

## Assumptions

- `List<Object>` values in `EvaluationContext` are provided by the consumer before
  evaluation; the library does not validate list contents at context-construction time.
- Nested lists (`List<List<...>>`) are not supported; elements must be scalar.
- Empty collections (`List.of()`) are valid; `IN []` always returns `false`,
  `NOT IN []` always returns `true`.
- Duplicate elements in the collection field are permitted and have no special
  handling. The `isEqual()` check stops at the first match, so duplicates do not
  affect the result.
- The `InNode` record field rename (`field`→`operand`, `values`→`collection`) is an
  internal AST change. Since AST types are public records, consumers directly
  instantiating `InNode` will need to update — this is an acceptable breaking change
  for a minor version given AST is implementation detail, not stable API.
