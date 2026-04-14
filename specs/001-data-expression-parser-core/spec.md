# Feature Specification: data-expression-parser

**Feature Branch**: `001-data-expression-parser-core`
**Created**: 2026-04-13
**Status**: Draft
**Input**: User description: "Reusable Java library that parses and evaluates business expressions over named data fields"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Parse Expression to AST (Priority: P1)

A developer passes a business expression string (e.g. `[age] > 18 AND [status] == 'active'`)
to the library and receives a structured AST (`Expression` tree) that can be inspected,
serialized, or evaluated later.

**Why this priority**: Parsing is the foundational capability — evaluation, validation,
and all downstream features depend on a correct AST.

**Independent Test**: Can be fully tested by calling `DataExpressionParser.parse()` with
various expression strings and asserting the returned AST node types and structure.

**Acceptance Scenarios**:

1. **Given** a valid arithmetic expression `[price] * 1.2`, **When** `parse()` is called,
   **Then** a `BinaryOpNode(FieldNode("price"), MULTIPLY, NumberNode(1.2))` is returned.
2. **Given** a valid logical expression `[age] > 18 AND [status] == 'active'`, **When**
   `parse()` is called, **Then** a `LogicalNode` with `AND` operator containing two
   `ComparisonNode` children is returned.
3. **Given** a malformed expression `[age] >`, **When** `parse()` is called, **Then**
   `ExpressionParseException` is thrown.
4. **Given** a `null` input, **When** `parse()` is called, **Then**
   `ExpressionParseException` is thrown.

---

### User Story 2 - Evaluate Expression to Boolean (Priority: P1)

A developer evaluates a boolean expression against a field context and receives a `true`
or `false` result. This is the primary use case — filtering or validating data rows
against business rules.

**Why this priority**: Boolean evaluation is the core business value of the library.

**Independent Test**: Can be fully tested by calling `evaluateBoolean()` with expressions
and an `EvaluationContext` containing test field values.

**Acceptance Scenarios**:

1. **Given** expression `[age] > 18 AND [status] == 'active'` and context
   `{age: 25.0, status: "active"}`, **When** `evaluateBoolean()` is called,
   **Then** `true` is returned.
2. **Given** expression `[role] IN ('admin', 'moderator')` and context
   `{role: "admin"}`, **When** `evaluateBoolean()` is called,
   **Then** `true` is returned.
3. **Given** expression `NOT [disabled]` and context `{disabled: false}`,
   **When** `evaluateBoolean()` is called, **Then** `true` is returned.
4. **Given** expression `[age] > 18` and context `{}` (field missing),
   **When** `evaluateBoolean()` is called, **Then** `ExpressionEvaluationException` is thrown.

---

### User Story 3 - Evaluate Expression to Double (Priority: P2)

A developer evaluates an arithmetic expression against a field context and receives a
numeric result. Useful for computed columns, pricing formulas, scoring.

**Why this priority**: Arithmetic evaluation extends the library beyond boolean filtering
into computed values — important but secondary to the core filtering use case.

**Independent Test**: Can be fully tested by calling `evaluateDouble()` with arithmetic
expressions and an `EvaluationContext` containing numeric field values.

**Acceptance Scenarios**:

1. **Given** expression `[price] * (1 + [tax_rate])` and context
   `{price: 100.0, tax_rate: 0.2}`, **When** `evaluateDouble()` is called,
   **Then** `120.0` is returned.
2. **Given** expression `abs([balance])` and context `{balance: -50.0}`,
   **When** `evaluateDouble()` is called, **Then** `50.0` is returned.
3. **Given** expression `[x] / 0` and context `{x: 10.0}`,
   **When** `evaluateDouble()` is called, **Then** `ExpressionEvaluationException` is thrown.

---

### User Story 4 - Spring Boot Auto-injection (Priority: P3)

A Spring Boot application adds `data-expression-parser-spring-boot-starter` as a Maven
dependency and receives `DataExpressionParser` as a ready-to-use singleton bean via
autoconfiguration — no manual `@Bean` definitions required.

**Why this priority**: Starter convenience is a distribution concern; the core library
works without Spring. This is the final integration layer.

**Independent Test**: Can be tested by creating a minimal Spring Boot application with
the starter dependency and verifying that `DataExpressionParser` is injectable and functional.

**Acceptance Scenarios**:

1. **Given** a Spring Boot application with the starter on the classpath, **When** the
   application context starts, **Then** `DataExpressionParser` and `ExpressionEvaluator`
   beans are available for injection.
2. **Given** a consumer defines their own `DataExpressionParser` bean, **When** the
   application context starts, **Then** the autoconfigured bean is not created
   (`@ConditionalOnMissingBean`).

---

### Edge Cases

- What happens when a field name contains spaces or special characters? → Supported via
  `[field name]` syntax; brackets allow any character except `]` and newline.
- How does the system handle `NOT IN` vs `IN`? → `NOT IN` is two tokens; `AstBuildingVisitor`
  detects the combination and sets `negated = true` on `InNode`.
- What happens with power operator associativity? → `2 ^ 3 ^ 2` evaluates as `2 ^ (3 ^ 2)`
  (right-associative), producing `512.0`.
- What happens with `**` (double-star) vs `*` (multiply)? → `**` is matched as two
  consecutive `STAR` tokens in the parser rule, not as a dedicated lexer token.
- What happens with case-insensitive keywords? → `TRUE`, `True`, `true` all parse to
  `BooleanNode(true)`. `AND`, `And`, `and` all parse as logical AND.
- What happens with case-sensitive field names? → `[Age]` and `[age]` reference different
  fields in `EvaluationContext`.
- What happens with NaN/Infinity from math? → Propagates silently, matching `java.lang.Math`
  semantics.

## Clarifications

### Session 2026-04-13

- Q: How should the evaluator handle non-Double numeric types (Integer, Long, BigDecimal) in EvaluationContext? → A: Accept any `Number` subtype, coerce to `double` via `Number.doubleValue()`.
- Q: Should ordering operators (>, <, >=, <=) support string operands? → A: No. Ordering operators are numeric-only; throw `ExpressionEvaluationException` on string operands.
- Q: What happens when `==` or `!=` compares operands of different types (e.g., number vs string)? → A: Mixed-type `==` returns `false`; mixed-type `!=` returns `true` (different types are never equal).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST parse expression strings into a typed AST (`Expression` tree).
- **FR-002**: System MUST evaluate an AST against an `EvaluationContext` and produce an
  `EvaluationResult` (either `DoubleResult` or `BooleanResult`).
- **FR-003**: System MUST support arithmetic operators: `+`, `-`, `*`, `/`, `%`, `^`, `**`.
- **FR-004**: System MUST support comparison operators: `>`, `<`, `>=`, `<=`, `==`, `!=`.
- **FR-005**: System MUST support logical operators: `AND`, `OR`, `NOT` (case-insensitive).
- **FR-006**: System MUST support `IN` and `NOT IN` operators with literal value lists.
- **FR-007**: System MUST support field references via `[field_name]` syntax.
- **FR-008**: System MUST support string literals (`'value'`), numeric literals, and
  boolean literals (`true`/`false`, case-insensitive).
- **FR-009**: System MUST support built-in functions: `abs`, `round`, `floor`, `ceil`,
  `min`, `max`, `pow` — resolved case-insensitively.
- **FR-010**: System MUST enforce correct operator precedence:
  primary > unary > power > multiplicative > additive > comparison > NOT > AND > OR.
- **FR-011**: System MUST enforce right-associativity for the power operator.
- **FR-012**: System MUST throw `ExpressionParseException` for syntax errors, null, or
  blank input.
- **FR-013**: System MUST throw `ExpressionEvaluationException` for runtime errors
  (unknown field, division by zero, unknown function, wrong argument count, type mismatch).
- **FR-014**: `DataExpressionParser` MUST be stateless and thread-safe.
- **FR-015**: The `data-expression-parser-core` module MUST NOT depend on Spring.
- **FR-016**: The `data-expression-parser-spring-boot-starter` module MUST provide
  autoconfigured beans with `@ConditionalOnMissingBean`.

### Key Entities

- **Expression**: Sealed interface — the root of the AST type hierarchy. All AST nodes
  are records implementing this interface.
- **EvaluationContext**: Immutable map of field names to runtime values. Case-sensitive
  field lookup. Throws on missing field.
- **EvaluationResult**: Sealed interface with two permitted implementations:
  `DoubleResult` and `BooleanResult`.
- **DataExpressionParser**: Public API entry point — parse, evaluate, and convenience methods.
- **ExpressionEvaluator**: Evaluates a pre-parsed AST against a context.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `mvn compile` passes with no errors in both modules.
- **SC-002**: ANTLR plugin generates `DataExpressionLexer.java` and
  `DataExpressionParser.java` under `target/generated-sources/antlr4`.
- **SC-003**: `mvn test` passes all tests in `data-expression-parser-core`.
- **SC-004**: A consumer project can inject `DataExpressionParser` and evaluate
  `[age] > 18 AND [status] == 'active'` with context `{age: 25.0, status: "active"}`
  returning `true`.
- **SC-005**: `data-expression-parser-core` compiles and runs without Spring on the classpath.

## Assumptions

- Consumers use Spring Boot 3.5.x with Java 21+.
- Field values in `EvaluationContext` are numeric (`Number` subtypes, coerced to `double`),
  `String`, or `Boolean` — no complex types or collections.
- The grammar is not expected to support user-defined functions in v1.0.0 — only the
  7 built-in functions listed in the specification.
- Parenthesized expressions are supported for grouping but not for tuples or multi-value
  returns.
- The library does not perform any I/O — it is a pure computation library.
- Expression depth and length are not limited in v1.0.0. The library is intended
  for developer-controlled business expressions. Pathological inputs (e.g. thousands
  of nested parentheses) may cause `StackOverflowError` and are out of scope.

---

## Technical Specification

### Maven Module Structure

#### Parent: `data-expression-parser`
```
groupId:    ru.zahaand
artifactId: data-expression-parser
version:    1.0.0
packaging:  pom
```
Declares `dependencyManagement` for all dependency versions.
Child modules: `data-expression-parser-core`, `data-expression-parser-spring-boot-starter`.

#### Module 1: `data-expression-parser-core`
```
artifactId: data-expression-parser-core
packaging:  jar
```
Pure Java — no Spring on the compile or runtime classpath.

Dependencies:
- `org.antlr:antlr4-runtime:4.13.2` (compile)
- `org.apache.commons:commons-lang3` (compile)
- `org.junit.jupiter:junit-jupiter` (test)
- `org.assertj:assertj-core` (test)
- `org.mockito:mockito-core` (test)

Build plugins:
- `org.antlr:antlr4-maven-plugin:4.13.2` — generates lexer/parser Java sources from
  `src/main/antlr4/ru/zahaand/dataexpr/DataExpression.g4` into
  `target/generated-sources/antlr4` during the `generate-sources` phase.

#### Module 2: `data-expression-parser-spring-boot-starter`
```
artifactId: data-expression-parser-spring-boot-starter
packaging:  jar
```
Dependencies:
- `ru.zahaand:data-expression-parser-core:1.0.0` (compile)
- `org.springframework.boot:spring-boot-autoconfigure` (compile)
- `org.springframework.boot:spring-boot-starter` (compile)

Autoconfiguration entry point:
`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
contains one line:
`ru.zahaand.dataexpr.autoconfigure.DataExpressionParserAutoConfiguration`

---

### Package Structure (`data-expression-parser-core`)

```
ru.zahaand.dataexpr
├── ast/
│   ├── Expression.java              sealed interface
│   ├── FieldNode.java               record
│   ├── NumberNode.java              record
│   ├── StringNode.java              record
│   ├── BooleanNode.java             record
│   ├── BinaryOpNode.java            record
│   ├── UnaryMinusNode.java          record
│   ├── FunctionCallNode.java        record
│   ├── ComparisonNode.java          record
│   ├── LogicalNode.java             record
│   ├── NotNode.java                 record
│   ├── InNode.java                  record
│   ├── ArithmeticOperator.java      enum
│   ├── ComparisonOperator.java      enum
│   └── LogicalOperator.java         enum
├── parser/
│   └── DataExpressionParser.java    public API
├── evaluator/
│   ├── ExpressionEvaluator.java
│   ├── EvaluationContext.java
│   ├── EvaluationResult.java        sealed interface
│   ├── DoubleResult.java            record
│   └── BooleanResult.java           record
├── visitor/
│   ├── AstBuildingVisitor.java
│   └── EvaluatingVisitor.java
├── function/
│   └── BuiltinFunctionRegistry.java
└── exception/
    ├── ExpressionParseException.java
    └── ExpressionEvaluationException.java
```

Package structure (`data-expression-parser-spring-boot-starter`):
```
ru.zahaand.dataexpr.autoconfigure
└── DataExpressionParserAutoConfiguration.java
```

---

### ANTLR Grammar

File: `data-expression-parser-core/src/main/antlr4/ru/zahaand/dataexpr/DataExpression.g4`

Grammar name: `DataExpression`

```antlr
grammar DataExpression;

// ─── Entry ───────────────────────────────────────────────────────────────────
prog : expr EOF ;

// ─── Logical (lowest precedence) ─────────────────────────────────────────────
expr       : orExpr ;
orExpr     : andExpr   ( OR  andExpr   )* ;
andExpr    : notExpr   ( AND notExpr   )* ;
notExpr    : NOT notExpr
           | comparison
           ;

// ─── Comparison ──────────────────────────────────────────────────────────────
comparison : additive ( ( '>=' | '<=' | '>' | '<' | '==' | '!=' ) additive )?
           | additive IN     '(' valueList ')'
           | additive NOT IN '(' valueList ')'
           ;

valueList  : literal (',' literal)* ;

// ─── Arithmetic ──────────────────────────────────────────────────────────────
additive       : multiplicative ( ('+' | '-') multiplicative )* ;
multiplicative : power          ( ('*' | '/' | '%') power    )* ;
power          : unary          ( ('^' | STAR STAR) unary    )* ;
unary          : '-' unary
               | primary
               ;

// ─── Primary (highest precedence) ────────────────────────────────────────────
primary    : FIELD                        // [column name]
           | literal
           | ID '(' argList? ')'          // abs([x]), max([a], [b])
           | '(' expr ')'
           ;

argList    : expr (',' expr)* ;

literal    : NUMBER
           | STRING
           | TRUE
           | FALSE
           ;

// ─── Lexer ───────────────────────────────────────────────────────────────────
// Reserved words MUST appear above the ID rule
TRUE   : [Tt][Rr][Uu][Ee] ;
FALSE  : [Ff][Aa][Ll][Ss][Ee] ;
AND    : [Aa][Nn][Dd] ;
OR     : [Oo][Rr] ;
NOT    : [Nn][Oo][Tt] ;
IN     : [Ii][Nn] ;

FIELD  : '[' ~[\]\n]+ ']' ;
STRING : '\'' ~['\\]* '\'' ;
NUMBER : [0-9]+ ('.' [0-9]+)? ;
ID     : [a-zA-Z_][a-zA-Z_0-9]* ;
STAR   : '*' ;
WS     : [ \t\r\n]+ -> skip ;
```

**Critical grammar notes:**
- `**` (double-star power) is matched as two consecutive `STAR` tokens in the parser rule,
  NOT as a dedicated lexer token. This avoids conflict with the `*` (multiply) operator.
- Reserved words (`TRUE`, `FALSE`, `AND`, `OR`, `NOT`, `IN`) are case-insensitive via
  character alternatives and MUST appear above the `ID` rule in the lexer.
- `NOT IN` is two tokens (`NOT` followed by `IN`). `AstBuildingVisitor` MUST detect
  this combination and set `negated = true` on `InNode`.
- Function names (`abs`, `round`, etc.) are matched by the `ID` rule and resolved
  case-insensitively at evaluation time via `BuiltinFunctionRegistry`.
- Variable names in `EvaluationContext` are case-sensitive.
- Field content inside `[...]` allows any characters except `]` and newline,
  enabling real database column names including spaces and special characters.

---

### AST Node Contracts

#### `Expression` — sealed interface
```java
package ru.zahaand.dataexpr.ast;

public sealed interface Expression
    permits FieldNode, NumberNode, StringNode, BooleanNode,
            BinaryOpNode, UnaryMinusNode, FunctionCallNode,
            ComparisonNode, LogicalNode, NotNode, InNode {}
```

#### Literal nodes
```java
public record FieldNode(String fieldName)      implements Expression {}
public record NumberNode(double value)         implements Expression {}
public record StringNode(String value)         implements Expression {}
public record BooleanNode(boolean value)       implements Expression {}
```

#### Arithmetic nodes
```java
public record BinaryOpNode(
    Expression left,
    ArithmeticOperator op,
    Expression right
) implements Expression {}

public record UnaryMinusNode(Expression operand) implements Expression {}

public record FunctionCallNode(
    String name,
    List<Expression> args
) implements Expression {}
```

#### Comparison node
```java
public record ComparisonNode(
    Expression left,
    ComparisonOperator op,
    Expression right
) implements Expression {}
```

#### Logical nodes
```java
public record LogicalNode(
    Expression left,
    LogicalOperator op,
    Expression right
) implements Expression {}

public record NotNode(Expression operand) implements Expression {}
```

#### IN node
```java
public record InNode(
    Expression field,
    List<Expression> values,
    boolean negated       // false = IN, true = NOT IN
) implements Expression {}
```

#### Operator enums
```java
public enum ArithmeticOperator { ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO, POWER }
public enum ComparisonOperator  { GT, LT, GTE, LTE, EQ, NEQ }
public enum LogicalOperator     { AND, OR }
```

---

### Evaluation Result

```java
package ru.zahaand.dataexpr.evaluator;

public sealed interface EvaluationResult
    permits DoubleResult, BooleanResult {}

public record DoubleResult(double value)   implements EvaluationResult {}
public record BooleanResult(boolean value) implements EvaluationResult {}
```

---

### Public API

#### `EvaluationContext`
```java
package ru.zahaand.dataexpr.evaluator;

public final class EvaluationContext {

    // Factory methods
    public static EvaluationContext empty() { ... }
    public static EvaluationContext of(String name, Object value) { ... }
    public static EvaluationContext of(Map<String, Object> fields) { ... }

    // Lookup — throws ExpressionEvaluationException if field not found
    public Object get(String fieldName) { ... }
}
```

Field names in `EvaluationContext` are case-sensitive.

`of(String, Object)` and `of(Map<String, Object>)` MUST reject `null` values
by throwing `ExpressionEvaluationException` with message:
`"Field value must not be null for field: '<fieldName>'"`.
`empty()` is always valid.

#### `DataExpressionParser`
```java
package ru.zahaand.dataexpr.parser;

public final class DataExpressionParser {

    // Parse expression string → AST
    // Throws ExpressionParseException on syntax error
    public Expression parse(String expression) { ... }

    // Parse + evaluate in one step
    // Throws ExpressionParseException or ExpressionEvaluationException
    public EvaluationResult evaluate(String expression, EvaluationContext context) { ... }

    // Convenience: evaluate and unwrap as boolean
    // Throws ExpressionEvaluationException if result is not BooleanResult
    public boolean evaluateBoolean(String expression, EvaluationContext context) { ... }

    // Convenience: evaluate and unwrap as double
    // Throws ExpressionEvaluationException if result is not DoubleResult
    public double evaluateDouble(String expression, EvaluationContext context) { ... }
}
```

Thread safety: `DataExpressionParser` is stateless. A new `AstBuildingVisitor` instance
MUST be created on every `parse()` call. The class is safe for use as a Spring singleton.

#### `ExpressionEvaluator`
```java
package ru.zahaand.dataexpr.evaluator;

public final class ExpressionEvaluator {

    // Evaluate a pre-parsed AST
    // Throws ExpressionEvaluationException on runtime error
    public EvaluationResult evaluate(Expression expression, EvaluationContext context) { ... }
}
```

---

### Visitor Contracts

#### `AstBuildingVisitor`
- Extends ANTLR-generated `DataExpressionBaseVisitor<Expression>`.
- Converts each Parse Tree node into the corresponding AST record.
- Power operator (`^` and `STAR STAR`) MUST be handled right-associatively:
  `a ^ b ^ c` → `BinaryOpNode(a, POWER, BinaryOpNode(b, POWER, c))`.
- `NOT IN` MUST produce `InNode(..., negated = true)`.
- `IN` MUST produce `InNode(..., negated = false)`.
- String literal values MUST have surrounding single quotes stripped:
  `'active'` → `StringNode("active")`.
- Field names MUST have surrounding brackets stripped:
  `[first name]` → `FieldNode("first name")`.
- This class is package-private — not part of the public API.
- Parse error format: `ExpressionParseException` message MUST include
  the ANTLR position in the format:
  `"Parse error at line <L>:<C>: <antlr_message>"`
  Example: `"Parse error at line 1:5: mismatched input '>' expecting {...}"`

#### `EvaluatingVisitor`
- Uses `switch` pattern matching on the sealed `Expression` interface.
- Arithmetic operations apply only to numeric values. Any `Number` subtype
  (`Integer`, `Long`, `BigDecimal`, etc.) is coerced to `double` via `Number.doubleValue()`.
  If a field resolves to a non-numeric value in an arithmetic context →
  throws `ExpressionEvaluationException`.
- Boolean values in arithmetic context (`BooleanNode` or a field resolving to
  `Boolean`) MUST throw `ExpressionEvaluationException`.
  Example: `[is_active] + 1` where `is_active = true` → throws.
  No Java-style `true → 1` coercion is performed.
- Comparison `==` and `!=` support both numeric and string operands.
  Mixed-type comparison (e.g., number vs string): `==` returns `false`, `!=` returns `true`.
- Ordering operators (`>`, `<`, `>=`, `<=`) are numeric-only. If either operand
  resolves to a non-numeric value → throws `ExpressionEvaluationException`.
- `IN` / `NOT IN`: compares field value against each list value using `.equals()`.
- Division by zero → throws `ExpressionEvaluationException`.
- Unknown field → delegates to `EvaluationContext.get()` which throws
  `ExpressionEvaluationException`.
- Unknown function → throws `ExpressionEvaluationException`.
- NaN and Infinity from math operations propagate silently (match `java.lang.Math` semantics).
  NaN comparison semantics follow IEEE 754: `NaN > x`, `NaN < x`, `NaN == x`
  all return `false`; `NaN != x` returns `true`. No special handling required —
  this is the default behavior of Java `double` comparisons.
- This class is package-private — not part of the public API.

---

### Built-in Functions

Registry: `BuiltinFunctionRegistry` — package-private utility class,
`private` no-arg constructor, static methods only.
Function name lookup is case-insensitive (normalize to lowercase before lookup).

| Function    | Args | Behaviour                 |
|-------------|------|---------------------------|
| `abs(x)`    | 1    | `Math.abs(x)`             |
| `round(x)`  | 1    | `(double) Math.round(x)`  |
| `floor(x)`  | 1    | `Math.floor(x)`           |
| `ceil(x)`   | 1    | `Math.ceil(x)`            |
| `min(x, y)` | 2    | `Math.min(x, y)`          |
| `max(x, y)` | 2    | `Math.max(x, y)`          |
| `pow(x, y)` | 2    | `Math.pow(x, y)`          |

Wrong argument count → throws `ExpressionEvaluationException`:
`"Function '<name>' expects <N> argument(s) but got <M>"`

Unknown function name → throws `ExpressionEvaluationException`:
`"Unknown function: '<name>'"`

---

### Exception Contracts

#### `ExpressionParseException`
```java
package ru.zahaand.dataexpr.exception;

public class ExpressionParseException extends RuntimeException {
    public ExpressionParseException(String message) { super(message); }
    public ExpressionParseException(String message, Throwable cause) { super(message, cause); }
}
```
Thrown when: ANTLR reports a syntax error, null or blank input.

#### `ExpressionEvaluationException`
```java
package ru.zahaand.dataexpr.exception;

public class ExpressionEvaluationException extends RuntimeException {
    public ExpressionEvaluationException(String message) { super(message); }
    public ExpressionEvaluationException(String message, Throwable cause) { super(message, cause); }
}
```
Thrown when: unknown field, division by zero, unknown function, wrong argument count,
type mismatch (e.g. arithmetic on a string field), wrong result type in convenience methods.

---

### Spring Autoconfiguration

#### `DataExpressionParserAutoConfiguration`
```java
package ru.zahaand.dataexpr.autoconfigure;

@AutoConfiguration
public class DataExpressionParserAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ExpressionEvaluator expressionEvaluator() {
        return new ExpressionEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataExpressionParser dataExpressionParser(ExpressionEvaluator evaluator) {
        return new DataExpressionParser(evaluator);
    }
}
```

Both beans MUST use `@ConditionalOnMissingBean` to allow consumer overrides.

---

### Testing Requirements

All tests reside in `data-expression-parser-core/src/test/java/ru/zahaand/dataexpr/`.

#### `DataExpressionParserTest`

- `Parse` — verifies AST structure:
  - `shouldReturnFieldNodeWhenExpressionIsFieldReference`
  - `shouldReturnNumberNodeWhenExpressionIsNumericLiteral`
  - `shouldReturnStringNodeWhenExpressionIsStringLiteral`
  - `shouldReturnBooleanNodeWhenExpressionIsTrueLiteral`
  - `shouldReturnBooleanNodeWhenExpressionIsFalseLiteral`
  - `shouldParseTrueAndFalseCaseInsensitively` — `@ParameterizedTest`: TRUE, True, true, FALSE
  - `shouldReturnBinaryOpNodeForArithmetic`
  - `shouldRespectArithmeticPrecedence`
  - `shouldReturnComparisonNodeForGreaterThan`
  - `shouldReturnLogicalNodeForAndExpression`
  - `shouldReturnNotNodeForNotExpression`
  - `shouldReturnInNodeForInExpression`
  - `shouldReturnInNodeWithNegatedTrueForNotIn`
  - `shouldHandleFieldNamesWithSpaces`
  - `shouldHandleRightAssociativityForPower`

- `EvaluateBoolean` — verifies boolean results:
  - `shouldReturnTrueWhenFieldGreaterThanLiteral`
  - `shouldReturnFalseWhenFieldNotMatchingString`
  - `shouldEvaluateAndExpression`
  - `shouldEvaluateOrExpression`
  - `shouldEvaluateNotExpression`
  - `shouldEvaluateInExpression`
  - `shouldEvaluateNotInExpression`
  - `shouldEvaluateComplexCondition` — e.g. `[age] > 18 AND [status] == 'active'`
  - `shouldReturnFalseWhenEqualityComparesFieldOfDifferentTypes` — e.g. `[age] == 'thirty'` where `age = 25.0` → `false`
  - `shouldReturnTrueWhenInequalityComparesFieldOfDifferentTypes` — e.g. `[age] != 'thirty'` where `age = 25.0` → `true`

- `EvaluateDouble` — verifies numeric results:
  - `shouldEvaluateArithmeticOverFields`
  - `shouldEvaluateFunctionCall`
  - `shouldEvaluateModuloOperator`
  - `shouldCoerceIntegerFieldToDouble` — context contains `Integer` value; arithmetic returns correct `double`
  - `shouldCoerceLongFieldToDouble` — context contains `Long` value; arithmetic returns correct `double`
  - `shouldCoerceBigDecimalFieldToDouble` — context contains `BigDecimal` value; arithmetic returns correct `double`

- `Errors` — verifies exception throwing:
  - `shouldThrowParseExceptionWhenExpressionIsMalformed`
  - `shouldThrowParseExceptionWhenInputIsNull`
  - `shouldThrowParseExceptionWhenInputIsBlank`
  - `shouldThrowEvaluationExceptionWhenFieldNotInContext`
  - `shouldThrowEvaluationExceptionWhenDivisionByZero`
  - `shouldThrowEvaluationExceptionWhenUnknownFunction`
  - `shouldThrowEvaluationExceptionWhenArithmeticOnStringField`
  - `shouldThrowEvaluationExceptionWhenEvaluateBooleanCalledOnDoubleResult`
  - `shouldThrowEvaluationExceptionWhenEvaluateDoubleCalledOnBooleanResult`
  - `shouldThrowEvaluationExceptionWhenOrderingOperatorAppliedToStringOperands` — e.g. `[name] > 'Alice'` where `name = "Bob"` → throws
  - `shouldThrowEvaluationExceptionWhenBooleanUsedInArithmetic` — e.g. `[flag] + 1` where `flag = true` → throws
  - `shouldThrowEvaluationExceptionWhenNullValueInContext` — `EvaluationContext.of("age", null)` → throws

#### `EvaluationContextTest`
- `shouldReturnValueWhenFieldIsPresent`
- `shouldThrowWhenFieldIsAbsent`
- `shouldBeCaseSensitiveForFieldNames`
- `shouldCreateEmptyContext`

#### `BuiltinFunctionRegistryTest`
- One `@Nested` group per function.
- `shouldBeCaseInsensitiveForFunctionNames` — `@ParameterizedTest`: ABS, Abs, abs
- `shouldThrowWhenWrongArgumentCount` — `@ParameterizedTest` covering all 7 functions.
