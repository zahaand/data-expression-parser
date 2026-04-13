# data-expression-parser Constitution

**Version**: 1.0.0 | **Ratified**: 2026-04-13

---

## Core Principles

### I. Architecture (NON-NEGOTIABLE)

The application architecture MUST be defined explicitly in the project's Technology Stack section
and adhered to strictly throughout the project lifetime.

- Architecture dependencies MUST point strictly inward — outer layers depend on inner layers,
  never the reverse.
- Business logic MUST reside in the appropriate inner layer only.
- Direct cross-layer calls that bypass intermediate layers are PROHIBITED.
- The chosen architecture MUST be documented in the Technology Stack section of this constitution
  before the first sprint begins.

Rationale: Consistent, explicitly defined architecture is the foundation of maintainable,
testable, and reviewable code.

### II. Single Responsibility (SRP)

Every class MUST have exactly one clearly defined responsibility.

- If a class's responsibility cannot be described in one sentence, it MUST be split.
- A class that performs a business operation MUST NOT also handle infrastructure concerns
  (e.g., sending notifications, persisting data) unless that is its sole purpose.

Rationale: SRP is the single most visible quality signal during code review.

### III. Database Migrations

This project contains no database layer. Principle III does not apply.

### IV. Secrets

This project is a library with no runtime secrets. Principle IV does not apply.

### V. Logging Standards (NON-NEGOTIABLE)

Log levels MUST be applied as follows:

| Level   | When to use                                              |
|---------|----------------------------------------------------------|
| `DEBUG` | Input parameters, intermediate processing steps          |
| `INFO`  | Successful completion of a business operation            |
| `WARN`  | Abnormal but handled situation (retry, invalid input)    |
| `ERROR` | Exception or operation failure                           |

- Every caught `Exception` MUST be logged at ERROR level with identifying context before
  being re-thrown or handled.
- Log messages MUST include relevant identifiers (e.g. `userId`, `entityId`, `requestId`).
- Logging sensitive data (passwords, tokens, personal data) is PROHIBITED.

Logger declaration:
```java
private static final Logger log = LoggerFactory.getLogger(ClassName.class);
```

Rationale: Structured, level-appropriate logging makes failures diagnosable in production.

### VI. Code Style (NON-NEGOTIABLE)

**Member declaration order:**
1. Static fields (constants, `Logger`)
2. Instance fields (injected dependencies, other fields)
3. Constructors
4. Public methods
5. Private methods

**Additional rules:**

- All instance fields MUST be declared `final`.
- Constructor injection MUST be used. `@Autowired` on fields is PROHIBITED.
- No more than one consecutive blank line is permitted anywhere in a class.
- Returning `null` from public methods is PROHIBITED — use `Optional` or throw an exception.
  Private methods MAY return `null` as an internal control-flow signal within a single class.
- Curly braces MUST always be used, including single-line `if` and `for` bodies.
- `var` IS PERMITTED when the type is unambiguously clear from the right-hand side
  (e.g. constructor call, factory method with obvious return type).
- `var` is PROHIBITED when the type requires inspecting another file to understand.
- Private method names MUST start with a verb: `extractId()`, `buildResponse()`.
- No orphaned code: unused classes, methods, or imports MUST be removed before commit.

Rationale: Consistent, explicit code style reduces cognitive load during review.

### VII. Testing Standards (NON-NEGOTIABLE)

- Unit tests MUST use `@ExtendWith(MockitoExtension.class)`.
- `@SpringBootTest` in unit tests is PROHIBITED.
- Tests MUST be structured with `@Nested` classes, one per method under test.
- Every test method MUST carry `@DisplayName` describing the scenario.
- Boundary and equivalence-class cases MUST use `@ParameterizedTest` + `@MethodSource`.
- Test method naming convention: `should{Result}When{Condition}`.

Rationale: A well-structured test suite is self-documenting and demonstrates testing discipline.

### VIII. Code and Documentation Language (NON-NEGOTIABLE)

- All source code, identifiers, comments, and Javadoc MUST be in English.
- `README.md` MUST be written in English. A Russian translation is recommended.
- Commit messages (subject line): English, following Conventional Commits format.
- Commit message body: optional, any language.

Conventional Commits types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`,
`perf`, `ci`, `build`.

Scope is optional and reflects the module or domain:
`feat(auth):`, `feat(habits):`, `fix(core):`.

Rationale: English-only source code is internationally readable for collaborators and reviewers.

### IX. Simplicity and Portfolio Readability

- YAGNI: features not listed in the current scope MUST NOT be implemented speculatively.
- Code MUST be written to be understood by a reviewer unfamiliar with the project.
- Identifier names (classes, methods, variables) MUST be self-descriptive.
- Comments, where present, MUST explain *why*, not *what*.

Rationale: Clarity and professionalism of code are primary success criteria alongside
functional correctness.

---

## Technology Stack

| Concern      | Choice                                               |
|--------------|------------------------------------------------------|
| Language     | Java 21                                              |
| Framework    | Spring Boot 3.5.x                                    |
| Build tool   | Maven Multi-Module                                   |
| Architecture | Maven Multi-Module library (core + starter)          |
| Tests        | JUnit 5 + AssertJ + Mockito                          |
| ANTLR4       | 4.13.2 (grammar + runtime)                           |

All dependency version changes MUST be tracked in version control with an explanatory
commit message.

---

## Development Standards

1. **Package structure** MUST mirror the chosen architecture layers.
2. **Naming** MUST follow standard Java conventions: PascalCase for classes,
   camelCase for members and variables.
3. **DTOs**: All Data Transfer Object classes MUST carry the `Dto` suffix
   (e.g. `TaskDto`, `CreateTaskRequestDto`).
4. **Utility classes**: Classes with only static methods and no injected Spring beans
   MUST use the `Utils` suffix (e.g. `TimeParserUtils`).
   Use a `private` no-arg constructor.
5. **No orphaned code**: unused classes, methods, or imports MUST be removed before commit.
6. **Commits**:
   - Follow Conventional Commits (see Principle VIII).
   - One commit per task or logical group of related files.
   - Never commit unrelated changes together.
   - Never accumulate all sprint work into a single commit.
   - Branch naming for Spec Kit sprints: `{NNN}-{feature-slug}`
     (e.g. `001-foundation`, `002-auth`).
   - Branch naming for technical/fix branches: `{type}/{short-description}`
     (e.g. `fix/null-pointer`, `chore/cleanup-imports`).
7. **Apache Commons**: `StringUtils` MUST be used for string null/blank checks.
   `CollectionUtils` MUST be used for collection null/empty checks.
   `commons-lang3` MUST be declared as an explicit dependency in `pom.xml`.
8. **Implementation execution** (Spec Kit):
   - Foundation phases (migrations, domain, infra config) SHOULD run separately —
     they are the build foundation for subsequent phases.
   - After each phase: verify build passes, mark all tasks `[x]`, commit.
   - Commits MUST be split by phase regardless of how implementation was run.
9. **ANTLR grammar file**: The grammar file MUST be located at
   `data-expression-parser-core/src/main/antlr4/ru/zahaand/dataexpr/DataExpression.g4`.
   Grammar rules MUST be written in the order: entry rule → logical (OR → AND → NOT)
   → comparison → additive → multiplicative → power → unary → primary.
   Grammar file MUST NOT contain business logic — only syntax rules.
10. **AST nodes**: All AST node classes MUST be records implementing the `Expression`
    sealed interface. New node types MUST be added to the `permits` clause of `Expression`.
    AST classes MUST reside in `ru.zahaand.dataexpr.ast` package.
11. **No Spring in core**: Any Spring annotation (`@Component`, `@Service`, `@Bean`,
    `@Autowired`, etc.) in `data-expression-parser-core` is PROHIBITED.
    The core module MUST compile and run without Spring on the classpath.

---

## Compliance

All code reviews MUST verify compliance with the Core Principles above.

Any violation that cannot be immediately corrected MUST be logged in the Complexity
Tracking section of the relevant `plan.md` with explicit justification.

This Constitution supersedes all other development practices for the project.
