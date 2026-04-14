# Tasks: Dynamic IN / NOT IN Against Context Collection (v1.2.0)

**Branch**: `003-in-collection-context`
**Spec**: [spec.md](./spec.md)
**Plan**: [plan.md](./plan.md)

## Overview

Single user story — **US1: Dynamic IN/NOT IN against context collection** — covering all functional requirements (FR-201..FR-210a). Tests are requested in the spec and included.

---

## Phase 1: Setup (version bumps)

**Goal**: Bump parent version `1.1.0` → `1.2.0` in all three pom files; confirm compile.

- [ ] T001 [P] Bump parent project version `1.1.0` → `1.2.0` in `pom.xml`
- [ ] T002 [P] Update parent-reference version to `1.2.0` in `data-expression-parser-core/pom.xml`
- [ ] T003 [P] Update parent-reference version and the `data-expression-parser-core` dependency version to `1.2.0` in `data-expression-parser-spring-boot-starter/pom.xml`
- [ ] T004 Run `mvn -q compile` at repo root and confirm both modules compile with no errors (commit gate for Phase 1: `chore: bump version to 1.2.0`)

**Independent test criterion**: `mvn -q compile` succeeds; all poms report `1.2.0`.

---

## Phase 2: Foundational — AST refactor

**Goal**: Refactor `InNode` and introduce `InListNode`. Update `Expression` permits. Must compile before visitors are edited.

**Blocks**: all US1 tasks.

- [ ] T005 [P] Create new record `InListNode(List<Expression> values)` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/InListNode.java` — implements `Expression` sealed interface
- [ ] T006 Refactor `InNode` to `InNode(Expression operand, Expression collection, boolean negated)` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/InNode.java` (rename `field`→`operand`, `values`→`collection`, add `negated`)
- [ ] T007 Add `InListNode` to the `permits` clause of `Expression` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/ast/Expression.java`

**Checkpoint**: `mvn -q compile` will fail here — visitors still reference old `InNode` shape. Proceed to US1 without committing.

---

## Phase 3: US1 — Dynamic IN/NOT IN against context collection

**Story goal**: Support `[operand] IN [field]` and `[operand] NOT IN [field]` where `[field]` resolves to a `List<Object>` from `EvaluationContext`, with three specified error paths and ERROR-level logging before each throw. Static `IN (...)` continues to work unchanged.

**Independent test criterion**: `InCollectionTest` passes; `DataExpressionParserTest` Parse group additions pass; existing 244-test suite stays green.

### Grammar + AST wiring

- [ ] T008 [US1] Extend `comparison` rule in `data-expression-parser-core/src/main/antlr4/ru/zahaand/dataexpr/DataExpression.g4` with two new alternatives: `additive IN FIELD` and `additive NOT IN FIELD`
- [ ] T009 [US1] Update `AstBuildingVisitor.visitComparison` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/visitor/AstBuildingVisitor.java` to emit `InNode(operand, InListNode(values), false)` for static `IN (...)`, `InNode(operand, InListNode(values), true)` for static `NOT IN (...)`, `InNode(operand, FieldNode(name), false)` for `IN FIELD`, and `InNode(operand, FieldNode(name), true)` for `NOT IN FIELD` (strip `[` / `]` from FIELD token text)

### Evaluator branch

- [ ] T010 [US1] Update `InNode` evaluation branch in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/visitor/EvaluatingVisitor.java`: handle `collection instanceof InListNode` (existing static list logic, unchanged behavior) and `collection instanceof FieldNode` (dynamic list logic per spec §Visitor Changes); return `BooleanResult(negated != found)`
- [ ] T011 [US1] In `EvaluatingVisitor`, implement the three error branches for the dynamic-collection path: (a) missing field — throw `ExpressionEvaluationException("Field '<name>' not found in context")`; (b) non-List value — throw `ExpressionEvaluationException("Field '<name>' must be a List for IN operator, got: <simpleName>")`; (c) unsupported element type (including `null`) — throw `ExpressionEvaluationException("Collection field '<name>' contains unsupported element type: <simpleName or 'null'>")`
- [ ] T012 [US1] In `EvaluatingVisitor`, add `log.error("IN operator error for field '{}': {}", fieldName, exceptionMessage)` immediately before each of the three throws in T011 (Constitution V — NON-NEGOTIABLE; FR-210, FR-210a)

### Compile gate

- [ ] T013 [US1] Run `mvn -q compile` at repo root; fix any remaining reference to old `InNode` fields. Commit Phase 2 + US1 source changes as `feat(core): support dynamic IN/NOT IN against context collection`

### Tests

- [ ] T014 [P] [US1] Create `InCollectionTest` at `data-expression-parser-core/src/test/java/ru/zahaand/dataexpr/InCollectionTest.java` with `@Nested` group `EvaluateBoolean` containing: `shouldReturnTrueWhenFieldValueIsInStringCollection`, `shouldReturnFalseWhenFieldValueIsNotInStringCollection`, `shouldReturnTrueWhenFieldValueIsInNumericCollection`, `shouldReturnFalseWhenFieldValueIsNotInNumericCollection`, `shouldReturnTrueForNotInWhenValueAbsent`, `shouldReturnFalseForNotInWhenValuePresent`, `shouldHandleMixedTypeCollectionWithNoMatch`, `shouldReturnFalseWhenCollectionIsEmpty`
- [ ] T015 [P] [US1] Add `@Nested` group `EvaluateBooleanParameterized` to `InCollectionTest` with a single `@ParameterizedTest` `shouldEvaluateInCollectionExpression` + `@MethodSource` `Stream<Arguments>` covering positive and negative cases for both `IN` and `NOT IN` with string and numeric collections
- [ ] T016 [P] [US1] Add `@Nested` group `Errors` to `InCollectionTest`: `shouldThrowWhenCollectionFieldIsNotAList` (asserts `hasMessageContaining("must be a List for IN operator, got:")`), `shouldThrowWhenCollectionContainsUnsupportedElementType` (asserts both a non-scalar type and a `null` element via a parameterized test), `shouldThrowWhenCollectionFieldDoesNotExist` (asserts `hasMessageContaining("not found in context")`)
- [ ] T017 [US1] Add to existing `Parse` `@Nested` group in `data-expression-parser-core/src/test/java/ru/zahaand/dataexpr/DataExpressionParserTest.java`: `shouldReturnInNodeWithFieldNodeCollectionForDynamicIn`, `shouldReturnInNodeWithFieldNodeCollectionForDynamicNotIn`, `shouldReturnInNodeWithInListNodeCollectionForStaticIn` — each asserts the AST structure via `parseInternal` or equivalent introspection

### Test gate

- [ ] T018 [US1] Run `mvn -q test -pl data-expression-parser-core` and confirm BUILD SUCCESS with test count ≥ 244 baseline + new tests; commit test changes as `test(core): add coverage for dynamic IN/NOT IN`

---

## Phase 4: Polish & Verification

**Goal**: Final cross-module build, documentation sanity, constitution gate re-check.

- [ ] T019 Run `mvn -q verify` at repo root; confirm BUILD SUCCESS for both modules and both JARs produced
- [ ] T020 [P] Confirm `data-expression-parser-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` is unchanged (no starter changes expected)
- [ ] T021 [P] Review `EvaluatingVisitor` diff to confirm no orphaned code (Dev Standard #5): old `InNode.field` / `InNode.values` references are fully removed
- [ ] T022 Mark all tasks `[X]` in this file and commit as `docs: mark all tasks complete in sprint 003`

---

## Dependencies

```
Phase 1 (Setup: T001–T004)
   └─▶ Phase 2 (Foundational AST: T005–T007)
          └─▶ Phase 3 (US1)
                 T008 ─▶ T009 ─▶ T010 ─▶ T011 ─▶ T012 ─▶ T013
                                                           ├─▶ T014 (P)
                                                           ├─▶ T015 (P, depends on T014)
                                                           ├─▶ T016 (P, depends on T014)
                                                           └─▶ T017
                                                                 └─▶ T018
                        └─▶ Phase 4 (Polish: T019–T022)
```

- T001–T003 are parallel-safe (different pom files).
- T005 is parallel-safe with T006 (different files), but T007 depends on T005 (permits clause references `InListNode`).
- T008–T013 are sequential: grammar → AST building → evaluator branch → error branches → logging → compile gate.
- T014 creates the test class; T015/T016 add nested groups inside it — in practice T015/T016 depend on T014 being merged/saved first. Mark `[P]` because they are distinct `@Nested` classes touching different logical sections.
- T017 is independent (different file).
- T018 gates all test work.
- Phase 4 runs after Phase 3 is green.

---

## Parallel Execution Examples

### Setup (Phase 1)

Run T001, T002, T003 in parallel — three separate pom files.

### AST foundational (Phase 2)

Run T005 and T006 in parallel. T007 sequential after T005.

### US1 tests (Phase 3)

Once T014 is saved, T015, T016, T017 can proceed in parallel — each touches a distinct `@Nested` group or different file.

### Polish (Phase 4)

T020 and T021 in parallel (read-only checks).

---

## Implementation Strategy

**MVP** = Phase 1 + Phase 2 + Phase 3 tasks T008–T013 — source-level completeness of the feature. Delivers a compilable `mvn -q compile` pass with the new grammar and evaluator behavior.

**Incremental delivery**:

1. **Commit A** after T004 — `chore: bump version to 1.2.0`
2. **Commit B** after T013 — `feat(core): support dynamic IN/NOT IN against context collection`
3. **Commit C** after T018 — `test(core): add coverage for dynamic IN/NOT IN`
4. **Commit D** after T019 — (no changes, gate only)
5. **Commit E** after T022 — `docs: mark all tasks complete in sprint 003`

Each commit leaves `mvn -q verify` green at its respective scope (compile-only after A; full test pass after C).

---

## Format Validation

All 22 tasks follow the checklist format:
- `- [ ]` checkbox ✓
- `T###` sequential ID ✓
- `[P]` marker on truly parallel tasks only ✓
- `[US1]` label on all Phase 3 tasks (Setup, Foundational, Polish have no story label) ✓
- Clear description with exact file path ✓
