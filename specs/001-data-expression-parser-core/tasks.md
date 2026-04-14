# Tasks: data-expression-parser-core

**Input**: Design documents from `/specs/001-data-expression-parser-core/`
**Prerequisites**: plan.md (required), spec.md (required for user stories)

**Tests**: Included - explicitly specified in the feature specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Parent POM**: `pom.xml` at repository root
- **Core module**: `data-expression-parser-core/`
- **Starter module**: `data-expression-parser-spring-boot-starter/`
- **Core source**: `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/`
- **ANTLR source**: `data-expression-parser-core/src/main/antlr4/ru/zahaand/dataexpr/`
- **Core tests**: `data-expression-parser-core/src/test/java/ru/zahaand/dataexpr/`

---

## Phase 1: Setup (Maven Skeleton)

**Purpose**: Both modules compile with no Java source files. `mvn compile` passes.

- [x] T001 Create parent `pom.xml` with groupId `ru.zahaand`, artifactId `data-expression-parser`, version `1.0.0`, packaging `pom`, child modules, dependencyManagement (ANTLR4 4.13.2, commons-lang3, Spring Boot BOM 3.5.0, JUnit Jupiter, AssertJ, Mockito), and Java 21 compiler settings
- [x] T002 [P] Create `data-expression-parser-core/pom.xml` with parent reference, dependencies (antlr4-runtime, commons-lang3, junit-jupiter, assertj-core, mockito-core), and antlr4-maven-plugin configuration
- [x] T003 [P] Create `data-expression-parser-spring-boot-starter/pom.xml` with parent reference and dependencies (data-expression-parser-core, spring-boot-autoconfigure, spring-boot-starter)
- [x] T004 Verify `mvn compile` passes with no errors in both modules

**Commit**: `feat(core): add Maven module skeleton`

---

## Phase 2: Foundational (ANTLR Grammar + AST + Exceptions)

**Purpose**: Grammar generates lexer/parser; all AST types and exception types compile. BLOCKS all user stories.

**CRITICAL**: No user story work can begin until this phase is complete.

### ANTLR Grammar

- [x] T005 Create ANTLR grammar in `data-expression-parser-core/src/main/antlr4/ru/zahaand/dataexpr/DataExpression.g4` with entry rule, logical/comparison/arithmetic layers, lexer rules (reserved words above ID), FIELD/STRING/NUMBER/ID/STAR/WS tokens per spec
- [x] T006 Verify `mvn generate-sources` generates `DataExpressionLexer.java` and `DataExpressionParser.java` under `target/generated-sources/antlr4/ru/zahaand/dataexpr/`

**Commit**: `feat(core): add ANTLR grammar DataExpression.g4`

### AST Node Types

- [x] T007 Create sealed interface `Expression` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/Expression.java` permitting all 11 node types
- [x] T008 [P] Create record `FieldNode(String fieldName)` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/FieldNode.java`
- [x] T009 [P] Create record `NumberNode(double value)` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/NumberNode.java`
- [x] T010 [P] Create record `StringNode(String value)` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/StringNode.java`
- [x] T011 [P] Create record `BooleanNode(boolean value)` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/BooleanNode.java`
- [x] T012 [P] Create enum `ArithmeticOperator` (ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO, POWER) in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/ArithmeticOperator.java`
- [x] T013 [P] Create enum `ComparisonOperator` (GT, LT, GTE, LTE, EQ, NEQ) in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/ComparisonOperator.java`
- [x] T014 [P] Create enum `LogicalOperator` (AND, OR) in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/LogicalOperator.java`
- [x] T015 [P] Create record `BinaryOpNode(Expression left, ArithmeticOperator op, Expression right)` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/BinaryOpNode.java`
- [x] T016 [P] Create record `UnaryMinusNode(Expression operand)` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/UnaryMinusNode.java`
- [x] T017 [P] Create record `FunctionCallNode(String name, List<Expression> args)` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/FunctionCallNode.java`
- [x] T018 [P] Create record `ComparisonNode(Expression left, ComparisonOperator op, Expression right)` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/ComparisonNode.java`
- [x] T019 [P] Create record `LogicalNode(Expression left, LogicalOperator op, Expression right)` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/LogicalNode.java`
- [x] T020 [P] Create record `NotNode(Expression operand)` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/NotNode.java`
- [x] T021 [P] Create record `InNode(Expression field, List<Expression> values, boolean negated)` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/InNode.java`

**Commit**: `feat(core): add AST node types`

### Exception Types

- [x] T022 [P] Create `ExpressionParseException` (RuntimeException, two constructors) in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/exception/ExpressionParseException.java`
- [x] T023 [P] Create `ExpressionEvaluationException` (RuntimeException, two constructors) in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/exception/ExpressionEvaluationException.java`
- [x] T024 Verify `mvn compile` passes

**Commit**: `feat(core): add exception types`

**Checkpoint**: Foundation ready - all AST types, grammar, and exceptions are in place. User story implementation can begin.

---

## Phase 3: User Story 1 - Parse Expression to AST (Priority: P1) MVP

**Goal**: A developer passes a business expression string to the library and receives a structured AST that can be inspected, serialized, or evaluated later.

**Independent Test**: Call `DataExpressionParser.parse()` with various expression strings and assert returned AST node types and structure.

### Implementation for User Story 1

- [x] T025 [US1] Create package-private `AstBuildingVisitor` extending `DataExpressionBaseVisitor<Expression>` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/visitor/AstBuildingVisitor.java` — handle bracket stripping for fields, quote stripping for strings, right-associative power, NOT IN detection, all operator mappings, and ANTLR error listener throwing `ExpressionParseException` with format `"Parse error at line <L>:<C>: <antlr_message>"`
- [x] T026 [US1] Create `DataExpressionParser` (final class) in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/parser/DataExpressionParser.java` — constructor accepts `ExpressionEvaluator`, `parse(String)` creates new `AstBuildingVisitor` per call, throws `ExpressionParseException` on null/blank/syntax error. Add placeholder `evaluate`, `evaluateBoolean`, `evaluateDouble` methods that delegate to evaluator (to be implemented in US2/US3)

### Tests for User Story 1

- [x] T027 [US1] Create `DataExpressionParserTest` with `@Nested` `Parse` group (15 tests) in `data-expression-parser-core/src/test/java/ru/zahaand/dataexpr/DataExpressionParserTest.java` — tests: shouldReturnFieldNodeWhenExpressionIsFieldReference, shouldReturnNumberNodeWhenExpressionIsNumericLiteral, shouldReturnStringNodeWhenExpressionIsStringLiteral, shouldReturnBooleanNodeWhenExpressionIsTrueLiteral, shouldReturnBooleanNodeWhenExpressionIsFalseLiteral, shouldParseTrueAndFalseCaseInsensitively (@ParameterizedTest), shouldReturnBinaryOpNodeForArithmetic, shouldRespectArithmeticPrecedence, shouldReturnComparisonNodeForGreaterThan, shouldReturnLogicalNodeForAndExpression, shouldReturnNotNodeForNotExpression, shouldReturnInNodeForInExpression, shouldReturnInNodeWithNegatedTrueForNotIn, shouldHandleFieldNamesWithSpaces, shouldHandleRightAssociativityForPower
- [x] T028 [US1] Add `@Nested` `Errors` group with parse-related tests (3 tests) to `DataExpressionParserTest` — shouldThrowParseExceptionWhenExpressionIsMalformed, shouldThrowParseExceptionWhenInputIsNull, shouldThrowParseExceptionWhenInputIsBlank
- [x] T029 [US1] Verify `mvn test` passes for all Parse and parse-related Errors tests

**Commit**: `feat(core): add parsing pipeline and parse tests`

**Checkpoint**: User Story 1 complete — `parse()` works end-to-end. AST can be inspected.

---

## Phase 4: User Story 2 - Evaluate Expression to Boolean (Priority: P1)

**Goal**: A developer evaluates a boolean expression against a field context and receives a `true` or `false` result.

**Independent Test**: Call `evaluateBoolean()` with expressions and an `EvaluationContext` containing test field values.

### Implementation for User Story 2

- [x] T030 [P] [US2] Create sealed interface `EvaluationResult` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/evaluator/EvaluationResult.java` permitting `DoubleResult` and `BooleanResult`
- [x] T031 [P] [US2] Create record `DoubleResult(double value)` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/evaluator/DoubleResult.java`
- [x] T032 [P] [US2] Create record `BooleanResult(boolean value)` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/evaluator/BooleanResult.java`
- [x] T033 [P] [US2] Create `EvaluationContext` (final class) in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/evaluator/EvaluationContext.java` — defensive copy in constructor, factories `empty()`, `of(String, Object)`, `of(Map<String, Object>)`, case-sensitive `get()` throwing `ExpressionEvaluationException` if absent, null value rejection at construction time
- [x] T034 [US2] Create package-private `EvaluatingVisitor` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/visitor/EvaluatingVisitor.java` — switch pattern matching on sealed Expression, numeric coercion via `Number.doubleValue()`, ordering operators numeric-only, mixed-type equality semantics, boolean-in-arithmetic throws, division by zero throws, NaN/Infinity propagates, IN/NOT IN via `.equals()`, function delegation to `BuiltinFunctionRegistry`
- [x] T035 [US2] Create `ExpressionEvaluator` (final class) in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/evaluator/ExpressionEvaluator.java` — `evaluate(Expression, EvaluationContext)` delegates to `EvaluatingVisitor`
- [x] T036 [US2] Wire `evaluate()` and `evaluateBoolean()` methods in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/parser/DataExpressionParser.java` — `evaluateBoolean` throws `ExpressionEvaluationException` if result is not `BooleanResult`

### Tests for User Story 2

- [x] T037 [P] [US2] Create `EvaluationContextTest` (4 tests) in `data-expression-parser-core/src/test/java/ru/zahaand/dataexpr/EvaluationContextTest.java` — shouldReturnValueWhenFieldIsPresent, shouldThrowWhenFieldIsAbsent, shouldBeCaseSensitiveForFieldNames, shouldCreateEmptyContext
- [x] T038 [US2] Add `@Nested` `EvaluateBoolean` group (10 tests) to `DataExpressionParserTest` — shouldReturnTrueWhenFieldGreaterThanLiteral, shouldReturnFalseWhenFieldNotMatchingString, shouldEvaluateAndExpression, shouldEvaluateOrExpression, shouldEvaluateNotExpression, shouldEvaluateInExpression, shouldEvaluateNotInExpression, shouldEvaluateComplexCondition, shouldReturnFalseWhenEqualityComparesFieldOfDifferentTypes, shouldReturnTrueWhenInequalityComparesFieldOfDifferentTypes
- [x] T039 [US2] Add evaluation-related error tests (8 tests) to `@Nested` `Errors` group in `DataExpressionParserTest` — shouldThrowEvaluationExceptionWhenFieldNotInContext, shouldThrowEvaluationExceptionWhenDivisionByZero, shouldThrowEvaluationExceptionWhenUnknownFunction, shouldThrowEvaluationExceptionWhenArithmeticOnStringField, shouldThrowEvaluationExceptionWhenEvaluateBooleanCalledOnDoubleResult, shouldThrowEvaluationExceptionWhenOrderingOperatorAppliedToStringOperands, shouldThrowEvaluationExceptionWhenBooleanUsedInArithmetic, shouldThrowEvaluationExceptionWhenNullValueInContext
- [x] T040 [US2] Verify `mvn test` passes for all EvaluateBoolean, EvaluationContext, and evaluation Errors tests

**Commit**: `feat(core): add evaluation pipeline and boolean evaluation tests`

**Checkpoint**: User Story 2 complete — boolean evaluation works end-to-end.

---

## Phase 5: User Story 3 - Evaluate Expression to Double (Priority: P2)

**Goal**: A developer evaluates an arithmetic expression against a field context and receives a numeric result.

**Independent Test**: Call `evaluateDouble()` with arithmetic expressions and an `EvaluationContext` containing numeric field values.

### Implementation for User Story 3

- [x] T041 [US3] Create package-private `BuiltinFunctionRegistry` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/function/BuiltinFunctionRegistry.java` — private constructor, 7 functions (abs, round, floor, ceil, min, max, pow), case-insensitive lookup via `toLowerCase()`, wrong arity and unknown function exceptions per spec
- [x] T042 [US3] Wire `evaluateDouble()` method in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/parser/DataExpressionParser.java` — throws `ExpressionEvaluationException` if result is not `DoubleResult`

### Tests for User Story 3

- [x] T043 [P] [US3] Create `BuiltinFunctionRegistryTest` in `data-expression-parser-core/src/test/java/ru/zahaand/dataexpr/BuiltinFunctionRegistryTest.java` — one `@Nested` per function (7 groups), shouldBeCaseInsensitiveForFunctionNames (@ParameterizedTest: ABS, Abs, abs), shouldThrowWhenWrongArgumentCount (@ParameterizedTest all 7 functions)
- [x] T044 [US3] Add `@Nested` `EvaluateDouble` group (6 tests) to `DataExpressionParserTest` — shouldEvaluateArithmeticOverFields, shouldEvaluateFunctionCall, shouldEvaluateModuloOperator, shouldCoerceIntegerFieldToDouble, shouldCoerceLongFieldToDouble, shouldCoerceBigDecimalFieldToDouble
- [x] T045 [US3] Add `shouldThrowEvaluationExceptionWhenEvaluateDoubleCalledOnBooleanResult` test to `@Nested` `Errors` group in `DataExpressionParserTest`
- [x] T046 [US3] Verify `mvn test` passes for all EvaluateDouble and BuiltinFunctionRegistry tests

**Commit**: `feat(core): add built-in functions and double evaluation tests`

**Checkpoint**: User Story 3 complete — arithmetic evaluation with built-in functions works.

---

## Phase 6: User Story 4 - Spring Boot Auto-injection (Priority: P3)

**Goal**: A Spring Boot application adds the starter dependency and receives `DataExpressionParser` as a ready-to-use singleton bean via autoconfiguration.

**Independent Test**: Verify starter module builds and autoconfiguration class is present in the JAR.

### Implementation for User Story 4

- [x] T047 [US4] Create `DataExpressionParserAutoConfiguration` in `data-expression-parser-spring-boot-starter/src/main/java/ru/zahaand/dataexpr/autoconfigure/DataExpressionParserAutoConfiguration.java` — `@AutoConfiguration`, `expressionEvaluator()` bean with `@ConditionalOnMissingBean`, `dataExpressionParser(ExpressionEvaluator)` bean with `@ConditionalOnMissingBean`
- [x] T048 [US4] Create `org.springframework.boot.autoconfigure.AutoConfiguration.imports` in `data-expression-parser-spring-boot-starter/src/main/resources/META-INF/spring/` containing one line: `ru.zahaand.dataexpr.autoconfigure.DataExpressionParserAutoConfiguration`
- [x] T049 [US4] Verify `mvn package` passes in both modules, starter JAR is produced

**Commit**: `feat(starter): add Spring Boot autoconfiguration`

**Checkpoint**: User Story 4 complete — starter module provides autoconfigured beans.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final verification across all modules

- [x] T050 Run full `mvn clean verify` and confirm all tests pass and both JARs are produced
- [x] T051 Verify `data-expression-parser-core` has zero Spring dependencies on classpath (check `mvn dependency:tree`)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (Maven skeleton must compile) — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Phase 2 (grammar + AST nodes + exceptions)
- **User Story 2 (Phase 4)**: Depends on Phase 3 (parsing pipeline must exist)
- **User Story 3 (Phase 5)**: Depends on Phase 4 (evaluation pipeline must exist)
- **User Story 4 (Phase 6)**: Depends on Phase 5 (core module must be complete)
- **Polish (Phase 7)**: Depends on all phases complete

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Foundational — no other story dependencies
- **User Story 2 (P1)**: Depends on US1 — needs parsing to produce AST for evaluation
- **User Story 3 (P2)**: Depends on US2 — needs evaluation pipeline, adds function registry and double convenience method
- **User Story 4 (P3)**: Depends on US3 — needs complete core module for autoconfiguration

### Within Each User Story

- Models/types before logic
- Logic before API surface
- Implementation before tests
- Tests verify story works independently
- Story complete before moving to next priority

### Parallel Opportunities

**Phase 1**:
- T002 and T003 (core and starter POMs) can run in parallel

**Phase 2**:
- T008-T021 (all AST node types) can run in parallel after T007 (sealed interface)
- T022 and T023 (exception types) can run in parallel with AST nodes

**Phase 4**:
- T030-T033 (EvaluationResult, DoubleResult, BooleanResult, EvaluationContext) can run in parallel
- T037 (EvaluationContextTest) can run in parallel with T038

**Phase 5**:
- T043 (BuiltinFunctionRegistryTest) can run in parallel with T044

---

## Parallel Example: Phase 2 (Foundational)

```bash
# Launch all AST literal nodes together:
Task T008: "Create FieldNode record"
Task T009: "Create NumberNode record"
Task T010: "Create StringNode record"
Task T011: "Create BooleanNode record"

# Launch all enums together:
Task T012: "Create ArithmeticOperator enum"
Task T013: "Create ComparisonOperator enum"
Task T014: "Create LogicalOperator enum"

# Launch all composite AST nodes together:
Task T015: "Create BinaryOpNode record"
Task T016: "Create UnaryMinusNode record"
Task T017: "Create FunctionCallNode record"
Task T018: "Create ComparisonNode record"
Task T019: "Create LogicalNode record"
Task T020: "Create NotNode record"
Task T021: "Create InNode record"

# Launch both exception types together:
Task T022: "Create ExpressionParseException"
Task T023: "Create ExpressionEvaluationException"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Maven Skeleton
2. Complete Phase 2: Grammar + AST + Exceptions
3. Complete Phase 3: User Story 1 — Parse Expression to AST
4. **STOP and VALIDATE**: Test parsing independently with `mvn test`
5. Parsing works — AST can be inspected

### Incremental Delivery

1. Phase 1 + Phase 2 → Foundation ready
2. Add User Story 1 → Test independently → Parsing works (MVP!)
3. Add User Story 2 → Test independently → Boolean evaluation works
4. Add User Story 3 → Test independently → Double evaluation + functions work
5. Add User Story 4 → Test independently → Spring Boot starter works
6. Each story adds value without breaking previous stories

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently testable after its phase completes
- Commit after each phase (matches plan.md commit strategy)
- User stories are sequential due to the layered nature of this library (parsing → evaluation → functions → starter)
- Total: 51 tasks across 7 phases
