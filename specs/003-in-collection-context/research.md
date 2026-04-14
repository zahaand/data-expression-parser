# Phase 0 — Research

**Feature**: Dynamic IN / NOT IN Against Context Collection
**Branch**: `003-in-collection-context`

## Summary

The spec is self-contained and mostly prescriptive (grammar, AST, visitor pseudocode, error messages). Research focuses on confirming ANTLR grammar alternative dispatch, AST record breaking change cost, and evaluator error-path consistency with v1.0.0/v1.1.0 patterns.

## Decisions

### D1 — Grammar alternative dispatch

- **Decision**: Add two new top-level alternatives to `comparison`: `additive IN FIELD` and `additive NOT IN FIELD`. Keep the existing static-list alternatives as-is.
- **Rationale**: ANTLR dispatches alternatives by lookahead; `'('` vs `FIELD` provides unambiguous LL(1) disambiguation. Minimal grammar change, no parser ambiguity.
- **Alternatives considered**:
  - Factor a shared `inRhs` sub-rule → rejected: adds indirection for a 2-variant fork; complicates `AstBuildingVisitor` without payoff.
  - Reuse `FIELD` inside `valueList` → rejected: would silently change semantics of static list (allow field mixed with literals) and increase test surface.

### D2 — AST shape: unified `InNode` + separate `InListNode`

- **Decision**: `InNode(operand, collection, negated)` where `collection` is `Expression` (always `InListNode` or `FieldNode` in practice). Static list is wrapped in `InListNode(List<Expression>)`.
- **Rationale**: Visitor polymorphism over a single evaluation branch. Pattern-matching `instanceof InListNode` / `instanceof FieldNode` keeps the logic in one place. Consistent with the existing sealed-interface + record AST style (Dev Standard #10).
- **Alternatives considered**:
  - Separate `InListNode` and `InFieldNode` top-level in `Expression` permits → rejected: duplicates the "IN-ness" (operand + negated) across two records; increases visitor branches.
  - Keep `InNode(field, values)` and add `InFieldNode` → rejected: forces two divergent evaluation paths and leaves `field`/`values` naming misleading once field-collection variant exists.

### D3 — Breaking change acceptance for AST record rename

- **Decision**: Accept the `InNode` field rename (`field`→`operand`, `values`→`collection`) as an internal-but-public record change. Document in spec §FR-207/FR-208 and Assumptions.
- **Rationale**: AST types are not part of the stable consumer-facing API surface (`DataExpressionParser`, `EvaluationContext`, `ValidationResult`, `CustomFunctionRegistry`, `ExpressionEvaluationException`). Consumers who construct `InNode` directly are bypassing the parser — a non-supported use case.
- **Alternatives considered**:
  - Keep original field names, add collection-variant via wildcard typing → rejected: semantic mismatch (`values` would sometimes be `null`).
  - Introduce a `@Deprecated` bridge constructor → rejected: bridge layer for a hypothetical consumer adds orphaned code (Dev Standard #5).

### D4 — Missing-field exception message (clarification Q1 → option D)

- **Decision**: Throw `ExpressionEvaluationException("Field '<name>' not found in context")` when the RHS field is absent. Generic, not IN-specific.
- **Rationale**: Locked by spec Clarifications §Session 2026-04-14. Matches the existing library style of returning actionable diagnostics while not adding operator-specific context that the caller can infer from the expression text.
- **Alternatives considered**: see spec Clarifications Q1 options A/B/C.

### D5 — Null operand behavior (clarification Q2 → option A)

- **Decision**: Operand `null` (missing context field on LHS) routes through the existing field-resolution error path — no special IN-operator handling.
- **Rationale**: Locked by spec Clarifications. Consistent with how other operators (`>`, `==`, etc.) currently handle missing fields.

### D6 — Element type whitelist (Number, String, Boolean)

- **Decision**: Reject elements that are not `Number`, `String`, or `Boolean` with `"Collection field '<name>' contains unsupported element type: <type>"`.
- **Rationale**: Mirrors the types accepted by the static `valueList` at parse time. Nested lists, maps, and arbitrary objects have no defined equality under `isEqual()`.
- **Alternatives considered**:
  - Accept arbitrary objects and fall back to `.equals()` → rejected: surprises consumers (`Integer(1)` vs `Long(1L)` would diverge from the rest of the library's numeric coercion).

### D7 — Logging level for collection errors

- **Decision**: `ERROR` log before throwing `ExpressionEvaluationException` in all three new failure branches (missing field, non-List value, unsupported element type). Uses existing `log` field on `EvaluatingVisitor`.
- **Rationale**: Constitution V (NON-NEGOTIABLE) and spec FR-210. Matches the pattern established in v1.1.0 for custom-function errors (WARN before throw in `EvaluatingVisitor.evaluateFunction`) and Builder validation (ERROR before throw).
- **Note**: Constitution V table shows ERROR for "unhandled exception". These failures are thrown synchronously as `ExpressionEvaluationException` — handled by the evaluator's contract but unrecoverable from the visitor's perspective. ERROR is the appropriate level.

### D8 — Performance posture

- **Decision**: Iterate the `List` once per evaluation; short-circuit on match. No caching across calls (evaluations are assumed independent).
- **Rationale**: Matches existing static-list semantics. Consumers who need cached membership tests should pre-compute a `Set` and use custom functions (v1.1.0).
- **Alternatives considered**: Auto-convert to `Set` if size > N → rejected: premature optimization, adds hidden allocation.

## Open questions

None. All NEEDS CLARIFICATION markers from spec §Clarifications are resolved.

## References

- `data-expression-parser-core/src/main/antlr4/ru/zahaand/dataexpr/DataExpression.g4` — current grammar
- `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/visitor/EvaluatingVisitor.java` — existing `InNode` branch, `isEqual()`, custom-function WARN pattern
- `.specify/memory/constitution.md` — V (logging), VII (testing), Dev Standards #5, #10, #11, #12
- `specs/003-in-collection-context/spec.md` §Clarifications — Q1 (D), Q2 (A)
