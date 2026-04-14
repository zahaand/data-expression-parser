# Implementation Plan: Dynamic IN / NOT IN Against Context Collection

**Branch**: `003-in-collection-context` | **Date**: 2026-04-14 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-in-collection-context/spec.md`

## Summary

v1.2.0 extends the existing `IN` / `NOT IN` operator so the right-hand side can be
a field reference resolving to a `List<Object>` from `EvaluationContext`, in
addition to the existing static literal list. The grammar gains two alternatives
(`additive IN FIELD`, `additive NOT IN FIELD`). The AST is refactored: a new
`InListNode` wraps the static list, and `InNode` becomes
`InNode(operand, collection, negated)` where `collection` is either an
`InListNode` (static) or a `FieldNode` (dynamic). `EvaluatingVisitor` branches on
the collection subtype, throws `ExpressionEvaluationException` with specific
messages for missing fields, non-List values, and unsupported element types, and
logs at ERROR before throwing per Constitution V. No new dependencies, no new
packages, no public API changes beyond the AST record rename.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: ANTLR 4.13.2, SLF4J API 2.0.x (no binding shipped), Spring Boot 3.5.x (starter module only)
**Storage**: N/A (library)
**Testing**: JUnit 5 (Jupiter), AssertJ, Mockito (only when mocks are actually used — per Constitution VII, Dev Standard #5)
**Target Platform**: JVM 21+, runtime-agnostic (core module has no Spring dependency)
**Project Type**: Maven multi-module library (`data-expression-parser-core` + `data-expression-parser-spring-boot-starter`)
**Performance Goals**: Parse + evaluate must not regress versus v1.1.0 (no new hot-path allocations beyond list iteration)
**Constraints**: Additive-only public API at the `DataExpressionParser` / `EvaluationContext` boundary; AST record rename is an accepted internal breaking change (spec §FR-207/FR-208)
**Scale/Scope**: Two AST files added/modified, one grammar file edited, two visitor branches updated, one new test class

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. Architecture (inward deps) | PASS | AST → visitors → evaluator → parser. No reverse deps added. Core still compiles without Spring. |
| II. Single Responsibility | PASS | `InListNode` holds static list; `InNode` wires operand + collection; evaluator branch handles two collection subtypes only. |
| III. Database Migrations | N/A | No DB. |
| IV. Secrets | N/A | No secrets. |
| V. Logging Standards (NON-NEGOTIABLE) | PASS | `EvaluatingVisitor` MUST log at ERROR before throwing `ExpressionEvaluationException` for the three new error conditions (FR-210). |
| VI. Class Member Ordering | PASS | New records have no instance state beyond record components; new visitor branches follow existing ordering. |
| VII. Testing Standards (NON-NEGOTIABLE) | PASS | New `InCollectionTest` uses JUnit 5 + AssertJ. No mocks needed — no `@ExtendWith(MockitoExtension.class)` added (would be orphaned per Dev Standard #5). |
| Dev Std #5 (no orphaned code) | PASS | Old `InNode(field, values)` shape is fully replaced; visitor branches rewritten; no leftover references. |
| Dev Std #10 (AST records) | PASS | `InListNode` is a record; `InNode` stays a record; both in `ru.zahaand.dataexpr.ast`; `InListNode` added to `Expression` permits. |
| Dev Std #11 (no Spring in core) | PASS | Only core AST/visitor/evaluator changes; no Spring imports. |
| Dev Std #12 (test coverage policy) | PASS | `InCollectionTest` covers positive, negative, and edge cases with `@ParameterizedTest` per spec Testing Requirements. |

No violations. Complexity Tracking section is empty.

## Project Structure

### Documentation (this feature)

```text
specs/003-in-collection-context/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── public-api.md    # Parser/AST/Context contracts
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
data-expression-parser-core/
├── src/main/antlr4/ru/zahaand/dataexpr/
│   └── DataExpression.g4                      # MODIFIED — 2 new comparison alternatives
├── src/main/java/ru/zahaand/dataexpr/
│   ├── ast/
│   │   ├── Expression.java                    # MODIFIED — permits += InListNode
│   │   ├── InNode.java                        # MODIFIED — (operand, collection, negated)
│   │   └── InListNode.java                    # NEW record
│   ├── visitor/
│   │   ├── AstBuildingVisitor.java            # MODIFIED — visitComparison branches
│   │   └── EvaluatingVisitor.java             # MODIFIED — InNode evaluation branch
│   └── evaluator/
│       └── EvaluationContext.java             # UNCHANGED — List<Object> already accepted
└── src/test/java/ru/zahaand/dataexpr/
    ├── InCollectionTest.java                  # NEW
    └── DataExpressionParserTest.java          # MODIFIED — Parse group additions

data-expression-parser-spring-boot-starter/    # UNCHANGED

pom.xml                                         # MODIFIED — version 1.1.0 → 1.2.0
data-expression-parser-core/pom.xml             # MODIFIED — parent version bump
data-expression-parser-spring-boot-starter/pom.xml  # MODIFIED — parent version bump
```

**Structure Decision**: Maven multi-module library (existing layout from v1.0.0/v1.1.0).
All changes isolated to the core module; starter unchanged. No new packages.

## Complexity Tracking

> No constitution violations. Section intentionally empty.
