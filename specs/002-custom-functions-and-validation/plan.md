# Implementation Plan: Custom Functions & Validation (v1.1.0)

**Branch**: `002-custom-functions-and-validation` | **Date**: 2026-04-14 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-custom-functions-and-validation/spec.md`

## Summary

Extend `data-expression-parser` from v1.0.0 to v1.1.0 with two capabilities: (1) consumer-defined
custom functions via `CustomFunctionRegistry` with fail-fast validation at registration, and
(2) syntactic expression validation via `DataExpressionParser.validate(...)` returning
`ValidationResult`. All v1.0.0 public contracts remain unchanged; additions are strictly
additive. SLF4J logging is introduced (API only, no binding) for custom-function failures
(`WARN`) and validation failures (`DEBUG`).

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: ANTLR4 4.13.2, Apache Commons Lang 3, SLF4J API (new), Spring Boot 3.5.x (starter only)
**Storage**: N/A
**Testing**: JUnit 5 + AssertJ + Mockito
**Target Platform**: JVM (library JAR)
**Project Type**: Library (Maven Multi-Module: core + Spring Boot starter)
**Performance Goals**: N/A (pure computation library)
**Constraints**: Core module MUST NOT depend on Spring; stateless and thread-safe; no SLF4J binding shipped
**Scale/Scope**: +3 new public types (`ExpressionFunction`, `CustomFunctionRegistry`, `ValidationResult`); additive changes to `DataExpressionParser`, `ExpressionEvaluator`, `EvaluatingVisitor`, `DataExpressionParserAutoConfiguration`

## Constitution Check

*GATE: Must pass before starting Phase 1. Re-check after each phase.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Architecture | PASS | Maven Multi-Module (core + starter); no structural change |
| II. SRP | PASS | `CustomFunctionRegistry` = storage/lookup; `ValidationResult` = value holder; evaluation logic stays in `EvaluatingVisitor` |
| III. Database Migrations | N/A | No database layer |
| IV. Secrets | N/A | No runtime secrets |
| V. Logging | PASS | `LoggerFactory.getLogger()` declaration; no Lombok `@Slf4j` |
| VI. Code Style | PASS | `final` classes; static factories; constructor injection |
| VII. Testing | PASS | JUnit 5 + `@Nested` + `@DisplayName` + `@ParameterizedTest` |
| VIII. Language | PASS | English; Conventional Commits |
| IX. Simplicity | PASS | No arity capture; no logging binding; minimal surface |
| Dev Standard #9 | N/A | Grammar unchanged |
| Dev Standard #10 | N/A | No new AST nodes |
| Dev Standard #11 | PASS | `CustomFunctionRegistry`, `ValidationResult`, `ExpressionFunction` live in core and carry zero Spring annotations |

## Project Structure

### Documentation (this feature)

```text
specs/002-custom-functions-and-validation/
├── spec.md              # Feature specification (incl. Clarifications)
└── plan.md              # This file
```

### Source Code Additions (repository root)

```text
data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/
├── function/
│   ├── BuiltinFunctionRegistry.java        # existing — expose built-in name set to Builder
│   ├── CustomFunctionRegistry.java         # NEW
│   └── ExpressionFunction.java             # NEW
├── parser/
│   ├── DataExpressionParser.java           # MODIFIED — new ctor + validate()
│   └── ValidationResult.java               # NEW
├── evaluator/
│   └── ExpressionEvaluator.java            # MODIFIED — accepts CustomFunctionRegistry
└── visitor/
    └── EvaluatingVisitor.java              # MODIFIED — custom function lookup + wrap

data-expression-parser-core/src/test/java/ru/zahaand/dataexpr/
├── CustomFunctionRegistryTest.java         # NEW
├── ValidationResultTest.java               # NEW
└── DataExpressionParserTest.java           # MODIFIED — Errors +1, new Validation group

data-expression-parser-spring-boot-starter/src/main/java/ru/zahaand/dataexpr/autoconfigure/
└── DataExpressionParserAutoConfiguration.java   # MODIFIED — customFunctionRegistry() bean
```

**Structure Decision**: No new modules or packages. All additions slot into existing
packages (`function`, `parser`, `evaluator`, `visitor`, `autoconfigure`). v1.0.0 files
are modified only where additive changes are unavoidable (two constructors, one new method,
one new bean).

---

## Sprint: 002-custom-functions-and-validation

Single sprint, 5 phases executed in strict order.
Each phase MUST be committed separately before the next phase begins.

### Clarifications Applied

The following clarifications from the spec session (2026-04-14) are integrated into this plan:

1. **Runtime exception wrapping** (FR-108a): any `RuntimeException` from a custom function is caught in `EvaluatingVisitor` and rethrown as `ExpressionEvaluationException("Error in custom function '<name>': <msg>", cause)`.
2. **Grammar-ID enforcement** (FR-105a): `Builder.register()` rejects names not matching `^[a-zA-Z_][a-zA-Z_0-9]*$`.
3. **No arity capture** (FR-108b): custom functions self-validate `args.length`; the library does not declare arity.
4. **Duplicate registration** (FR-105b): `Builder.register()` rejects the same name (case-insensitive) registered twice on the same builder.
5. **SLF4J logging** (FR-117): `WARN` before wrapping a custom-function exception; `DEBUG` when `validate()` returns invalid. No binding shipped.

---

### Phase 1 — Version Bump

**Goal**: all three `pom.xml` files updated to v1.1.0 and SLF4J API declared. `mvn compile` passes.

- [x] Update parent `pom.xml`: `<version>1.0.0</version>` → `<version>1.1.0</version>`.
- [x] Add `org.slf4j:slf4j-api` to parent `dependencyManagement` (version aligned with Spring Boot 3.5.x BOM or pinned explicitly if not present).
- [x] Update `data-expression-parser-core/pom.xml`:
  - Parent version reference → `1.1.0`.
  - Add `org.slf4j:slf4j-api` as a `compile` dependency (version inherited from parent `dependencyManagement`).
- [x] Update `data-expression-parser-spring-boot-starter/pom.xml`:
  - Parent version reference → `1.1.0`.
  - `data-expression-parser-core` dependency version → `1.1.0`.
- [ ] **Verify**: `mvn compile` passes in both modules with no errors.

**Commit**: `chore: bump version to 1.1.0`

---

### Phase 2 — New Types

**Goal**: `ExpressionFunction`, `CustomFunctionRegistry`, `ValidationResult` compile cleanly.
No behavioral changes to existing classes yet.

- [x] Create `ru.zahaand.dataexpr.function.ExpressionFunction`:
  - `@FunctionalInterface`
  - Single method: `double apply(double[] args, EvaluationContext context)`.
- [x] Expose built-in function names to the new registry (minimally-invasive way):
  - Add a package-visible constant on `BuiltinFunctionRegistry`:
    `static final Set<String> BUILTIN_NAMES = Set.of("abs","round","floor","ceil","min","max","pow")`.
  - This is the only change to `BuiltinFunctionRegistry` permitted in this phase; no behavior change.
- [x] Create `ru.zahaand.dataexpr.function.CustomFunctionRegistry`:
  - `public final class`; package `ru.zahaand.dataexpr.function`.
  - Private constructor taking `Map<String, ExpressionFunction>` (defensively copied as an unmodifiable map).
  - `public static CustomFunctionRegistry empty()` — returns an instance with an empty map.
  - `public static Builder builder()` — returns `new Builder()`.
  - `public ExpressionFunction find(String name)` — returns `map.get(name.toLowerCase(Locale.ROOT))`; returns `null` when absent (never throws).
  - `private static final Logger log = LoggerFactory.getLogger(CustomFunctionRegistry.class)`.
  - Inner `public static final class Builder`:
    - Backing storage: `Map<String, ExpressionFunction> entries = new LinkedHashMap<>()` (keys already lower-cased).
    - `public Builder register(String name, ExpressionFunction function)` applies checks in this order, logging at `ERROR` before each throw:
      1. Null / blank name → `IllegalArgumentException("Function name must not be null or blank")`.
      2. Name does not match `^[a-zA-Z_][a-zA-Z_0-9]*$` → `IllegalArgumentException("Function name '<name>' is not a valid identifier. Must match [a-zA-Z_][a-zA-Z_0-9]*")` (FR-105a).
      3. `BuiltinFunctionRegistry.BUILTIN_NAMES.contains(name.toLowerCase(Locale.ROOT))` → `IllegalArgumentException("Function name '<name>' conflicts with built-in function")` (FR-105).
      4. `entries.containsKey(name.toLowerCase(Locale.ROOT))` → `IllegalArgumentException("Custom function '<name>' is already registered")` (FR-105b).
      5. Null `function` → `IllegalArgumentException("Function must not be null")`.
      6. Store as `entries.put(name.toLowerCase(Locale.ROOT), function)`.
      7. `return this`.
    - `public CustomFunctionRegistry build()` — constructs the registry with an unmodifiable snapshot of `entries`.
- [x] Create `ru.zahaand.dataexpr.parser.ValidationResult`:
  - `public final class`; single constructor is private.
  - Fields: `boolean valid`, `String errorMessage` (nullable).
  - `public static ValidationResult valid()` — returns a cached singleton with `valid=true`, `errorMessage=null`.
  - `public static ValidationResult invalid(String errorMessage)` — throws `IllegalArgumentException` if `errorMessage` is null/blank (invariant: invalid results always carry a non-blank message); otherwise returns a new instance with `valid=false`.
  - `public boolean isValid()`.
  - `public Optional<String> errorMessage()` — returns `Optional.ofNullable(errorMessage)`; guaranteed empty when `isValid()` is `true`.
- [ ] **Verify**: `mvn compile` passes in both modules.

**Commit**: `feat(core): add ExpressionFunction, CustomFunctionRegistry, ValidationResult`

---

### Phase 3 — Core Logic

**Goal**: `EvaluatingVisitor` consults the custom registry first; `DataExpressionParser`
gains a registry-aware constructor and `validate(...)`.

- [x] Update `EvaluatingVisitor`:
  - Add `private final CustomFunctionRegistry customFunctionRegistry` field.
  - Constructor accepts `CustomFunctionRegistry` (non-null).
  - In the function-call branch (replace the existing delegation to `BuiltinFunctionRegistry`):
    1. `String lower = name.toLowerCase(Locale.ROOT)`.
    2. Evaluate arguments into `double[] args` (existing logic — arguments are already numeric-only; any non-numeric operand reuses the current type-mismatch error path).
    3. `ExpressionFunction custom = customFunctionRegistry.find(lower)`.
    4. If `custom != null`:
       - `try { return new DoubleResult(custom.apply(args, context)); }`
       - `catch (RuntimeException ex) { log.warn("Custom function '{}' threw {}", name, ex.toString()); throw new ExpressionEvaluationException("Error in custom function '" + name + "': " + ex.getMessage(), ex); }` (FR-108a, FR-117).
    5. Else delegate to `BuiltinFunctionRegistry` (unchanged behavior, including arity check and `Unknown function` message for non-existent names).
  - Add `private static final Logger log = LoggerFactory.getLogger(EvaluatingVisitor.class)` if not already present.
- [x] Update `ExpressionEvaluator`:
  - Add `private final CustomFunctionRegistry customFunctionRegistry` field.
  - New constructor: `public ExpressionEvaluator(CustomFunctionRegistry customFunctionRegistry)`.
  - Keep the existing no-arg constructor; it must delegate to `this(CustomFunctionRegistry.empty())` for backward compatibility (FR-116).
  - Pass `customFunctionRegistry` into every `new EvaluatingVisitor(...)` it constructs.
- [x] Update `DataExpressionParser`:
  - Add `private final CustomFunctionRegistry customFunctionRegistry` field.
  - New constructor: `public DataExpressionParser(ExpressionEvaluator evaluator, CustomFunctionRegistry customFunctionRegistry)` (stores both).
  - Keep the v1.0.0 constructor `public DataExpressionParser(ExpressionEvaluator evaluator)`; it must delegate to `this(evaluator, CustomFunctionRegistry.empty())` (FR-116).
  - Add `public ValidationResult validate(String expression)`:
    - If `StringUtils.isBlank(expression)` → throw `ExpressionParseException("Expression must not be null or blank")` (FR-110).
    - Build a `CharStream`, `DataExpressionLexer`, and `DataExpressionParser` (ANTLR-generated) over the input.
    - Install a custom `BaseErrorListener` on BOTH the lexer and the parser that captures the first `syntaxError(...)` callback into a `String[1]` holder formatted as `"Parse error at line <line>:<charPositionInLine>: <msg>"` (FR-111; identical format to `AstBuildingVisitor`).
    - Remove the default ANTLR console listener via `removeErrorListeners()` before adding the capturing listener.
    - Invoke `parser.prog()` purely to drive error detection (the resulting parse tree is discarded).
    - If the holder is still `null` → `return ValidationResult.valid()`.
    - Otherwise → `log.debug("Expression validation failed: {}", holder[0])` and `return ValidationResult.invalid(holder[0])` (FR-117).
    - MUST NOT invoke `AstBuildingVisitor` or `EvaluatingVisitor` (FR-112).
  - Add `private static final Logger log = LoggerFactory.getLogger(DataExpressionParser.class)` if not already present.
- [ ] **Verify**: `mvn compile` passes in both modules.

**Commit**: `feat(core): add custom function evaluation and validate() method`

---

### Phase 4 — Spring Starter

**Goal**: autoconfiguration exposes `CustomFunctionRegistry` as a bean and wires it into
`DataExpressionParser`.

- [x] Update `DataExpressionParserAutoConfiguration`:
  - Add bean method:
    ```java
    @Bean
    @ConditionalOnMissingBean
    public CustomFunctionRegistry customFunctionRegistry() {
        return CustomFunctionRegistry.empty();
    }
    ```
  - Replace the existing `dataExpressionParser(ExpressionEvaluator)` bean method signature with one that also takes `CustomFunctionRegistry`:
    ```java
    @Bean
    @ConditionalOnMissingBean
    public DataExpressionParser dataExpressionParser(ExpressionEvaluator evaluator,
                                                     CustomFunctionRegistry customFunctionRegistry) {
        return new DataExpressionParser(evaluator, customFunctionRegistry);
    }
    ```
  - `expressionEvaluator()` bean method: update body to `return new ExpressionEvaluator(customFunctionRegistry);` — pass the registry bean through so non-starter evaluation paths also honor custom functions. Add `CustomFunctionRegistry customFunctionRegistry` parameter; Spring resolves the auto-configured bean unless overridden.
- [ ] `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` unchanged.
- [ ] **Verify**: `mvn package` passes in both modules; both JARs produced.

**Commit**: `feat(starter): wire CustomFunctionRegistry into autoconfiguration`

---

### Phase 5 — Tests

**Goal**: `mvn test` green — existing v1.0.0 suite plus all new tests.

- [x] Create `CustomFunctionRegistryTest`:
  - `@Nested Registration`:
    - `shouldRegisterCustomFunctionSuccessfully`
    - `shouldFindRegisteredFunctionCaseInsensitively` — registered as `"TAX"`, found as `"tax"`, `"TAX"`, `"Tax"`
    - `shouldReturnNullWhenFunctionNotFound`
    - `shouldThrowWhenRegisteringNullName`
    - `shouldThrowWhenRegisteringBlankName`
    - `shouldThrowWhenNameDoesNotMatchGrammarIdPattern` — `@ParameterizedTest` over `"2pay"`, `"my-func"`, `"tax rate"`, `"fn!"`
    - `shouldThrowWhenNameConflictsWithBuiltin` — `@ParameterizedTest` over `abs`, `ABS`, `Abs`, `round`, `floor`, `ceil`, `min`, `max`, `pow`
    - `shouldThrowWhenSameNameRegisteredTwice` — case-insensitive duplicate detection
    - `shouldCreateEmptyRegistry`
  - `@Nested Evaluation` (via `DataExpressionParser` wired with the test registry):
    - `shouldEvaluateCustomFunctionWithArgs` — `TAX([price])` where `TAX = args -> args[0] * 0.15`
    - `shouldEvaluateCustomFunctionWithContextAccess` — `DISCOUNT([price])` reading `customer_tier` from context
    - `shouldFallBackToBuiltinWhenCustomNotFound` — only custom `"TAX"` registered; `abs(-5)` still works
    - `shouldThrowEvaluationExceptionWhenCustomFunctionThrows` — lambda throws `RuntimeException`; assert wrapped as `ExpressionEvaluationException` with message prefix `"Error in custom function '<name>': "` and `getCause()` is the original exception
    - `shouldAllowCustomFunctionToValidateOwnArity` — lambda throws `IllegalArgumentException` on wrong arg count; verify wrapped per FR-108a
    - `shouldThrowWhenFunctionNotFoundInEitherRegistry`
- [x] Create `ValidationResultTest`:
  - `shouldReturnValidForCorrectExpression`
  - `shouldReturnInvalidForMalformedExpression`
  - `shouldContainErrorMessageWithLineAndColumn` — assert message matches regex `Parse error at line \d+:\d+: .+`
  - `shouldThrowParseExceptionForNullInput`
  - `shouldThrowParseExceptionForBlankInput`
  - `shouldReturnEmptyOptionalWhenValid`
  - `shouldReturnPresentOptionalWhenInvalid`
- [x] Add to existing `DataExpressionParserTest` — `Errors` group:
  - `shouldThrowWhenCustomFunctionRegisteredWithBuiltinName` — exercises the conflict check through the parser's wiring; confirms `IllegalArgumentException` (not `ExpressionEvaluationException`) at build time.
- [x] Add new `@Nested Validation` group to `DataExpressionParserTest`:
  - `shouldReturnValidResultForValidExpression` — e.g. `[age] > 18 AND [status] == 'active'`
  - `shouldReturnInvalidResultWithMessageForMalformedExpression` — e.g. `[age] >`
  - `shouldValidateWithoutEvaluating` — validate a complex expression referencing undefined fields; assert `valid()` and do NOT invoke `evaluate*` (FR-112).
- [ ] **Verify**: `mvn test` passes — all existing tests green plus all new tests green.

**Commit**: `test(core): add tests for custom functions and validation`

---

## Commit Strategy

One commit per phase, in order:

| Phase | Commit Message |
|-------|---------------|
| 1 | `chore: bump version to 1.1.0` |
| 2 | `feat(core): add ExpressionFunction, CustomFunctionRegistry, ValidationResult` |
| 3 | `feat(core): add custom function evaluation and validate() method` |
| 4 | `feat(starter): wire CustomFunctionRegistry into autoconfiguration` |
| 5 | `test(core): add tests for custom functions and validation` |

---

## Complexity Tracking

| Risk | Mitigation |
|------|------------|
| `validate()` must NOT throw on syntax errors — only capture them | Custom ANTLR `BaseErrorListener` on lexer + parser stores the first error in a `String[1]` holder; `validate()` reads the holder and wraps in `ValidationResult.invalid(...)` — never propagates the ANTLR exception path used by `parse()`. |
| `EvaluatingVisitor` gains a new constructor parameter | `ExpressionEvaluator` supplies it; v1.0.0 `ExpressionEvaluator()` no-arg constructor delegates with `CustomFunctionRegistry.empty()` so all existing test and consumer paths remain functional (FR-116). |
| Custom function `RuntimeException` must be wrapped, not propagated | `try/catch (RuntimeException)` in `EvaluatingVisitor`'s custom-function branch: log at `WARN`, then throw `ExpressionEvaluationException` with original as `cause` (FR-108a, FR-117). |
| `Builder.register()` conflict check needs built-in names | Add package-visible `BuiltinFunctionRegistry.BUILTIN_NAMES` constant in Phase 2 — single source of truth, no duplication. |
| v1.0.0 `DataExpressionParser(ExpressionEvaluator)` constructor must remain functional | New constructor is additive; v1.0.0 constructor delegates with `CustomFunctionRegistry.empty()` (FR-116). |
| Starter update changes `expressionEvaluator()` bean signature | New parameter `CustomFunctionRegistry` is auto-resolved from the bean added in the same configuration; `@ConditionalOnMissingBean` still honors consumer overrides for all three beans. |
| SLF4J added to core | API only — no binding declared in core or starter. Consumers continue to supply their own logging backend; parity with Spring Boot's default Logback binding in the starter consumer context. |
| `ValidationResult.invalid(null/blank)` would violate the "present Optional" invariant | Constructor rejects null/blank messages with `IllegalArgumentException` — the invariant in FR-113 is enforced structurally, not by discipline. |

No constitution deviations introduced beyond those already documented for v1.0.0.
