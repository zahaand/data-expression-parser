# Implementation Plan: data-expression-parser

**Branch**: `001-data-expression-parser-core` | **Date**: 2026-04-13 | **Spec**: [spec.md](../specs/001-data-expression-parser-core/spec.md)
**Input**: Feature specification from `/specs/001-data-expression-parser-core/spec.md`

## Summary

Build a reusable Java library that parses and evaluates business expressions over named
data fields using ANTLR4-generated lexer/parser, a sealed-interface AST, and a visitor-based
evaluation pipeline. Distribute as a Spring Boot Starter with zero-config autoconfiguration.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: ANTLR4 4.13.2, Apache Commons Lang 3, Spring Boot 3.5.x (starter only)
**Storage**: N/A
**Testing**: JUnit 5 + AssertJ + Mockito
**Target Platform**: JVM (library JAR)
**Project Type**: Library (Maven Multi-Module: core + Spring Boot starter)
**Performance Goals**: N/A (pure computation library, no I/O)
**Constraints**: Core module MUST NOT depend on Spring; stateless and thread-safe
**Scale/Scope**: 7 built-in functions, 11 AST node types, ~25 source files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Architecture | PASS | Maven Multi-Module (core + starter) defined in Technology Stack |
| II. SRP | PASS | Each class has single responsibility: AST nodes = data, visitors = logic, parser = API |
| III. Database Migrations | N/A | No database layer |
| IV. Secrets | N/A | No runtime secrets |
| V. Logging | PASS | `LoggerFactory.getLogger()` declaration; no Lombok `@Slf4j` |
| VI. Code Style | PASS | Records for AST, `final` classes, constructor injection in starter |
| VII. Testing | PASS | JUnit 5 + `@Nested` + `@DisplayName` + `@ParameterizedTest` |
| VIII. Language | PASS | All English; Conventional Commits |
| IX. Simplicity | PASS | YAGNI — only 7 built-in functions, no user-defined functions in v1 |
| Dev Standard #9 | PASS | Grammar at specified path, rule order enforced |
| Dev Standard #10 | PASS | All AST nodes are records implementing sealed `Expression` |
| Dev Standard #11 | PASS | No Spring annotations in core module |

## Project Structure

### Documentation (this feature)

```text
specs/001-data-expression-parser-core/
├── spec.md              # Feature specification
└── plan.md              # This file
```

### Source Code (repository root)

```text
data-expression-parser/                          # Parent POM
├── pom.xml
├── data-expression-parser-core/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── antlr4/ru/zahaand/dataexpr/
│       │   │   └── DataExpression.g4
│       │   └── java/ru/zahaand/dataexpr/
│       │       ├── ast/
│       │       │   ├── Expression.java
│       │       │   ├── FieldNode.java
│       │       │   ├── NumberNode.java
│       │       │   ├── StringNode.java
│       │       │   ├── BooleanNode.java
│       │       │   ├── BinaryOpNode.java
│       │       │   ├── UnaryMinusNode.java
│       │       │   ├── FunctionCallNode.java
│       │       │   ├── ComparisonNode.java
│       │       │   ├── LogicalNode.java
│       │       │   ├── NotNode.java
│       │       │   ├── InNode.java
│       │       │   ├── ArithmeticOperator.java
│       │       │   ├── ComparisonOperator.java
│       │       │   └── LogicalOperator.java
│       │       ├── parser/
│       │       │   └── DataExpressionParser.java
│       │       ├── evaluator/
│       │       │   ├── ExpressionEvaluator.java
│       │       │   ├── EvaluationContext.java
│       │       │   ├── EvaluationResult.java
│       │       │   ├── DoubleResult.java
│       │       │   └── BooleanResult.java
│       │       ├── visitor/
│       │       │   ├── AstBuildingVisitor.java
│       │       │   └── EvaluatingVisitor.java
│       │       ├── function/
│       │       │   └── BuiltinFunctionRegistry.java
│       │       └── exception/
│       │           ├── ExpressionParseException.java
│       │           └── ExpressionEvaluationException.java
│       └── test/java/ru/zahaand/dataexpr/
│           ├── DataExpressionParserTest.java
│           ├── EvaluationContextTest.java
│           └── BuiltinFunctionRegistryTest.java
└── data-expression-parser-spring-boot-starter/
    ├── pom.xml
    └── src/main/
        ├── java/ru/zahaand/dataexpr/autoconfigure/
        │   └── DataExpressionParserAutoConfiguration.java
        └── resources/META-INF/spring/
            └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

**Structure Decision**: Maven Multi-Module library with two child modules.
`data-expression-parser-core` contains all logic (zero Spring dependencies).
`data-expression-parser-spring-boot-starter` provides autoconfiguration only.

---

## Sprint: 001-data-expression-parser-core

Single sprint, 6 phases executed in strict order.
Each phase MUST be committed separately before the next phase begins.

### Clarifications Applied

The following clarifications from the spec session are integrated into this plan:

1. **Numeric coercion**: Evaluator accepts any `Number` subtype via `Number.doubleValue()` (handles `Integer`, `Long`, `BigDecimal`).
2. **Ordering operators**: `>`, `<`, `>=`, `<=` are numeric-only; throw `ExpressionEvaluationException` on string operands.
3. **Mixed-type equality**: `==` on different types returns `false`; `!=` returns `true`.

---

### Phase 1 — Maven Skeleton

**Goal**: Both modules compile with no Java source files. `mvn compile` passes.

- [ ] Create parent `pom.xml`:
  - `groupId: ru.zahaand`, `artifactId: data-expression-parser`, `version: 1.0.0`, `packaging: pom`
  - Declare child modules: `data-expression-parser-core`, `data-expression-parser-spring-boot-starter`
  - `dependencyManagement`: ANTLR4 `4.13.2`, commons-lang3, Spring Boot BOM `3.5.0`,
    JUnit Jupiter, AssertJ, Mockito
  - Java 21 compiler settings via `maven-compiler-plugin`
- [ ] Create `data-expression-parser-core/pom.xml`:
  - Parent reference
  - Dependencies: `antlr4-runtime` (compile), `commons-lang3` (compile),
    `junit-jupiter` (test), `assertj-core` (test), `mockito-core` (test)
  - Plugin: `antlr4-maven-plugin:4.13.2` — source dir:
    `src/main/antlr4/ru/zahaand/dataexpr`
- [ ] Create `data-expression-parser-spring-boot-starter/pom.xml`:
  - Parent reference
  - Dependencies: `data-expression-parser-core` (compile),
    `spring-boot-autoconfigure` (compile), `spring-boot-starter` (compile)
- [ ] **Verify**: `mvn compile` passes with no errors in both modules

**Commit**: `feat(core): add Maven module skeleton`

---

### Phase 2 — ANTLR Grammar

**Goal**: ANTLR plugin generates `DataExpressionLexer.java` and `DataExpressionParser.java`.

- [ ] Create directory:
  `data-expression-parser-core/src/main/antlr4/ru/zahaand/dataexpr/`
- [ ] Create `DataExpression.g4` with the exact grammar from the spec:
  - Entry rule: `prog : expr EOF`
  - Logical layer: `orExpr → andExpr → notExpr`
  - Comparison layer with `IN` / `NOT IN`
  - Arithmetic layer: `additive → multiplicative → power → unary → primary`
  - Lexer rules in order: reserved words (`TRUE`, `FALSE`, `AND`, `OR`, `NOT`, `IN`) ABOVE `ID`
  - `FIELD : '[' ~[\]\n]+ ']'` — allows spaces and special chars in field names
  - `STAR : '*'` — `**` matched as two consecutive `STAR` tokens in parser, NOT a lexer token
  - Reserved words use character alternatives for case-insensitivity
- [ ] **Verify**: `mvn generate-sources` generates both lexer and parser Java files
  under `target/generated-sources/antlr4/ru/zahaand/dataexpr/`

**Commit**: `feat(core): add ANTLR grammar DataExpression.g4`

---

### Phase 3 — AST Nodes

**Goal**: All AST types compile cleanly. No logic — data structures only.

- [ ] Create `ru.zahaand.dataexpr.ast.Expression` — sealed interface:
  `permits FieldNode, NumberNode, StringNode, BooleanNode, BinaryOpNode,
  UnaryMinusNode, FunctionCallNode, ComparisonNode, LogicalNode, NotNode, InNode`
- [ ] Create `FieldNode(String fieldName)` — record
- [ ] Create `NumberNode(double value)` — record
- [ ] Create `StringNode(String value)` — record
- [ ] Create `BooleanNode(boolean value)` — record
- [ ] Create `ArithmeticOperator` enum: `ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO, POWER`
- [ ] Create `ComparisonOperator` enum: `GT, LT, GTE, LTE, EQ, NEQ`
- [ ] Create `LogicalOperator` enum: `AND, OR`
- [ ] Create `BinaryOpNode(Expression left, ArithmeticOperator op, Expression right)` — record
- [ ] Create `UnaryMinusNode(Expression operand)` — record
- [ ] Create `FunctionCallNode(String name, List<Expression> args)` — record
- [ ] Create `ComparisonNode(Expression left, ComparisonOperator op, Expression right)` — record
- [ ] Create `LogicalNode(Expression left, LogicalOperator op, Expression right)` — record
- [ ] Create `NotNode(Expression operand)` — record
- [ ] Create `InNode(Expression field, List<Expression> values, boolean negated)` — record
- [ ] **Verify**: `mvn compile` passes

**Commit**: `feat(core): add AST node types`

---

### Phase 4 — Core Logic

**Goal**: Full parsing and evaluation pipeline works end-to-end.

- [ ] Create `ru.zahaand.dataexpr.exception.ExpressionParseException` — `RuntimeException`,
  two constructors: `(String message)` and `(String message, Throwable cause)`
- [ ] Create `ru.zahaand.dataexpr.exception.ExpressionEvaluationException` — `RuntimeException`,
  same two constructors
- [ ] Create `ru.zahaand.dataexpr.evaluator.EvaluationResult` — sealed interface:
  `permits DoubleResult, BooleanResult`
- [ ] Create `DoubleResult(double value)` — record implementing `EvaluationResult`
- [ ] Create `BooleanResult(boolean value)` — record implementing `EvaluationResult`
- [ ] Create `ru.zahaand.dataexpr.evaluator.EvaluationContext`:
  - `final` class, internal `Map<String, Object>` field (defensive copy in constructor)
  - Factory: `empty()`, `of(String, Object)`, `of(Map<String, Object>)`
  - `get(String fieldName)` — case-sensitive lookup,
    throws `ExpressionEvaluationException` if absent
- [ ] Create `ru.zahaand.dataexpr.function.BuiltinFunctionRegistry`:
  - Package-private utility class, `private` no-arg constructor
  - 7 functions: `abs`, `round`, `floor`, `ceil`, `min`, `max`, `pow`
  - Case-insensitive lookup via `name.toLowerCase()`
  - Wrong arity → `ExpressionEvaluationException`:
    `"Function '<name>' expects <N> argument(s) but got <M>"`
  - Unknown name → `ExpressionEvaluationException`:
    `"Unknown function: '<name>'"`
- [ ] Create `ru.zahaand.dataexpr.visitor.AstBuildingVisitor`:
  - Package-private, extends `DataExpressionBaseVisitor<Expression>`
  - New instance MUST be created per `parse()` call (thread safety)
  - Strip brackets from field names: `[first name]` → `FieldNode("first name")`
  - Strip quotes from strings: `'active'` → `StringNode("active")`
  - Power right-associativity: `a ^ b ^ c` →
    `BinaryOpNode(a, POWER, BinaryOpNode(b, POWER, c))`
  - `NOT IN` → `InNode(..., negated = true)`
  - `IN` → `InNode(..., negated = false)`
- [ ] Create `ru.zahaand.dataexpr.visitor.EvaluatingVisitor`:
  - Package-private
  - `switch` pattern matching on sealed `Expression`
  - **Numeric coercion**: accept any `Number` subtype via `Number.doubleValue()`
    (handles `Integer`, `Long`, `BigDecimal` from real-world contexts)
  - **Ordering operators** (`>`, `<`, `>=`, `<=`): numeric operands only;
    throws `ExpressionEvaluationException` on string operands
  - **Equality operators** (`==`, `!=`): support both numeric and string operands;
    mixed-type `==` → `false`, mixed-type `!=` → `true`
  - Division by zero → `ExpressionEvaluationException`
  - NaN / Infinity → propagate silently (match `java.lang.Math` semantics)
  - `IN` / `NOT IN`: compares field value against each list value using `.equals()`
  - Functions: delegate to `BuiltinFunctionRegistry`
- [ ] Create `ru.zahaand.dataexpr.evaluator.ExpressionEvaluator`:
  - `final` class
  - `evaluate(Expression, EvaluationContext)` → `EvaluationResult`
  - Delegates to `EvaluatingVisitor`
- [ ] Create `ru.zahaand.dataexpr.parser.DataExpressionParser`:
  - `final` class, accepts `ExpressionEvaluator` via constructor
  - `parse(String)` → `Expression` — new `AstBuildingVisitor` per call
  - Throws `ExpressionParseException` on null, blank, or syntax error
  - ANTLR error listener: replace default with one that throws `ExpressionParseException`
  - `evaluate(String, EvaluationContext)` → `EvaluationResult`
  - `evaluateBoolean(String, EvaluationContext)` → `boolean` —
    throws `ExpressionEvaluationException` if result is not `BooleanResult`
  - `evaluateDouble(String, EvaluationContext)` → `double` —
    throws `ExpressionEvaluationException` if result is not `DoubleResult`
- [ ] **Verify**: `mvn compile` passes in both modules

**Commit**: `feat(core): add parsing and evaluation pipeline`

---

### Phase 5 — Tests

**Goal**: `mvn test` passes with all test cases green.

- [ ] Create `DataExpressionParserTest` with four `@Nested` groups:
  - `Parse` (15 test methods — AST structure assertions):
    - `shouldReturnFieldNodeWhenExpressionIsFieldReference`
    - `shouldReturnNumberNodeWhenExpressionIsNumericLiteral`
    - `shouldReturnStringNodeWhenExpressionIsStringLiteral`
    - `shouldReturnBooleanNodeWhenExpressionIsTrueLiteral`
    - `shouldReturnBooleanNodeWhenExpressionIsFalseLiteral`
    - `shouldParseTrueAndFalseCaseInsensitively` — `@ParameterizedTest`
    - `shouldReturnBinaryOpNodeForArithmetic`
    - `shouldRespectArithmeticPrecedence`
    - `shouldReturnComparisonNodeForGreaterThan`
    - `shouldReturnLogicalNodeForAndExpression`
    - `shouldReturnNotNodeForNotExpression`
    - `shouldReturnInNodeForInExpression`
    - `shouldReturnInNodeWithNegatedTrueForNotIn`
    - `shouldHandleFieldNamesWithSpaces`
    - `shouldHandleRightAssociativityForPower`
  - `EvaluateBoolean` (8 test methods — boolean result assertions):
    - `shouldReturnTrueWhenFieldGreaterThanLiteral`
    - `shouldReturnFalseWhenFieldNotMatchingString`
    - `shouldEvaluateAndExpression`
    - `shouldEvaluateOrExpression`
    - `shouldEvaluateNotExpression`
    - `shouldEvaluateInExpression`
    - `shouldEvaluateNotInExpression`
    - `shouldEvaluateComplexCondition`
  - `EvaluateDouble` (3 test methods — numeric result assertions):
    - `shouldEvaluateArithmeticOverFields`
    - `shouldEvaluateFunctionCall`
    - `shouldEvaluateModuloOperator`
  - `Errors` (9 test methods — exception assertions):
    - `shouldThrowParseExceptionWhenExpressionIsMalformed`
    - `shouldThrowParseExceptionWhenInputIsNull`
    - `shouldThrowParseExceptionWhenInputIsBlank`
    - `shouldThrowEvaluationExceptionWhenFieldNotInContext`
    - `shouldThrowEvaluationExceptionWhenDivisionByZero`
    - `shouldThrowEvaluationExceptionWhenUnknownFunction`
    - `shouldThrowEvaluationExceptionWhenArithmeticOnStringField`
    - `shouldThrowEvaluationExceptionWhenEvaluateBooleanCalledOnDoubleResult`
    - `shouldThrowEvaluationExceptionWhenEvaluateDoubleCalledOnBooleanResult`
- [ ] Create `EvaluationContextTest` (4 test methods):
  - `shouldReturnValueWhenFieldIsPresent`
  - `shouldThrowWhenFieldIsAbsent`
  - `shouldBeCaseSensitiveForFieldNames`
  - `shouldCreateEmptyContext`
- [ ] Create `BuiltinFunctionRegistryTest`:
  - One `@Nested` per function (7 groups)
  - `shouldBeCaseInsensitiveForFunctionNames` — `@ParameterizedTest`: ABS, Abs, abs
  - `shouldThrowWhenWrongArgumentCount` — `@ParameterizedTest` all 7 functions
- [ ] **Verify**: `mvn test` passes with no failures

**Commit**: `test(core): add unit tests`

---

### Phase 6 — Spring Starter

**Goal**: Starter module builds and autoconfigures beans correctly.

- [ ] Create `ru.zahaand.dataexpr.autoconfigure.DataExpressionParserAutoConfiguration`:
  - Annotated with `@AutoConfiguration`
  - `expressionEvaluator()` bean with `@ConditionalOnMissingBean`
  - `dataExpressionParser(ExpressionEvaluator)` bean with `@ConditionalOnMissingBean`
- [ ] Create
  `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  containing exactly one line:
  `ru.zahaand.dataexpr.autoconfigure.DataExpressionParserAutoConfiguration`
- [ ] **Verify**: `mvn package` passes in both modules, starter JAR is produced

**Commit**: `feat(starter): add Spring Boot autoconfiguration`

---

## Commit Strategy

One commit per phase, in order:

| Phase | Commit Message |
|-------|---------------|
| 1 | `feat(core): add Maven module skeleton` |
| 2 | `feat(core): add ANTLR grammar DataExpression.g4` |
| 3 | `feat(core): add AST node types` |
| 4 | `feat(core): add parsing and evaluation pipeline` |
| 5 | `test(core): add unit tests` |
| 6 | `feat(starter): add Spring Boot autoconfiguration` |

---

## Complexity Tracking

| Risk | Mitigation |
|------|------------|
| `**` operator conflicts with `*` in lexer | Matched as two `STAR` tokens in parser rule — no lexer token for `**` |
| `NOT IN` requires two-token detection in visitor | Visitor checks for `NOT` token presence before `IN` in comparison context |
| Right-associativity of `^` | Implemented manually in visitor via recursive right-side folding |
| Reserved words matched as `ID` | Lexer order enforced: reserved word rules above `ID` rule |
| Thread safety of `AstBuildingVisitor` | New instance created per `parse()` call — stateless parser |
| Numeric type coercion from real-world contexts | `Number.doubleValue()` handles `Integer`, `Long`, `BigDecimal` transparently |
| Mixed-type equality semantics | `==` returns `false`, `!=` returns `true` — no exceptions for type mismatch |

No constitution violations. Complexity Tracking contains risk mitigations only.
