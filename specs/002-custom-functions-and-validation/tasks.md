# Tasks: Custom Functions & Validation (v1.1.0)

**Input**: Design documents from `/specs/002-custom-functions-and-validation/`
**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [checklists/requirements.md](./checklists/requirements.md)

**Tests**: Included — the spec explicitly enumerates required tests (§Testing Requirements) and plan Phase 5 is a dedicated test phase.

**Organization**: Tasks are grouped by user story (US1 = Custom Functions, US2 = Validation, US3 = Spring Autoconfig). US1 and US2 are both P1 and can be worked on in parallel after Phase 2; US3 depends on both.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: parallelizable — different files, no dependency on other incomplete tasks
- **[Story]**: `US1` | `US2` | `US3` — only on story-phase tasks
- Paths are absolute from repo root

## Path Conventions

Maven multi-module library:
- Core sources: `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/**`
- Core tests:   `data-expression-parser-core/src/test/java/ru/zahaand/dataexpr/**`
- Starter:      `data-expression-parser-spring-boot-starter/src/main/java/ru/zahaand/dataexpr/autoconfigure/**`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Bump all POMs to 1.1.0 and introduce SLF4J API dependency. `mvn compile` must pass.

- [X] T001 Bump parent version from 1.0.0 to 1.1.0 in `pom.xml` (root `<version>` element)
- [X] T002 [P] Update parent-reference version to 1.1.0 in `data-expression-parser-core/pom.xml`
- [X] T003 [P] Update parent-reference version and the `data-expression-parser-core` dependency version to 1.1.0 in `data-expression-parser-spring-boot-starter/pom.xml`
- [X] T004 Verify `org.slf4j:slf4j-api` is present in parent `pom.xml` `dependencyManagement` (already added in v1.0.0 — confirm version aligns with Spring Boot 3.5.x BOM)
- [X] T005 Verify `org.slf4j:slf4j-api` is declared as compile dependency in `data-expression-parser-core/pom.xml` (already present — no change required)
- [X] T006 Run `mvn -q compile` at repo root and confirm both modules compile with no errors (commit gate for Phase 1: `chore: bump version to 1.1.0`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Types and helpers that BOTH US1 and US2 need.

⚠️ **CRITICAL**: No story work in Phase 3 or Phase 4 can begin until Phase 2 completes.

- [X] T007 [P] Create `ExpressionFunction` functional interface at `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/function/ExpressionFunction.java` — `@FunctionalInterface` with single method `double apply(double[] args, EvaluationContext context)` (spec §ExpressionFunction, FR-102)
- [X] T008 [P] Expose built-in function names for the conflict check by adding a package-visible constant `static final Set<String> BUILTIN_NAMES = Set.of("abs","round","floor","ceil","min","max","pow")` in `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/function/BuiltinFunctionRegistry.java` (plan §Phase 2; no other behavior change permitted in this task)
- [X] T009 Run `mvn -q compile` to confirm foundational types compile cleanly

**Checkpoint**: US1 (Phase 3) and US2 (Phase 4) may now begin in parallel on distinct files. The shared file `DataExpressionParser.java` is edited in both stories — sequence the two edits to avoid conflicts (see Dependencies section).

---

## Phase 3: User Story 1 — Register Custom Functions (Priority: P1) 🎯 MVP

**Goal**: Consumers can build a `CustomFunctionRegistry`, register named functions receiving `double[]` and `EvaluationContext`, and have them resolved by `DataExpressionParser` at evaluation time with correct precedence over built-ins (by name collision prevention).

**Independent Test**: With a `CustomFunctionRegistry` containing `TAX = (args, ctx) -> args[0] * 0.15` and `DISCOUNT = (args, ctx) -> args[0] * ("premium".equals(ctx.get("customer_tier")) ? 0.8 : 0.95)`, `evaluateDouble("TAX([price])", {price:100.0})` returns `15.0` and `evaluateDouble("DISCOUNT([price])", {price:100.0, customer_tier:"premium"})` returns `80.0`. Registration of any built-in name (case-insensitive) throws `IllegalArgumentException` at build time.

### Implementation

- [X] T010 [US1] Create `CustomFunctionRegistry` at `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/function/CustomFunctionRegistry.java` — `public final class` with:
  - private constructor accepting an unmodifiable `Map<String, ExpressionFunction>`
  - `public static CustomFunctionRegistry empty()` returning an instance over an empty map
  - `public static Builder builder()`
  - `public ExpressionFunction find(String name)` — implementation `return map.get(name.toLowerCase(Locale.ROOT));` (Case-Insensitivity Convention, FR-103, FR-106)
  - `private static final Logger log = LoggerFactory.getLogger(CustomFunctionRegistry.class)` (FR-117a)
- [X] T011 [US1] Add inner `public static final class Builder` in `CustomFunctionRegistry.java` with `register(String name, ExpressionFunction function)` applying checks in this order, each preceded by `log.error("Custom function registration failed for name '{}': {}", name, <msg>)` (FR-117a):
  1. null/blank name → `IllegalArgumentException("Function name must not be null or blank")` (FR-104)
  2. regex `^[a-zA-Z_][a-zA-Z_0-9]*$` mismatch → `IllegalArgumentException("Function name '<name>' is not a valid identifier. Must match [a-zA-Z_][a-zA-Z_0-9]*")` (FR-105a)
  3. `BuiltinFunctionRegistry.BUILTIN_NAMES.contains(name.toLowerCase(Locale.ROOT))` → `IllegalArgumentException("Function name '<name>' conflicts with built-in function")` (FR-105)
  4. duplicate on this builder (case-insensitive) → `IllegalArgumentException("Custom function '<name>' is already registered")` (FR-105b)
  5. null function → `IllegalArgumentException("Function must not be null")`
  6. store `entries.put(name.toLowerCase(Locale.ROOT), function)`, `return this`
  Also add `public CustomFunctionRegistry build()` returning the immutable registry.
- [X] T012 [US1] Update `EvaluatingVisitor` at `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/visitor/EvaluatingVisitor.java`:
  - add `private final CustomFunctionRegistry customFunctionRegistry` field + constructor injection
  - in the function-call branch: `String lower = name.toLowerCase(Locale.ROOT)`, then `ExpressionFunction custom = customFunctionRegistry.find(lower)`; if non-null, evaluate args to `double[]`, wrap the call in `try { return new DoubleResult(custom.apply(args, context)); } catch (RuntimeException ex) { log.warn("Custom function '{}' threw {}", name, ex.toString()); throw new ExpressionEvaluationException("Error in custom function '" + name + "': " + ex.getMessage(), ex); }` (FR-108, FR-108a, FR-117)
  - otherwise delegate to existing `BuiltinFunctionRegistry` path (unchanged)
  - add `private static final Logger log = LoggerFactory.getLogger(EvaluatingVisitor.class)` if absent
- [X] T013 [US1] Update `ExpressionEvaluator` at `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/evaluator/ExpressionEvaluator.java`:
  - add `private final CustomFunctionRegistry customFunctionRegistry` field
  - new constructor `public ExpressionEvaluator(CustomFunctionRegistry customFunctionRegistry)`
  - keep the existing no-arg constructor, making it delegate: `this(CustomFunctionRegistry.empty())` (FR-116)
  - pass the registry into every `new EvaluatingVisitor(...)` it constructs
- [X] T014 [US1] Update `DataExpressionParser` at `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/parser/DataExpressionParser.java`:
  - add `private final CustomFunctionRegistry customFunctionRegistry` field
  - add new constructor `public DataExpressionParser(ExpressionEvaluator evaluator, CustomFunctionRegistry customFunctionRegistry)` storing both
  - modify the existing `DataExpressionParser(ExpressionEvaluator evaluator)` to delegate `this(evaluator, CustomFunctionRegistry.empty())` (FR-116)
  - (do NOT yet add `validate()` — that belongs to US2)
- [X] T015 [US1] Run `mvn -q compile` to confirm all changes compile in both modules

### Tests (from spec §CustomFunctionRegistryTest and §DataExpressionParserTest additions)

- [X] T016 [P] [US1] Create `CustomFunctionRegistryTest` at `data-expression-parser-core/src/test/java/ru/zahaand/dataexpr/CustomFunctionRegistryTest.java` with `@Nested Registration` group covering:
  - `shouldRegisterCustomFunctionSuccessfully`
  - `shouldFindRegisteredFunctionCaseInsensitively` (registered `"TAX"`, looked up as `"tax"`, `"TAX"`, `"Tax"`)
  - `shouldReturnNullWhenFunctionNotFound`
  - `shouldThrowWhenRegisteringNullName`
  - `shouldThrowWhenRegisteringBlankName`
  - `shouldThrowWhenNameDoesNotMatchGrammarIdPattern` — `@ParameterizedTest` over `"2pay"`, `"my-func"`, `"tax rate"`, `"fn!"` (FR-105a)
  - `shouldThrowWhenNameConflictsWithBuiltin` — `@ParameterizedTest` over `abs`, `ABS`, `Abs`, `round`, `floor`, `ceil`, `min`, `max`, `pow` (FR-105)
  - `shouldThrowWhenSameNameRegisteredTwice` (FR-105b)
  - `shouldCreateEmptyRegistry`
- [X] T017 [US1] Add `@Nested Evaluation` group to the same `CustomFunctionRegistryTest` (T016), exercising via `new DataExpressionParser(new ExpressionEvaluator(registry), registry)`:
  - `shouldEvaluateCustomFunctionWithArgs` — `TAX([price])` → 15.0
  - `shouldEvaluateCustomFunctionWithContextAccess` — `DISCOUNT([price])` reads `customer_tier`
  - `shouldFallBackToBuiltinWhenCustomNotFound` — only custom `"TAX"` registered; `abs(-5)` still works
  - `shouldThrowEvaluationExceptionWhenCustomFunctionThrows` — assert wrapped `ExpressionEvaluationException` with message prefix `"Error in custom function '<name>': "` AND `getCause()` is the original (FR-108a)
  - `shouldAllowCustomFunctionToValidateOwnArity` — lambda throws `IllegalArgumentException` on wrong `args.length`; assert wrapping per FR-108a/FR-108b
  - `shouldThrowWhenFunctionNotFoundInEitherRegistry` — asserts `"Unknown function: '<name>'"`
- [X] T018 [P] [US1] Add `shouldThrowWhenCustomFunctionRegisteredWithBuiltinName` to the existing `Errors` `@Nested` group inside `data-expression-parser-core/src/test/java/ru/zahaand/dataexpr/DataExpressionParserTest.java` (SC-107) — verify the registration throws at build time, not during evaluation
- [X] T019 [US1] Run `mvn -q -pl data-expression-parser-core test -Dtest='CustomFunctionRegistryTest,DataExpressionParserTest'` and confirm all listed tests pass

**Checkpoint**: US1 is independently deliverable. A consumer supplying a `CustomFunctionRegistry` directly (without the starter) gets working custom functions.

---

## Phase 4: User Story 2 — Validate Expression Syntax (Priority: P1)

**Goal**: Consumers can call `DataExpressionParser.validate(String)` and receive a `ValidationResult` indicating syntactic validity, with a line/column-annotated message when invalid. Validation does not evaluate; missing functions or fields do not make it invalid.

**Independent Test**: `validate("[age] > 18 AND [status] == 'active'")` → `isValid() == true`, `errorMessage().isEmpty()`. `validate("[age] >")` → `isValid() == false`, `errorMessage()` present and matching regex `Parse error at line \d+:\d+: .+`. `validate(null)` and `validate("")` throw `ExpressionParseException`.

### Implementation

- [X] T020 [P] [US2] Create `ValidationResult` at `data-expression-parser-core/src/main/java/ru/zahaand/dataexpr/parser/ValidationResult.java` — `public final class` with private constructor, fields `boolean valid` and `String errorMessage` (nullable), plus:
  - `public static ValidationResult valid()` returning a cached singleton with `valid=true, errorMessage=null`
  - `public static ValidationResult invalid(String errorMessage)` — throws `IllegalArgumentException("Error message must not be null or blank")` when null/blank (FR-113a); otherwise returns a new instance with `valid=false`
  - `public boolean isValid()`
  - `public Optional<String> errorMessage()` returning `Optional.ofNullable(errorMessage)` — guaranteed empty when valid (FR-113)
- [X] T021 [US2] Add `public ValidationResult validate(String expression)` to `DataExpressionParser` (same file as T014 — sequence AFTER T014):
  - if `StringUtils.isBlank(expression)` → throw `ExpressionParseException("Expression must not be null or blank")` (FR-110)
  - build `CharStream` → `DataExpressionLexer` → `DataExpressionParser` (ANTLR-generated); call `removeErrorListeners()` on both, then attach a `BaseErrorListener` subclass whose `syntaxError(...)` stores the first error into a `String[1]` holder formatted exactly as `"Parse error at line " + line + ":" + charPositionInLine + ": " + msg` (FR-111, matches v1.0.0 `AstBuildingVisitor` format)
  - invoke `parser.prog()` purely to drive error detection; discard the tree — MUST NOT invoke `AstBuildingVisitor` or `EvaluatingVisitor` (FR-112)
  - if holder is `null`: `return ValidationResult.valid()`
  - else: `log.debug("Expression validation failed: {}", holder[0])` and `return ValidationResult.invalid(holder[0])` (FR-117)
  - declare `private static final Logger log = LoggerFactory.getLogger(DataExpressionParser.class)` if absent
- [X] T022 [US2] Run `mvn -q compile` to confirm Phase 4 changes compile

### Tests (from spec §ValidationResultTest and §DataExpressionParserTest `Validation` group)

- [X] T023 [P] [US2] Create `ValidationResultTest` at `data-expression-parser-core/src/test/java/ru/zahaand/dataexpr/ValidationResultTest.java` covering:
  - `shouldReturnValidForCorrectExpression`
  - `shouldReturnInvalidForMalformedExpression`
  - `shouldContainErrorMessageWithLineAndColumn` — assert message matches regex `Parse error at line \d+:\d+: .+`
  - `shouldThrowParseExceptionForNullInput` (FR-110)
  - `shouldThrowParseExceptionForBlankInput` (FR-110)
  - `shouldReturnEmptyOptionalWhenValid` (FR-113)
  - `shouldReturnPresentOptionalWhenInvalid` (FR-113)
  - `shouldThrowWhenInvalidCalledWithNullOrBlankMessage` (FR-113a)
- [X] T024 [US2] Add new `@Nested Validation` group to `data-expression-parser-core/src/test/java/ru/zahaand/dataexpr/DataExpressionParserTest.java` (sequence AFTER T018 — same file) containing:
  - `shouldReturnValidResultForValidExpression`
  - `shouldReturnInvalidResultWithMessageForMalformedExpression`
  - `shouldValidateWithoutEvaluating` — validate a complex expression referencing undefined fields/functions; assert `valid()` and verify `evaluate*` was never called (FR-112)
- [X] T025 [US2] Run `mvn -q -pl data-expression-parser-core test -Dtest='ValidationResultTest,DataExpressionParserTest'` and confirm all listed tests pass

**Checkpoint**: US2 is independently deliverable. A consumer using only `DataExpressionParser` (without Spring) gets working `validate(...)`.

---

## Phase 5: User Story 3 — Spring Boot Auto-injection of Registry (Priority: P2)

**Goal**: Starter auto-configures an empty `CustomFunctionRegistry` bean and wires it into `ExpressionEvaluator` and `DataExpressionParser`. Consumers who define their own bean override via `@ConditionalOnMissingBean`.

**Independent Test**: A minimal Spring Boot context with only the starter on the classpath produces `CustomFunctionRegistry`, `ExpressionEvaluator`, and `DataExpressionParser` beans; `dataExpressionParser.evaluateDouble("abs(-5)", EvaluationContext.empty())` returns `5.0`. A second context that defines a consumer `CustomFunctionRegistry` bean with a `TAX` function makes `evaluateDouble("TAX([price])", {price:100.0})` return `15.0`, and the auto-configured empty registry is not created.

**Depends on**: US1 (`CustomFunctionRegistry` exists and `ExpressionEvaluator`/`DataExpressionParser` accept it). Does NOT depend on US2 for the starter changes themselves, but US2 should be on the same branch at ship time so the autoconfigured parser is feature-complete.

### Implementation

- [X] T026 [US3] Update `DataExpressionParserAutoConfiguration` at `data-expression-parser-spring-boot-starter/src/main/java/ru/zahaand/dataexpr/autoconfigure/DataExpressionParserAutoConfiguration.java`:
  - add new bean method:
    ```java
    @Bean
    @ConditionalOnMissingBean
    public CustomFunctionRegistry customFunctionRegistry() {
        return CustomFunctionRegistry.empty();
    }
    ```
  - modify existing `expressionEvaluator()` to accept and pass through the registry: `public ExpressionEvaluator expressionEvaluator(CustomFunctionRegistry customFunctionRegistry) { return new ExpressionEvaluator(customFunctionRegistry); }` (keep `@Bean @ConditionalOnMissingBean`)
  - modify existing `dataExpressionParser(ExpressionEvaluator)` signature to `dataExpressionParser(ExpressionEvaluator evaluator, CustomFunctionRegistry customFunctionRegistry)` returning `new DataExpressionParser(evaluator, customFunctionRegistry)` (keep `@Bean @ConditionalOnMissingBean`)
  - `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` remains unchanged
- [X] T027 [US3] Run `mvn -q package` at repo root and confirm both JARs are produced with no errors

**Checkpoint**: US3 is deliverable. Starter consumers get the registry bean auto-wired.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Full-suite regression, acceptance-criteria sweep, and commit hygiene.

- [X] T028 Run `mvn -q test` at repo root and confirm ALL existing v1.0.0 tests plus new v1.1.0 tests pass (SC-102)
- [X] T029 Manual sweep of SC-101–SC-108 against the built modules: spot-check `TAX([price])=15.0`, `DISCOUNT` with tier read, `validate` on valid and malformed inputs, `register("abs",...)` throws at build time, and starter boot with/without consumer registry bean
- [X] T030 Confirm the five per-phase commits land in order on branch `002-custom-functions-and-validation`:
  1. `chore: bump version to 1.1.0` (Phase 1 — after T006)
  2. `feat(core): add ExpressionFunction, CustomFunctionRegistry, ValidationResult` (after T007–T011, T020)
  3. `feat(core): add custom function evaluation and validate() method` (after T012–T015, T021–T022)
  4. `feat(starter): wire CustomFunctionRegistry into autoconfiguration` (after T026–T027)
  5. `test(core): add tests for custom functions and validation` (after T016–T019, T023–T025, T028)

---

## Dependencies

Story-level order:

```
Phase 1 (Setup)
    └─▶ Phase 2 (Foundational: ExpressionFunction, BUILTIN_NAMES)
            ├─▶ Phase 3 (US1: Custom Functions)
            │       └─▶ Phase 5 (US3: Spring Autoconfig)  [depends on US1 types]
            └─▶ Phase 4 (US2: Validation)
                    └─▶ Phase 6 (Polish)
```

Intra-file sequencing for `DataExpressionParser.java` (US1 and US2 both edit it):
- T014 (US1 ctor changes) MUST precede T021 (US2 validate method). If developed in parallel, merge T014 first.

Intra-file sequencing for `DataExpressionParserTest.java` (US1 and US2 both edit it):
- T018 (Errors group addition) can land independently of T024 (new Validation group). They edit different `@Nested` classes; conflicts are textual only. Sequence is not strict but linear order T018 → T024 keeps history clean.

## Parallel Execution Opportunities

After Phase 2 completes, the following task groups can run in parallel within a single developer session (different files, no shared state):

**Setup phase** — T002, T003 can run in parallel (distinct POM files), both after T001:
```
T002  update core/pom.xml parent version
T003  update starter/pom.xml parent + core dep version
```

**Foundational phase** — T007, T008 in parallel (distinct files):
```
T007  create ExpressionFunction.java
T008  add BUILTIN_NAMES constant to BuiltinFunctionRegistry.java
```

**US1 vs US2 implementation** — T020 (`ValidationResult`) can be built in parallel with T010–T011 (`CustomFunctionRegistry`) — different files.

**Test file creation** — T016 (`CustomFunctionRegistryTest.java`) and T023 (`ValidationResultTest.java`) in parallel, both new files.

---

## Implementation Strategy

**MVP = US1 (Phase 3)** alone delivers the headline v1.1.0 capability (custom functions end-to-end, no Spring required). Shipping just Phase 1 → Phase 2 → Phase 3 → Phase 6 produces a working library release.

**Incremental delivery**:
1. Phase 1 → Phase 2 → Phase 3 (US1) → Phase 6 — ship v1.1.0-rc1 with custom functions only.
2. Add Phase 4 (US2) → ship v1.1.0-rc2 with validation.
3. Add Phase 5 (US3) → ship v1.1.0 GA with Spring autoconfig.

If all three stories ship together (recommended — one minor-version bump), follow the default phase order 1 → 2 → 3 → 4 → 5 → 6 with the five commits listed in T030.

---

## Independent Test Criteria (per story)

- **US1**: Without Spring, `new DataExpressionParser(new ExpressionEvaluator(registry), registry).evaluateDouble("TAX([price])", EvaluationContext.of("price", 100.0))` returns `15.0`; `CustomFunctionRegistry.builder().register("abs", (a,c)->0).build()` throws `IllegalArgumentException` at the `register` call.
- **US2**: `parser.validate("[age] > 18 AND [status] == 'active'").isValid()` is `true`; `parser.validate("[age] >").errorMessage().orElseThrow()` matches `Parse error at line 1:\d+: .+`.
- **US3**: Starter-only Spring Boot context exposes all three beans; adding a consumer `CustomFunctionRegistry` bean with `TAX` wins over the auto-configured empty one (verify by evaluating `TAX([price])`).
