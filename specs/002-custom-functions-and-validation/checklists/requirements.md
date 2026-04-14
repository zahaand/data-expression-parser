# Requirements Quality Checklist: Custom Functions & Validation (v1.1.0)

**Purpose**: Formal author self-review validating the quality of v1.1.0 requirements across API surface, exception contracts, and test coverage completeness — before `/speckit.tasks`.
**Created**: 2026-04-14
**Feature**: [spec.md](../spec.md) · [plan.md](../plan.md)

**Scope**: Tests whether v1.1.0 requirements are written well — complete, clear, consistent, measurable, and fully covered by the listed tests. Does NOT test whether the implementation works.

---

## API Surface — Completeness

- [ ] CHK001 Are the exact signatures of `ExpressionFunction.apply`, `CustomFunctionRegistry.empty/builder/find`, and `ValidationResult.valid/invalid/isValid/errorMessage` specified without ambiguity? [Completeness, Spec §New Packages / Classes]
- [ ] CHK002 Is the visibility (`public final`, package-private) specified for every new and modified class in `ru.zahaand.dataexpr`? [Completeness, Spec §New Packages / Classes, Plan §Phase 2]
- [ ] CHK003 Are both constructors of `DataExpressionParser` (v1.0.0 single-arg and v1.1.0 two-arg) specified with delegation semantics between them? [Completeness, Spec §Modified Classes, FR-116]
- [ ] CHK004 Are the constructor changes on `ExpressionEvaluator` documented (new registry-aware ctor + no-arg delegation for backward compatibility)? [Completeness, Plan §Phase 3]
- [ ] CHK005 Does the spec define how `BuiltinFunctionRegistry` exposes its built-in name set to `CustomFunctionRegistry.Builder` without leaking to the public API? [Gap, Plan §Phase 2]
- [ ] CHK006 Is the `@FunctionalInterface` annotation requirement on `ExpressionFunction` explicit, and is its single-abstract-method contract unambiguous? [Completeness, Spec §ExpressionFunction]
- [ ] CHK007 Are the autoconfiguration bean signatures — `customFunctionRegistry()`, updated `expressionEvaluator(...)`, updated `dataExpressionParser(...)` — fully specified with `@ConditionalOnMissingBean` semantics? [Completeness, Spec §DataExpressionParserAutoConfiguration, Plan §Phase 4]

## API Surface — Clarity & Measurability

- [ ] CHK008 Is the function-name regex `^[a-zA-Z_][a-zA-Z_0-9]*$` stated identically in FR-105a, the `Builder` Javadoc, and the test description? [Clarity & Consistency, Spec §FR-105a]
- [ ] CHK009 Is the case-insensitive storage/lookup rule (lower-case via `Locale.ROOT` vs. default locale) specified unambiguously to avoid Turkish-locale `"I"→"ı"` pitfalls? [Clarity, Gap, Spec §FR-103]
- [ ] CHK010 Is "immutable after `build()`" measurable — does the spec require `Builder` mutations after `build()` to have no effect on the built registry? [Measurability, Spec §FR-115]
- [ ] CHK011 Can thread-safety claims for `CustomFunctionRegistry` and `ValidationResult` be objectively verified (e.g. documented as "safe for publication after construction, no mutable state")? [Measurability, Spec §FR-115]

## Exception Contracts — Completeness

- [ ] CHK012 Are all `IllegalArgumentException` trigger conditions enumerated exhaustively (null/blank name, bad identifier, built-in conflict, duplicate, null function) in one place? [Completeness, Spec §Exception Contracts (Additions)]
- [ ] CHK013 Is the behavior specified when a custom function lambda is itself `null` in `register(name, null)`? [Gap, Spec §FR-104..FR-105b]
- [ ] CHK014 Is the behavior specified when `BuiltinFunctionRegistry.find(name)` is reached via fallback with an unknown name (i.e. the `"Unknown function: '<name>'"` message format re-asserted for v1.1.0)? [Completeness, Spec §FR-108]
- [ ] CHK015 Does the spec cover behavior when `validate()` receives an expression that lexes but produces a parser-level error vs. lexer-level error (both must be captured, not throw)? [Gap, Coverage, Spec §FR-109..FR-112]

## Exception Contracts — Clarity & Consistency

- [ ] CHK016 Are the exact exception message formats quoted consistently across the spec: `"Error in custom function '<name>': <msg>"`, `"Function name '<name>' conflicts with built-in function"`, `"Custom function '<name>' is already registered"`, `"Parse error at line <L>:<C>: <antlr_message>"`? [Consistency, Spec §FR-108a, §FR-105, §FR-105b, §FR-111]
- [ ] CHK017 Is the ordering of validation checks inside `Builder.register()` specified (null/blank → regex → built-in → duplicate → null-function) so that tests can assert the *first* error surfaced? [Clarity, Plan §Phase 2]
- [ ] CHK018 Is the rule "wrap `RuntimeException` as `ExpressionEvaluationException` with original as `cause`" consistent between FR-108a, the Modified Classes section, and the Clarifications session bullet? [Consistency, Spec §FR-108a]
- [ ] CHK019 Is the FR-108b "no arity capture" rule consistent with the library not pre-validating args — i.e. no contradiction elsewhere where the library is said to validate custom arity? [Consistency, Spec §FR-108b]
- [ ] CHK020 Does `ValidationResult.invalid(null)` or `invalid("")` behavior have an explicit contract (plan says `IllegalArgumentException`, spec is silent)? [Conflict, Plan §Phase 2 vs Spec §ValidationResult]

## Backward Compatibility (v1.0.0 → v1.1.0)

- [ ] CHK021 Are all v1.0.0 public constructors, methods, and exception messages explicitly asserted as unchanged in FR-116, with a mechanism (tests) to catch accidental breakage? [Completeness, Spec §FR-116]
- [ ] CHK022 Is the behavior of a v1.0.0 consumer who supplies only `ExpressionEvaluator` (no `CustomFunctionRegistry`) specified as identical to v1.0.0 evaluation semantics? [Consistency, Spec §FR-116, Plan §Complexity Tracking]
- [ ] CHK023 Does the starter update preserve override behavior — any of the three beans (`ExpressionEvaluator`, `CustomFunctionRegistry`, `DataExpressionParser`) overridable via `@ConditionalOnMissingBean`? [Completeness, Plan §Phase 4]

## Logging Requirements (SLF4J)

- [ ] CHK024 Are the exact log levels specified per event (WARN for custom function failure, DEBUG for validation invalid result) with the message content expected at each site? [Clarity, Spec §FR-117]
- [ ] CHK025 Is the requirement "SLF4J API only, no binding shipped" measurable and testable (e.g. by inspecting published POMs for absence of `slf4j-simple`/`logback-classic` runtime scope)? [Measurability, Spec §FR-117]
- [ ] CHK026 Are logging requirements silent on error-level logs in `Builder.register()` — the plan adds `log.error` before each throw but the spec doesn't mention it? [Conflict, Plan §Phase 2 vs Spec §FR-117]

## Validation Method (`validate`) — Scenario Coverage

- [ ] CHK027 Are requirements defined for both the "valid syntax, unknown function" and "valid syntax, unknown field" scenarios of `validate()` (spec says both must return `valid()`)? [Coverage, Spec §FR-112, §Edge Cases]
- [ ] CHK028 Are recovery / partial-error scenarios addressed — e.g. does `validate()` return the first error or all errors? If first-only, is that explicit? [Gap, Coverage, Spec §FR-111]
- [ ] CHK029 Is the `Optional<String> errorMessage()` invariant testable on both branches — empty when valid, present-and-non-blank when invalid? [Measurability, Spec §FR-113]

## Test Coverage — Traceability to FRs/SCs

- [ ] CHK030 Does each FR-101 through FR-117 have at least one listed test in the Testing Requirements section, and is the mapping explicit or easily derivable? [Traceability, Spec §Testing Requirements]
- [ ] CHK031 Is there a test listed for FR-105a (grammar-ID pattern rejection) covering at least one failure category each: leading digit, hyphen, space, special character? [Coverage, Spec §CustomFunctionRegistryTest]
- [ ] CHK032 Is there a test for FR-105b (duplicate registration) that explicitly verifies case-insensitive duplicate detection (`"TAX"` then `"tax"` throws)? [Coverage, Spec §CustomFunctionRegistryTest]
- [ ] CHK033 Is there a test for FR-108a that asserts both the wrapped exception *type* and that `getCause()` returns the original `RuntimeException`? [Coverage, Spec §Evaluation tests]
- [ ] CHK034 Is there a test for FR-108b (self-validated arity) demonstrating that a lambda throwing on `args.length != expected` is wrapped per FR-108a? [Coverage, Spec §Evaluation tests]
- [ ] CHK035 Is there a test for FR-117 logging contract (WARN on custom function failure / DEBUG on validation invalid), or is logging intentionally excluded from the test matrix? [Gap, Coverage, Spec §FR-117]
- [ ] CHK036 Is there a test asserting the `"Parse error at line <L>:<C>: ..."` message format on `ValidationResult.invalid(...)` and that it matches the v1.0.0 `AstBuildingVisitor` format? [Coverage & Consistency, Spec §FR-111, §ValidationResultTest]
- [ ] CHK037 Is there a test asserting v1.0.0 `DataExpressionParser(ExpressionEvaluator)` single-arg constructor still functions (delegation path), not only the new two-arg constructor? [Coverage, Gap, Spec §FR-116]

## Measurable Success Criteria

- [ ] CHK038 Are SC-101 through SC-108 stated as objectively verifiable outcomes (pass/fail at CI), with no vague terms like "works correctly" or "successful"? [Measurability, Spec §Measurable Outcomes]
- [ ] CHK039 Does SC-102 specify the test count delta from v1.0.0 (e.g. "full v1.0.0 suite plus the tests listed in §Testing Requirements") rather than an opaque "all tests pass"? [Clarity, Spec §SC-102]
- [ ] CHK040 Can SC-108 ("same behavior as v1.0.0 for consumers without a custom registry bean") be objectively checked, e.g. via a Spring Boot test context that boots the starter with no user-defined beans? [Measurability, Spec §SC-108]

## Ambiguities & Conflicts

- [ ] CHK041 Is the `ValidationResult.invalid(null/"")` contract aligned between spec (silent) and plan (throws `IllegalArgumentException`)? Resolve by promoting the plan's rule into FR-113 or explicitly marking it internal. [Conflict, Spec §FR-113 vs Plan §Phase 2]
- [ ] CHK042 Does the spec explicitly state locale for `toLowerCase` (plan says `Locale.ROOT`; spec says "via `toLowerCase`" without locale)? [Ambiguity, Spec §FR-103 vs Plan §Phase 2]
- [ ] CHK043 Is the registered-name storage ordering guarantee specified (plan uses `LinkedHashMap`, spec is silent on iteration order)? [Gap, Plan §Phase 2]
- [ ] CHK044 Does the spec distinguish "fail at registration" (FR-105, FR-105a, FR-105b) from "fail at build" — is it acceptable for `build()` to also re-check, or must all checks happen strictly at `register()`? [Ambiguity, Spec §FR-104..FR-105b]
- [ ] CHK045 Is the phrase "additive" (FR-116) precise enough to forbid behavior changes to existing methods, not only signature preservation? [Clarity, Spec §FR-116]

## Dependencies & Assumptions

- [ ] CHK046 Is the new SLF4J dependency declared in a single authoritative location (parent `dependencyManagement`) and the version source stated (BOM vs. pinned)? [Completeness, Plan §Phase 1]
- [ ] CHK047 Is the assumption "custom functions are pure and reasonably fast" documented as a non-enforced consumer contract rather than a library guarantee? [Assumption, Spec §Assumptions]
- [ ] CHK048 Is the assumption that `validate()` consumers will not rely on it for performance-critical paths stated explicitly (spec suggests design-time use only)? [Assumption, Spec §Assumptions]

---

## Notes

- Check items off as completed: `[x]`
- Mark any unresolvable conflict with an inline `→ resolution:` note pointing to the FR/Clarification update needed
- Items labeled `[Gap]` require amending `spec.md`; items labeled `[Conflict]` require reconciling spec vs. plan
- Re-run `/speckit.checklist` after spec amendments only if new requirement categories are added; otherwise update items in-place
