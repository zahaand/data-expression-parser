# Requirements Quality Checklist: data-expression-parser

**Purpose**: Thorough pre-implementation audit of requirement completeness, clarity, consistency, measurability, and edge case coverage across all spec areas.
**Created**: 2026-04-13
**Feature**: [spec.md](../spec.md)
**Audience**: Author (pre-implementation gate)
**Depth**: Thorough

## Requirement Completeness

- [ ] CHK001 - Are all 11 AST node types fully specified with constructor signatures and field types? [Completeness, Spec §AST Node Contracts]
- [ ] CHK002 - Are all 7 built-in functions specified with exact argument counts and `Math.*` delegation? [Completeness, Spec §Built-in Functions]
- [ ] CHK003 - Are all 6 arithmetic operators mapped to `ArithmeticOperator` enum values? [Completeness, Spec §Operator enums]
- [ ] CHK004 - Are all 6 comparison operators mapped to `ComparisonOperator` enum values? [Completeness, Spec §Operator enums]
- [ ] CHK005 - Are factory methods for `EvaluationContext` (`empty`, `of(String, Object)`, `of(Map)`) all specified with behavior? [Completeness, Spec §Public API]
- [ ] CHK006 - Are all 4 public methods on `DataExpressionParser` (`parse`, `evaluate`, `evaluateBoolean`, `evaluateDouble`) specified with signatures and exception contracts? [Completeness, Spec §Public API]
- [ ] CHK007 - Is the autoconfiguration registration file path fully specified (`META-INF/spring/...imports`)? [Completeness, Spec §Spring Autoconfiguration]
- [ ] CHK008 - Are both exception classes specified with both constructor variants (`message` and `message + cause`)? [Completeness, Spec §Exception Contracts]
- [ ] CHK009 - Are error message templates specified for function arity mismatch and unknown function? [Completeness, Spec §Built-in Functions]
- [ ] CHK010 - Is the ANTLR plugin configuration specified (source directory, target directory, Maven phase)? [Completeness, Spec §Module 1]

## Requirement Clarity

- [ ] CHK011 - Is "case-insensitive" for reserved words defined precisely as character-alternative lexer rules (not runtime normalization)? [Clarity, Spec §ANTLR Grammar]
- [ ] CHK012 - Is "case-insensitive" for function names defined precisely as `toLowerCase()` normalization at runtime? [Clarity, Spec §Built-in Functions]
- [ ] CHK013 - Is "case-sensitive" for field names in `EvaluationContext` unambiguously stated? [Clarity, Spec §Public API]
- [ ] CHK014 - Is "right-associative" for power operator defined with a concrete example (`a ^ b ^ c` → specific AST)? [Clarity, Spec §Visitor Contracts]
- [ ] CHK015 - Is "stateless" for `DataExpressionParser` defined precisely — new `AstBuildingVisitor` per call, no mutable fields? [Clarity, Spec §Public API]
- [ ] CHK016 - Is "package-private" visibility explicitly stated for `AstBuildingVisitor` and `EvaluatingVisitor`? [Clarity, Spec §Visitor Contracts]
- [ ] CHK017 - Is the `NUMBER` lexer rule unambiguous about whether negative literals are lexer tokens or parsed via unary minus? [Clarity, Spec §ANTLR Grammar]
- [ ] CHK018 - Is the `STRING` lexer rule's escape handling defined — does `~['\\]*` mean no escape sequences are supported? [Clarity, Spec §ANTLR Grammar]
- [ ] CHK019 - Is the `round()` function behavior specified precisely as `(double) Math.round(x)` (long-to-double cast)? [Clarity, Spec §Built-in Functions]
- [ ] CHK020 - Is "defensive copy" for `EvaluationContext.of(Map)` specified or left as implementation detail? [Clarity, Spec §Public API]

## Requirement Consistency

- [ ] CHK021 - Is the `Expression` sealed interface `permits` clause consistent with the actual list of 11 AST node records? [Consistency, Spec §AST Node Contracts]
- [ ] CHK022 - Is the grammar rule order (entry → logical → comparison → arithmetic → primary) consistent between the grammar listing and Constitution Dev Standard #9? [Consistency, Spec §ANTLR Grammar / Constitution §Dev Standard 9]
- [ ] CHK023 - Are `EvaluationResult` permits (`DoubleResult`, `BooleanResult`) consistent between the sealed interface definition and usage in `evaluateBoolean`/`evaluateDouble`? [Consistency, Spec §Evaluation Result / §Public API]
- [ ] CHK024 - Is the `DataExpressionParser` constructor signature consistent between the Public API section (takes `ExpressionEvaluator`) and the Autoconfiguration section (injects `ExpressionEvaluator`)? [Consistency, Spec §Public API / §Spring Autoconfiguration]
- [ ] CHK025 - Is the grammar file path consistent between Module 1 build plugin config and Constitution Dev Standard #9? [Consistency, Spec §Module 1 / Constitution §Dev Standard 9]
- [ ] CHK026 - Are the `ExpressionEvaluator` constructor requirements consistent — spec says `final` class but autoconfiguration creates via `new ExpressionEvaluator()` (no-arg)? [Consistency, Spec §Public API / §Spring Autoconfiguration]
- [ ] CHK027 - Is the number of test methods per `@Nested` group consistent between the Testing Requirements section and the actual scenario count (15 + 8 + 3 + 9 = 35)? [Consistency, Spec §Testing Requirements]
- [ ] CHK028 - Are comparison semantics consistent between grammar (allows any `additive` operands with `>`, `<`) and evaluator (numeric-only restriction for ordering)? [Consistency, Spec §ANTLR Grammar / §Visitor Contracts / §Clarifications]

## Acceptance Criteria Quality

- [ ] CHK029 - Is SC-001 (`mvn compile` passes) measurable and automatable? [Measurability, Spec §Success Criteria]
- [ ] CHK030 - Is SC-002 (ANTLR generates lexer/parser) verifiable by checking specific file existence? [Measurability, Spec §Success Criteria]
- [ ] CHK031 - Is SC-004 (consumer integration test) specified with exact expression, context, and expected result? [Measurability, Spec §Success Criteria]
- [ ] CHK032 - Are User Story acceptance scenarios written in Given/When/Then format with concrete values? [Measurability, Spec §User Scenarios]
- [ ] CHK033 - Can each of the 35 test methods be objectively evaluated as pass/fail from its name and the spec alone? [Measurability, Spec §Testing Requirements]

## Scenario Coverage — Grammar & Parsing

- [ ] CHK034 - Are requirements defined for parsing empty parentheses `()` as a primary expression? [Coverage, Gap]
- [ ] CHK035 - Are requirements defined for deeply nested expressions (e.g., `((([x] + 1)))`)? [Coverage, Edge Case]
- [ ] CHK036 - Are requirements defined for consecutive unary minus (e.g., `--[x]`, `---[x]`)? [Coverage, Edge Case]
- [ ] CHK037 - Are requirements defined for empty `IN` value list `[x] IN ()`? [Coverage, Edge Case]
- [ ] CHK038 - Are requirements defined for single-item `IN` value list `[x] IN ('a')`? [Coverage, Edge Case]
- [ ] CHK039 - Are requirements defined for whitespace-only input (e.g., `"   "`)? Does "blank" in the spec cover this? [Clarity, Spec §Exception Contracts]
- [ ] CHK040 - Are requirements defined for field names containing only special characters (e.g., `[#$%]`)? [Coverage, Edge Case]
- [ ] CHK041 - Are requirements defined for maximum expression length or recursion depth limits? [Coverage, Gap]
- [ ] CHK042 - Are requirements defined for expressions using all operators together (e.g., arithmetic + comparison + logical + IN)? [Coverage, Spec §Testing Requirements]

## Scenario Coverage — Evaluation Semantics

- [ ] CHK043 - Are requirements defined for `Boolean` field values in comparison context (e.g., `[active] == true`)? [Coverage, Gap]
- [ ] CHK044 - Are requirements defined for `Boolean` field values in arithmetic context (e.g., `[active] + 1`)? Should this throw? [Coverage, Gap]
- [ ] CHK045 - Are requirements defined for `IN` with mixed literal types (e.g., `[x] IN (1, 'two', true)`)? [Coverage, Edge Case]
- [ ] CHK046 - Are requirements defined for `IN` comparing numeric field to string literals and vice versa? Does `.equals()` handle cross-type? [Clarity, Spec §Visitor Contracts]
- [ ] CHK047 - Are requirements defined for `null` field value in `EvaluationContext` (e.g., `of("x", null)`)? [Coverage, Gap]
- [ ] CHK048 - Are requirements defined for chained comparisons (e.g., `[a] > [b] > [c]`)? Grammar allows only one comparison operator — is this explicitly stated? [Clarity, Spec §ANTLR Grammar]
- [ ] CHK049 - Are the numeric coercion requirements from clarifications reflected in both the evaluator contract and test specifications? [Completeness, Spec §Clarifications / §Testing Requirements]
- [ ] CHK050 - Are requirements defined for `pow(x, y)` vs `[x] ^ [y]` producing identical results? [Consistency, Spec §Built-in Functions / §ANTLR Grammar]

## Scenario Coverage — Error Handling

- [ ] CHK051 - Are ANTLR syntax error messages specified — should they include line/column information? [Clarity, Gap]
- [ ] CHK052 - Are requirements defined for partial parse success (e.g., `[x] + 1 garbage`)? Does ANTLR's `EOF` rule handle this? [Coverage, Spec §ANTLR Grammar]
- [ ] CHK053 - Are error message formats specified for type mismatch in ordering operators (string in `>`)? [Clarity, Gap]
- [ ] CHK054 - Are error message formats specified for wrong result type in `evaluateBoolean`/`evaluateDouble`? [Clarity, Spec §Public API]
- [ ] CHK055 - Are requirements defined for function arguments that are themselves errors (e.g., `abs([missing_field])`)? Is exception propagation behavior specified? [Coverage, Edge Case]
- [ ] CHK056 - Is the distinction between `ExpressionParseException` and `ExpressionEvaluationException` clear for all documented failure scenarios? [Clarity, Spec §Exception Contracts]

## Non-Functional Requirements

- [ ] CHK057 - Are thread safety requirements for `ExpressionEvaluator` specified (stateless? new instance per call?)? [Completeness, Gap]
- [ ] CHK058 - Are thread safety requirements for `EvaluationContext` specified (immutable after construction?)? [Completeness, Spec §Key Entities]
- [ ] CHK059 - Is the `BuiltinFunctionRegistry` thread safety requirement implicit from its static-only design? [Clarity, Spec §Built-in Functions]
- [ ] CHK060 - Are performance requirements absent intentionally (pure library, no I/O) or is this a gap? [Completeness, Spec §Assumptions]
- [ ] CHK061 - Are memory/resource cleanup requirements for ANTLR lexer/parser instances specified or unnecessary? [Coverage, Gap]

## Dependencies & Assumptions

- [ ] CHK062 - Is the Spring Boot BOM version (`3.5.0`) consistent with the "Spring Boot 3.5.x" assumption? [Consistency, Spec §Module 2 / §Assumptions]
- [ ] CHK063 - Is the assumption "consumers use Java 21+" validated against the parent POM compiler settings? [Consistency, Spec §Assumptions / Plan §Phase 1]
- [ ] CHK064 - Is the `commons-lang3` version specified or inherited from Spring Boot BOM? [Clarity, Spec §Module 1]
- [ ] CHK065 - Is the assumption that field values are only `Number`/`String`/`Boolean` documented as a public API contract or just an assumption? [Clarity, Spec §Assumptions / §Clarifications]
- [ ] CHK066 - Are ANTLR4 runtime and plugin versions pinned to the same `4.13.2`? [Consistency, Spec §Module 1]

## Testing Requirements Quality

- [ ] CHK067 - Are test methods for the 3 clarification decisions (numeric coercion, ordering on strings, mixed-type equality) included in the test specification? [Coverage, Gap]
- [ ] CHK068 - Is the `@ParameterizedTest` for case-insensitive boolean parsing specified with exhaustive variants (TRUE, True, true, FALSE, False, false)? [Completeness, Spec §Testing Requirements]
- [ ] CHK069 - Are `@DisplayName` values specified or left to implementation? Constitution requires them — is this traceable? [Clarity, Spec §Testing Requirements / Constitution §VII]
- [ ] CHK070 - Is the `BuiltinFunctionRegistryTest` structure (one `@Nested` per function) consistent with the constitution requirement of "one per method under test"? [Consistency, Spec §Testing Requirements / Constitution §VII]
- [ ] CHK071 - Are negative test cases for ordering operators on strings included in the test specification? [Coverage, Gap]
- [ ] CHK072 - Are test cases for `Number` subtype coercion (Integer, Long, BigDecimal field values) included? [Coverage, Gap]
- [ ] CHK073 - Are test cases for mixed-type equality (`==` number vs string → `false`) included? [Coverage, Gap]
- [ ] CHK074 - Is `EvaluationContextTest` missing test cases for `null` values or non-standard types? [Coverage, Gap]
- [ ] CHK075 - Are Spring autoconfiguration integration tests specified or intentionally excluded from core module tests? [Coverage, Spec §Testing Requirements]

## Notes

- Check items off as completed: `[x]`
- Items marked `[Gap]` indicate requirements that may need to be added to the spec
- Items marked `[Clarity]` indicate requirements that exist but may need sharper definition
- Items marked `[Consistency]` indicate potential conflicts between spec sections
- Total items: 75 (CHK001–CHK075)
