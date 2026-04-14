# Phase 1 — Quickstart

**Feature**: Dynamic IN / NOT IN Against Context Collection
**Version**: 1.2.0

## Setup

```xml
<dependency>
    <groupId>ru.zahaand</groupId>
    <artifactId>data-expression-parser-core</artifactId>
    <version>1.2.0</version>
</dependency>
```

Spring Boot users: use `data-expression-parser-spring-boot-starter:1.2.0` and inject `DataExpressionParser`.

## Example: dynamic IN against context collection

```java
DataExpressionParser parser = new DataExpressionParser(new ExpressionEvaluator());

Map<String, Object> ctx = new HashMap<>();
ctx.put("status", "active");
ctx.put("allowed_statuses", List.of("active", "trial", "grace"));

boolean ok = parser.evaluateBoolean(
    "[status] IN [allowed_statuses]",
    EvaluationContext.of(ctx)
);  // → true
```

## Example: NOT IN

```java
Map<String, Object> ctx = Map.of(
    "status",  "blocked",
    "allowed", List.of("active", "trial")
);

boolean ok = parser.evaluateBoolean(
    "[status] NOT IN [allowed]",
    EvaluationContext.of(ctx)
);  // → true
```

## Example: numeric collection

```java
Map<String, Object> ctx = Map.of(
    "code",         2.0,
    "valid_codes",  List.of(1.0, 2.0, 3.0)
);

boolean ok = parser.evaluateBoolean(
    "[code] IN [valid_codes]",
    EvaluationContext.of(ctx)
);  // → true
```

## Example: mixed with boolean / AND

```java
Map<String, Object> ctx = Map.of(
    "region",      "EU",
    "eu_regions",  List.of("EU", "UK"),
    "tier",        "premium"
);

boolean ok = parser.evaluateBoolean(
    "[region] IN [eu_regions] AND [tier] == 'premium'",
    EvaluationContext.of(ctx)
);  // → true
```

## Error scenarios

### Missing field

```java
parser.evaluateBoolean("[x] IN [missing]", EvaluationContext.of("x", 1.0));
// throws ExpressionEvaluationException: "Field 'missing' not found in context"
```

### Wrong type (not a List)

```java
Map<String, Object> ctx = Map.of("x", 1.0, "notAList", "hello");
parser.evaluateBoolean("[x] IN [notAList]", EvaluationContext.of(ctx));
// throws ExpressionEvaluationException:
//   "Field 'notAList' must be a List for IN operator, got: String"
```

### Unsupported element type

```java
Map<String, Object> ctx = Map.of(
    "x",   1.0,
    "bad", List.of(new Object())
);
parser.evaluateBoolean("[x] IN [bad]", EvaluationContext.of(ctx));
// throws ExpressionEvaluationException:
//   "Collection field 'bad' contains unsupported element type: Object"
```

## Backward compatibility

Static `IN ('a', 'b')` is unchanged:

```java
parser.evaluateBoolean(
    "[status] IN ('active', 'trial')",
    EvaluationContext.of("status", "active")
);  // → true (same as v1.0.0)
```

## Validate before evaluate

`validate()` accepts both syntactic forms; field existence is runtime-only:

```java
ValidationResult r = parser.validate("[x] IN [y]");
// r.isValid() → true (regardless of whether [y] exists at evaluation time)
```
