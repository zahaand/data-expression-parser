# Feature Specification: Custom Functions & Validation (v1.1.0)

**Feature Branch**: `002-custom-functions-and-validation`
**Created**: 2026-04-14
**Status**: Draft
**Version**: 1.1.0 (extends v1.0.0 of `data-expression-parser`)
**Input**: User description: "Add custom function registration and expression validation on top of v1.0.0"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Register Custom Functions (Priority: P1)

A consumer application needs business-specific computations (e.g. tax, discount, rounding
rules) that are not built into the library. The consumer defines a `CustomFunctionRegistry`
Spring bean and registers named functions that receive `double[] args` and the current
`EvaluationContext`. Expressions referencing these functions (e.g. `TAX([price])`) then
evaluate correctly.

**Why this priority**: Custom functions unlock consumer-specific business logic without
requiring them to fork or extend the grammar. This is the main v1.1.0 value-add.

**Independent Test**: Build a `CustomFunctionRegistry` via its builder, inject it into
`DataExpressionParser`, and assert that expressions using custom function names evaluate
as expected while built-in functions continue to work.

**Acceptance Scenarios**:

1. **Given** a registry with `TAX = (args, ctx) -> args[0] * 0.15`, expression `TAX([price])`,
   context `{price: 100.0}`, **When** `evaluateDouble()` is called, **Then** `15.0` is returned.
2. **Given** a registry with `DISCOUNT` that reads `customer_tier` from the context,
   expression `DISCOUNT([price])`, context `{price: 100.0, customer_tier: "premium"}`,
   **When** `evaluateDouble()` is called, **Then** `80.0` is returned.
3. **Given** a registry with name "TAX", expression `tax([price])`,
   **When** `evaluateDouble()` is called, **Then** the custom function is resolved
   case-insensitively and executes.
4. **Given** a builder call `register("abs", ...)`, **When** `build()` is invoked
   or earlier, **Then** `IllegalArgumentException` is thrown at registration time.

---

### User Story 2 - Validate Expression Syntax (Priority: P1)

A consumer application accepts user-authored expressions (e.g. in an admin UI) and needs
to check syntactic correctness *before* storing or evaluating them. `validate()` returns
a `ValidationResult` exposing `isValid()` and an optional error message including the
line/column of the syntax error.

**Why this priority**: Validation lets consumers surface parse errors to users without
catching exceptions across layer boundaries, and without paying the cost of full evaluation.

**Independent Test**: Call `DataExpressionParser.validate(String)` with valid and
malformed expressions and assert the returned `ValidationResult` shape.

**Acceptance Scenarios**:

1. **Given** expression `[age] > 18 AND [status] == 'active'`, **When** `validate()`
   is called, **Then** `ValidationResult.valid()` is returned and `errorMessage()` is
   `Optional.empty()`.
2. **Given** expression `[age] >`, **When** `validate()` is called, **Then** the result
   is invalid and `errorMessage()` contains a non-empty message with ANTLR line/column
   position.
3. **Given** a valid but unevaluable expression (references undefined function or field),
   **When** `validate()` is called, **Then** the result is `valid()` — function and field
   resolution are runtime concerns, not syntactic.
4. **Given** `null` or blank input, **When** `validate()` is called, **Then**
   `ExpressionParseException` is thrown.

---

### User Story 3 - Spring Boot Auto-injection of Registry (Priority: P2)

A Spring Boot consumer adds the starter, defines an optional `CustomFunctionRegistry`
bean, and receives a `DataExpressionParser` that uses it. If the consumer does not define
a bean, an empty registry is auto-configured and the parser behaves exactly as v1.0.0.

**Why this priority**: Convenience for consumers using the starter; the core module works
without Spring.

**Independent Test**: Start a minimal Spring Boot context with the starter on the
classpath; assert `CustomFunctionRegistry` is injectable. Then override the bean in a
separate context and assert the override is honored (`@ConditionalOnMissingBean`).

**Acceptance Scenarios**:

1. **Given** the starter is on the classpath and no consumer `CustomFunctionRegistry`
   bean is defined, **When** the context starts, **Then** an empty `CustomFunctionRegistry`
   is created and `DataExpressionParser` uses it.
2. **Given** a consumer defines a `CustomFunctionRegistry` bean, **When** the context
   starts, **Then** the autoconfigured bean is not created and the consumer's bean is
   injected into `DataExpressionParser`.

---

### Edge Cases

- What happens when a custom function name exactly matches a built-in (any case)? →
  `Builder.register()` throws `IllegalArgumentException` at registration time (fail-fast,
  not at evaluation time).
- What happens when `find()` is called for a name that was not registered? → Returns
  `null`; `EvaluatingVisitor` then falls back to `BuiltinFunctionRegistry`.
- What happens when both registries lack a function name? → `ExpressionEvaluationException`
  with message `"Unknown function: '<name>'"` — unchanged from v1.0.0.
- What happens when `validate()` receives a syntactically valid expression that references
  a non-existent function or field? → Returns `valid()`. Function/field existence is a
  runtime concern, not a syntactic one.
- What happens when a custom function reads a missing field via `context.get(...)`? →
  `EvaluationContext` throws `ExpressionEvaluationException` (same contract as v1.0.0).
- What happens when the custom function registry name is registered with leading or
  trailing whitespace? → The name is validated as non-blank; storage is lowercased via
  `toLowerCase`. Consumers should pass trimmed names.

## Clarifications

### Session 2026-04-14

- Q: Should `validate()` detect unknown function or field names? → A: No. Validation is
  syntactic only; unknown function/field detection remains a runtime concern that surfaces
  via `ExpressionEvaluationException` during evaluation.
- Q: When a custom name conflicts with a built-in, should the check happen at registration
  or evaluation? → A: Registration. `Builder.register()` throws `IllegalArgumentException`
  so conflicts surface immediately at bean-construction time.
- Q: Should custom functions have access to `EvaluationContext`? → A: Yes. The
  `ExpressionFunction` signature receives both `double[] args` and `EvaluationContext`,
  enabling context-aware logic such as tier-based discounts.
- Q: How should a `RuntimeException` thrown from a custom function during evaluation be
  surfaced? → A: Wrap in `ExpressionEvaluationException("Error in custom function
  '<name>': <msg>", cause)`, preserving the original as `cause`. Keeps the library's
  exception contract uniform with v1.0.0.
- Q: Should `Builder.register()` enforce the grammar ID format on custom function names?
  → A: Yes. Reject names not matching `^[a-zA-Z_][a-zA-Z_0-9]*$` with
  `IllegalArgumentException` at registration — prevents silent dead registrations that
  would never be callable from an expression.
- Q: Should the registry capture/enforce arity for custom functions? → A: No. Custom
  functions self-validate `args.length` and throw; the library does not declare arity.
  Supports variadic signatures naturally; thrown errors are wrapped per FR-108a.
- Q: What happens on duplicate `register(name, ...)` calls in the same builder? → A:
  Throw `IllegalArgumentException("Custom function '<name>' is already registered")`
  (case-insensitive duplicate detection). Silent overwrites would mask bean-wiring bugs.
- Q: Should the library emit logs in v1.1.0? → A: Yes — via SLF4J. Log `WARN` when a
  custom function invocation fails (before wrapping in `ExpressionEvaluationException`)
  and `DEBUG` when `validate()` returns an invalid result. SLF4J is API-only; no
  transitive binding added.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-101**: System MUST allow consumers to register named custom functions via a
  `CustomFunctionRegistry` built through a `Builder`.
- **FR-102**: Custom functions MUST receive `double[] args` and the active
  `EvaluationContext` and return a `double`.
- **FR-103**: Custom function names MUST be stored and resolved case-insensitively
  (lower-cased at registration and lookup).
- **FR-104**: `Builder.register()` MUST throw `IllegalArgumentException` when the name is
  `null` or blank.
- **FR-105**: `Builder.register()` MUST throw `IllegalArgumentException` when the name
  conflicts with any built-in function name (`abs`, `round`, `floor`, `ceil`, `min`,
  `max`, `pow`) — case-insensitive.
- **FR-105a**: `Builder.register()` MUST throw `IllegalArgumentException` when the name
  does not match the grammar `ID` pattern `^[a-zA-Z_][a-zA-Z_0-9]*$`. Such names would
  be unreachable from any parseable expression; failing fast at registration prevents
  silent dead registrations.
- **FR-105b**: `Builder.register()` MUST throw
  `IllegalArgumentException("Custom function '<name>' is already registered")` when the
  same name (case-insensitive) is registered twice on the same builder. Silent
  overwrites would mask misconfigured bean wiring.
- **FR-106**: `CustomFunctionRegistry.find(String)` MUST return `null` when the name is
  not registered.
- **FR-107**: `CustomFunctionRegistry.empty()` MUST return a registry with no custom
  functions.
- **FR-108**: During evaluation, `EvaluatingVisitor` MUST look up functions in the
  following order: (1) `CustomFunctionRegistry`, (2) `BuiltinFunctionRegistry`, and
  throw `ExpressionEvaluationException("Unknown function: '<name>'")` when neither
  contains the name.
- **FR-108a**: When a custom `ExpressionFunction` throws any `RuntimeException` during
  `apply(...)`, `EvaluatingVisitor` MUST catch it and rethrow as
  `ExpressionEvaluationException("Error in custom function '<name>': <original message>",
  cause)` with the original exception preserved as `cause`.
- **FR-108b**: The library MUST NOT declare or enforce arity for custom functions. Each
  custom function is responsible for validating `args.length` and throwing on mismatch;
  such errors are wrapped per FR-108a. This preserves a minimal `ExpressionFunction`
  signature and permits variadic behavior.
- **FR-109**: `DataExpressionParser` MUST expose a `validate(String)` method that returns
  `ValidationResult.valid()` for syntactically correct expressions and
  `ValidationResult.invalid(errorMessage)` otherwise.
- **FR-110**: `validate()` MUST throw `ExpressionParseException` for `null` or blank
  input (consistent with `parse()`).
- **FR-111**: The error message in `ValidationResult.invalid(...)` MUST contain the
  ANTLR line and column of the first syntax error in the format used by v1.0.0:
  `"Parse error at line <L>:<C>: <antlr_message>"`.
- **FR-112**: `validate()` MUST NOT evaluate the expression; it MUST NOT check function
  names or field existence.
- **FR-113**: `ValidationResult.errorMessage()` MUST return `Optional.empty()` when
  `isValid()` is `true`, and a present `Optional<String>` otherwise.
- **FR-114**: The starter MUST auto-configure an empty `CustomFunctionRegistry` bean
  with `@ConditionalOnMissingBean`, and inject it into the autoconfigured
  `DataExpressionParser`.
- **FR-115**: `CustomFunctionRegistry` and `ValidationResult` MUST be thread-safe and
  suitable for use as Spring singletons (immutable after `build()` / static factories).
- **FR-116**: Existing v1.0.0 public API contracts MUST remain unchanged. Any new
  `DataExpressionParser` constructor is additive; existing constructors and methods MUST
  continue to work.
- **FR-117**: The core module MUST use SLF4J (`org.slf4j:slf4j-api`) for logging. When a
  custom function invocation throws, the library MUST log at `WARN` with function name
  and cause summary before wrapping into `ExpressionEvaluationException` (per FR-108a).
  When `validate()` returns `ValidationResult.invalid(...)`, the library MUST log the
  error at `DEBUG`. No SLF4J binding is declared — consumers supply their own binding
  (existing v1.0.0 consumer expectation).

### Key Entities

- **ExpressionFunction**: Functional interface — `double apply(double[] args,
  EvaluationContext context)`. Represents a consumer-provided computation.
- **CustomFunctionRegistry**: Immutable, name-keyed registry of `ExpressionFunction`
  instances. Built via `CustomFunctionRegistry.builder()`; name conflicts with built-ins
  are rejected at registration time.
- **ValidationResult**: Immutable record-like value carrying `isValid()` and an optional
  error message. Constructed via static factories `valid()` and `invalid(message)`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-101**: `mvn compile` passes with no errors in both modules.
- **SC-102**: `mvn test` passes all existing v1.0.0 tests plus the new tests listed
  in the Testing Requirements section below.
- **SC-103**: A consumer can define a `CustomFunctionRegistry` bean and evaluate
  `TAX([price])` with context `{price: 100.0}`, receiving `15.0`.
- **SC-104**: A consumer's context-aware custom function (`DISCOUNT`) reads
  `customer_tier` from `EvaluationContext` and returns the tier-correct result.
- **SC-105**: `validate("[age] > 18 AND [status] == 'active'")` returns
  `ValidationResult.valid()`.
- **SC-106**: `validate("[age] >")` returns an invalid result whose `errorMessage()` is
  present and contains `line` and column position.
- **SC-107**: Attempting to `register("abs", ...)` (in any case) throws
  `IllegalArgumentException` at registration time.
- **SC-108**: A Spring Boot consumer with the starter and no custom registry bean sees
  the same behavior as v1.0.0; adding a registry bean enables custom functions with no
  other configuration.

## Assumptions

- Consumers continue to use Spring Boot 3.5.x with Java 21+.
- Custom functions return a `double`; functions producing `boolean` or `String` are out of
  scope for v1.1.0 (the existing grammar models function calls only as numeric-valued
  primaries).
- Custom functions are expected to be pure and reasonably fast. The library does not
  enforce timeouts or sandboxing.
- The `validate()` method is intended for design-time checks (e.g. admin UIs);
  high-throughput evaluation paths should continue to use `parse()` / `evaluate()` so
  that the parse result can be cached.
- No changes are made to the ANTLR grammar, AST nodes, v1.0.0 exception hierarchy, or
  existing test classes beyond additions noted below.

---

## Technical Specification

### Maven Coordinates

Parent version bumps to `1.1.0`. Module coordinates:
- `ru.zahaand:data-expression-parser:1.1.0` (pom)
- `ru.zahaand:data-expression-parser-core:1.1.0` (jar)
- `ru.zahaand:data-expression-parser-spring-boot-starter:1.1.0` (jar)

New dependency:
- `org.slf4j:slf4j-api` (compile, in `data-expression-parser-core`) — logging API only.
  No binding is shipped; consumers provide their own. If v1.0.0 already inherits
  SLF4J transitively via other dependencies, the addition is effectively a no-op at the
  classpath level but is declared explicitly to make the usage contract visible.

---

### New Packages / Classes

#### `ru.zahaand.dataexpr.function.ExpressionFunction`

```java
package ru.zahaand.dataexpr.function;

@FunctionalInterface
public interface ExpressionFunction {
    double apply(double[] args, EvaluationContext context);
}
```

- Functional interface — supports lambda registration.
- Receives already-evaluated numeric arguments and the active context so that
  context-aware business logic (e.g. tier lookups) can execute.

#### `ru.zahaand.dataexpr.function.CustomFunctionRegistry`

```java
package ru.zahaand.dataexpr.function;

public final class CustomFunctionRegistry {

    public static CustomFunctionRegistry empty() { ... }

    public static Builder builder() { ... }

    // Returns null if name not registered; caller falls back to BuiltinFunctionRegistry.
    public ExpressionFunction find(String name) { ... }

    public static final class Builder {

        // Throws IllegalArgumentException if:
        //   - name is null or blank
        //   - name does not match grammar ID pattern ^[a-zA-Z_][a-zA-Z_0-9]*$
        //   - name conflicts (case-insensitive) with a built-in: abs, round, floor,
        //     ceil, min, max, pow
        //   - name was already registered on this builder (case-insensitive)
        public Builder register(String name, ExpressionFunction function) { ... }

        public CustomFunctionRegistry build() { ... }
    }
}
```

- Final class, immutable after `build()`.
- Internal storage lower-cases the name; `find()` lower-cases the lookup key.
- Conflict check reads the built-in name set from `BuiltinFunctionRegistry` (or a
  package-visible accessor). Check MUST occur at `register()` time, not at `build()` or
  evaluation time.

#### `ru.zahaand.dataexpr.parser.ValidationResult`

```java
package ru.zahaand.dataexpr.parser;

public final class ValidationResult {

    public static ValidationResult valid() { ... }
    public static ValidationResult invalid(String errorMessage) { ... }

    public boolean isValid() { ... }

    // Present only when isValid() == false.
    public Optional<String> errorMessage() { ... }
}
```

- Final, immutable.
- `valid()` and `invalid(...)` are the only construction paths.

---

### Modified Classes

#### `DataExpressionParser`

Add a new constructor and new method; existing members remain unchanged.

```java
public final class DataExpressionParser {

    // Existing v1.0.0 constructor remains.
    public DataExpressionParser(ExpressionEvaluator evaluator) { ... }

    // New — injects a custom function registry; used by the starter.
    public DataExpressionParser(ExpressionEvaluator evaluator,
                                CustomFunctionRegistry customFunctionRegistry) { ... }

    // New — syntactic validation only.
    // Throws ExpressionParseException if input is null or blank.
    public ValidationResult validate(String expression) { ... }
}
```

- `validate()` runs the grammar over the input using the same ANTLR pipeline as
  `parse()`, but captures syntax errors into a `ValidationResult.invalid(...)` instead
  of throwing `ExpressionParseException`.
- `validate()` MUST NOT run `AstBuildingVisitor` logic that depends on runtime metadata,
  and MUST NOT run `EvaluatingVisitor`.
- When both v1.0.0 constructor and new constructor are present, the v1.0.0 constructor
  MUST remain functional — it may internally delegate to the new constructor with
  `CustomFunctionRegistry.empty()`.

#### `EvaluatingVisitor`

Update function-call handling to check `CustomFunctionRegistry` first.

- Resolution order during `visitFunctionCall`:
  1. Lower-case the function name.
  2. `ExpressionFunction f = customFunctionRegistry.find(name)` — if non-null, evaluate
     arguments to `double[]`, call `f.apply(args, context)`, return `DoubleResult`.
  3. Otherwise delegate to `BuiltinFunctionRegistry` as in v1.0.0.
  4. If neither resolves, throw
     `ExpressionEvaluationException("Unknown function: '<name>'")`.
- Custom functions are passed the active `EvaluationContext` so they can read fields.
- The `EvaluatingVisitor` gains a `CustomFunctionRegistry` field supplied by
  `ExpressionEvaluator` / `DataExpressionParser` wiring; the constructor change remains
  internal (package-private class).
- Any `RuntimeException` thrown by a custom function MUST be caught and wrapped in
  `ExpressionEvaluationException("Error in custom function '<name>': <original message>",
  cause)`. The original exception is preserved as `cause` for diagnostics.

#### `DataExpressionParserAutoConfiguration`

```java
@Bean
@ConditionalOnMissingBean
public CustomFunctionRegistry customFunctionRegistry() {
    return CustomFunctionRegistry.empty();
}

@Bean
@ConditionalOnMissingBean
public DataExpressionParser dataExpressionParser(ExpressionEvaluator evaluator,
                                                 CustomFunctionRegistry customFunctionRegistry) {
    return new DataExpressionParser(evaluator, customFunctionRegistry);
}
```

- The `ExpressionEvaluator` bean definition from v1.0.0 remains unchanged.
- Both new / updated beans use `@ConditionalOnMissingBean` so consumers can override.

---

### Exception Contracts (Additions)

- `IllegalArgumentException` — thrown by `CustomFunctionRegistry.Builder.register()`
  when the name is null/blank, does not match the grammar ID pattern
  `^[a-zA-Z_][a-zA-Z_0-9]*$`, or conflicts with a built-in (case-insensitive).
- `ExpressionParseException` — unchanged. Also thrown by `validate()` for null/blank
  input.
- `ExpressionEvaluationException` — unchanged. Still thrown for unknown functions after
  both registries are checked.

---

### Testing Requirements

All new tests reside in `data-expression-parser-core/src/test/java/ru/zahaand/dataexpr/`.
Existing v1.0.0 test classes are not rewritten; new tests are added to `DataExpressionParserTest`
only in the locations explicitly listed below.

#### `CustomFunctionRegistryTest` — new class

`@Nested` group `Registration`:
- `shouldRegisterCustomFunctionSuccessfully`
- `shouldFindRegisteredFunctionCaseInsensitively` — registered as `"TAX"`, found as
  `"tax"`, `"TAX"`, `"Tax"`.
- `shouldReturnNullWhenFunctionNotFound`
- `shouldThrowWhenRegisteringNullName`
- `shouldThrowWhenRegisteringBlankName`
- `shouldThrowWhenNameConflictsWithBuiltin` — `@ParameterizedTest` over `abs`, `ABS`,
  `Abs`, `round`, `floor`, `ceil`, `min`, `max`, `pow`.
- `shouldThrowWhenNameDoesNotMatchGrammarIdPattern` — `@ParameterizedTest` over
  `"2pay"`, `"my-func"`, `"tax rate"`, `"fn!"`.
- `shouldThrowWhenSameNameRegisteredTwice` — case-insensitive duplicate detection
  (register `"TAX"` then `"tax"` → throws).
- `shouldCreateEmptyRegistry`

`@Nested` group `Evaluation` (via `DataExpressionParser`):
- `shouldEvaluateCustomFunctionWithArgs`
- `shouldEvaluateCustomFunctionWithContextAccess` — function reads a field from
  `EvaluationContext`.
- `shouldPreferCustomFunctionOverBuiltin` — registering a non-builtin name succeeds;
  built-in names are blocked at registration (covered by `Registration` group).
- `shouldThrowWhenCustomFunctionNotFound`
- `shouldWrapRuntimeExceptionFromCustomFunction` — function throws `RuntimeException`;
  evaluator rethrows `ExpressionEvaluationException` with the original as `cause` and
  message starting with `"Error in custom function '<name>': "`.
- `shouldAllowCustomFunctionToValidateOwnArity` — function throws
  `IllegalArgumentException` when `args.length` is wrong; verify it is wrapped per
  FR-108a.

#### `ValidationResultTest` — new class

- `shouldReturnValidForCorrectExpression`
- `shouldReturnInvalidForMalformedExpression`
- `shouldContainErrorMessageWithLineAndColumn`
- `shouldReturnInvalidForNullInput` — `validate(null)` throws `ExpressionParseException`.
- `shouldReturnInvalidForBlankInput` — `validate("")` throws `ExpressionParseException`.
- `shouldReturnEmptyOptionalWhenValid`
- `shouldReturnPresentOptionalWhenInvalid`

#### `DataExpressionParserTest` — additions only

Add to the existing `Errors` group:
- `shouldThrowWhenCustomFunctionRegisteredWithBuiltinName`

Add new `@Nested` group `Validation`:
- `shouldReturnValidResultForValidExpression`
- `shouldReturnInvalidResultWithMessageForMalformedExpression`
- `shouldValidateWithoutEvaluating` — validate a complex expression; assert valid;
  do NOT call `evaluate*`.

No existing v1.0.0 tests are modified.

---

### Acceptance Criteria (Consumer-Facing)

1. `mvn compile` passes with no errors.
2. `mvn test` passes all tests — the full v1.0.0 suite plus the new tests listed above.
3. A consumer can define a `CustomFunctionRegistry` bean and use custom functions in
   expressions: `TAX([price])` with `{price: 100.0}` returns `15.0`.
4. A context-aware custom function `DISCOUNT([price])` reads `customer_tier` from
   `EvaluationContext` and returns the correct discounted value
   (`premium` → `*0.8`, otherwise `*0.95`).
5. `validate("[age] > 18 AND [status] == 'active'")` returns `ValidationResult.valid()`.
6. `validate("[age] >")` returns an invalid result whose `errorMessage()` is present and
   contains line/column position.
7. Registering a custom function named `"abs"` (any case) throws `IllegalArgumentException`
   at build time, not at evaluation time.
