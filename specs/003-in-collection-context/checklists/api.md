# API & Error Contracts — Requirements Quality Checklist

**Feature**: Dynamic IN / NOT IN Against Context Collection (v1.2.0)
**Purpose**: Unit-test the spec's API and error-contract requirements before `/speckit.tasks`.
**Audience**: Author, pre-tasks
**Depth**: Standard
**Created**: 2026-04-14

## Grammar & Syntax Requirements

- [ ] CHK001 - Are both `IN [field]` and `NOT IN [field]` alternatives explicitly enumerated in the grammar rule? [Completeness, Spec §Grammar Changes]
- [ ] CHK002 - Is the LL(1) disambiguation between `IN '(' valueList ')'` and `IN FIELD` addressed in the spec? [Clarity, Spec §Grammar Changes]
- [ ] CHK003 - Is the `FIELD` token's bracket-stripping behavior described consistently with existing field references? [Consistency, Spec §Visitor Changes]
- [ ] CHK004 - Are grammar requirements explicit about whether the operand (LHS) can be any `additive` expression or only a field reference? [Clarity, Spec §FR-201]

## AST Contract Requirements

- [ ] CHK005 - Is the `InNode` record field rename (`field`→`operand`, `values`→`collection`) flagged as a breaking change with explicit version impact? [Completeness, Spec §FR-207, §FR-208]
- [ ] CHK006 - Does the spec specify that `InNode.collection` is constrained to exactly `InListNode` or `FieldNode`? [Clarity, Spec §Data Model]
- [ ] CHK007 - Is `InListNode` added to the `Expression` sealed `permits` clause in all three locations (grammar, AST, visitor)? [Consistency, Spec §AST Changes]
- [ ] CHK008 - Is the rationale for AST breaking change documented (AST not part of public API)? [Assumption, Spec §FR-208, §Assumptions]

## Exception Message Contracts

- [ ] CHK009 - Is the missing-field error message string specified character-for-character? [Clarity, Spec §FR-204a]
- [ ] CHK010 - Is the non-List type error message specified character-for-character including `<type>` placeholder resolution (e.g., `getSimpleName()`)? [Clarity, Spec §FR-204]
- [ ] CHK011 - Is the unsupported-element-type error message specified character-for-character? [Clarity, Spec §FR-205]
- [ ] CHK012 - Are the three exception messages consistent in tone, quoting style, and field-name placeholder format? [Consistency, Spec §Exception Contracts]
- [ ] CHK013 - Is the exception type (`ExpressionEvaluationException`) consistent across all three error branches? [Consistency, Spec §Exception Contracts]

## Element Type Whitelist Requirements

- [ ] CHK014 - Is the accepted element type set (`Number`, `String`, `Boolean`) defined unambiguously, with no room for subclass interpretation? [Clarity, Spec §FR-205]
- [ ] CHK015 - Does the spec define behavior when the collection contains a `null` element (supported type or rejected)? [Gap, Edge Case]
- [ ] CHK016 - Is element comparison semantics (delegation to `isEqual()` including numeric coercion) explicitly inherited, not redefined? [Consistency, Spec §FR-203]

## Logging Requirements

- [ ] CHK017 - Is the required log level (`ERROR`) specified for each of the three new error conditions? [Completeness, Spec §FR-210]
- [ ] CHK018 - Is the ordering requirement "log before throw" stated, aligning with Constitution V? [Clarity, Spec §FR-210]
- [ ] CHK019 - Are log message contents specified (or deliberately left to implementation) to avoid log/exception-message drift? [Gap, Consistency]

## Backward Compatibility Requirements

- [ ] CHK020 - Is the static `IN ('a', 'b')` syntax preservation explicitly asserted as a requirement, not an assumption? [Completeness, Spec §FR-206]
- [ ] CHK021 - Is the public API surface (`DataExpressionParser`, `EvaluationContext`, `ValidationResult`, `CustomFunctionRegistry`) declared unchanged? [Completeness, Spec §FR-208]
- [ ] CHK022 - Does the spec define how `validate()` treats `IN [field]` (syntactic-only, no runtime field check)? [Clarity, Spec §Testing Requirements]

## Edge Case Coverage

- [ ] CHK023 - Is empty-list behavior (`IN` returns `false`, `NOT IN` returns `true`) specified as a requirement, not only in Assumptions? [Measurability, Spec §Assumptions]
- [ ] CHK024 - Is operand-null behavior (missing LHS field) routed to an existing specified error path rather than silently undefined? [Coverage, Spec Clarifications Q2]
- [ ] CHK025 - Does the spec address whether duplicate elements in the list are permitted and treated as a single match or multiple? [Gap, Edge Case]
- [ ] CHK026 - Does the spec define behavior when the same collection field appears on both sides of the operator (e.g., `[a] IN [a]`)? [Gap, Edge Case]

## Success Criteria Quality

- [ ] CHK027 - Are all Success Criteria (SC-201..SC-207) independently measurable without implementation knowledge? [Measurability, Spec §Success Criteria]
- [ ] CHK028 - Does at least one Success Criterion cover each new error branch (missing field, non-List, unsupported element)? [Coverage, Spec §Success Criteria]

## Assumptions & Dependencies

- [ ] CHK029 - Is the assumption "`EvaluationContext.of()` already accepts `List<Object>`" validated against v1.1.0 source, not merely asserted? [Assumption, Spec §FR-209]
- [ ] CHK030 - Is the "no new external dependencies" claim cross-checked against Spring Boot starter's transitive deps? [Dependency, Spec §Maven Coordinates]
