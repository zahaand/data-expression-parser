🇷🇺 Описание на русском ниже &nbsp;/&nbsp; 🇬🇧 Russian description below

---

# Data Expression Parser

A reusable Java library for parsing and evaluating business expressions over named data fields.
Distributed as a Spring Boot Starter — add one dependency and get `DataExpressionParser`
as a ready-to-use Spring bean.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-green?style=flat-square&logo=springboot)
![Maven](https://img.shields.io/badge/Build-Maven_Multi--Module-blue?style=flat-square&logo=apachemaven)
![Version](https://img.shields.io/badge/Version-1.2.0-brightgreen?style=flat-square)
![Tests](https://img.shields.io/badge/Tests-273_passing-success?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square)

273 tests | 3 sprints | Maven Multi-Module (core + starter)

## Methodology

Built using **Spec-Driven Development (SDD)** via [Spec Kit](https://github.com/speckit/speckit)
and [Claude Code](https://docs.anthropic.com/en/docs/claude-code).

Each feature went through a structured SDD cycle:

- **Specify** — natural language feature spec with user stories and acceptance criteria
- **Clarify** — structured Q&A to resolve ambiguities before planning
- **Plan** — architecture plan with constitution compliance check
- **Checklist** — pre-implementation quality review
- **Tasks** — dependency-ordered task list with phase grouping
- **Implement** — code generation following the task list
- **Analyze** — post-implementation review against spec and constitution

The library was developed over 3 sprints, each following this full cycle.

## Features

- **Field references** — `[field_name]` syntax resolves values from a runtime context
- **Arithmetic** — `+`, `-`, `*`, `/`, `%`, `^`, `**` with correct operator precedence
- **Comparisons** — `>`, `<`, `>=`, `<=`, `==`, `!=` for numbers and strings
- **Logical operators** — `AND`, `OR`, `NOT` (case-insensitive)
- **IN / NOT IN** — check membership against static lists or dynamic context collections
- **Built-in functions** — `abs`, `round`, `floor`, `ceil`, `min`, `max`, `pow`
- **Custom functions** — register named functions with access to `EvaluationContext`
- **Syntax validation** — validate expression syntax without evaluating
- **Spring Boot Starter** — zero-config autoconfiguration via `@ConditionalOnMissingBean`
- **Thread-safe** — stateless parser, safe for singleton Spring beans

## Supported Syntax

### Operators (by precedence, lowest to highest)

| Level | Operators | Example |
|-------|-----------|---------|
| Logical OR | `OR` | `[a] > 1 OR [b] > 1` |
| Logical AND | `AND` | `[a] > 1 AND [b] > 1` |
| Logical NOT | `NOT` | `NOT [is_blocked]` |
| Comparison | `>` `<` `>=` `<=` `==` `!=` | `[age] >= 18` |
| IN / NOT IN | `IN` `NOT IN` | `[status] IN ('active', 'trial')` |
| Additive | `+` `-` | `[price] + [tax]` |
| Multiplicative | `*` `/` `%` | `[price] * [qty]` |
| Power | `^` `**` | `[x] ^ 2` |
| Unary | `-` | `-[value]` |
| Primary | `[field]` `'string'` `42` `true` | `[name]` |

### Field References

Field names are enclosed in square brackets and support any characters except `]` and newline:
[age]
[first name]
[Сумма долга]
[amount (USD)]

### String Literals

Single quotes only:
[status] == 'active'
[role] IN ('admin', 'moderator')

### Boolean Literals

Case-insensitive: `true`, `TRUE`, `True`, `false`, `FALSE`, `False`

### IN with Dynamic Collection

The right-hand side of `IN` / `NOT IN` can be a field reference resolving to `List<Object>`:
[status] IN [allowed_statuses]
[code] NOT IN [blocked_codes]

## Quick Start

### 1. Add dependency

```xml
<dependency>
    <groupId>ru.zahaand</groupId>
    <artifactId>data-expression-parser-spring-boot-starter</artifactId>
    <version>1.2.0</version>
</dependency>
```

### 2. Inject and use

```java
@Autowired
DataExpressionParser parser;
```

## Usage

### Evaluate a boolean expression

```java
var ctx = EvaluationContext.of(Map.of(
    "age",    25.0,
    "status", "active"
));

boolean result = parser.evaluateBoolean(
    "[age] > 18 AND [status] == 'active'",
    ctx
); // → true
```

### Evaluate an arithmetic expression

```java
var ctx = EvaluationContext.of(Map.of(
    "price",    100.0,
    "qty",      3.0,
    "discount", 0.1
));

double total = parser.evaluateDouble(
    "[price] * [qty] * (1 - [discount])",
    ctx
); // → 270.0
```

### Parse once, evaluate many times

Parsing is expensive; evaluation is cheap. Parse the expression once and reuse the AST:

```java
// Parse once
Expression ast = parser.parse("[age] > 18 AND [status] == 'active'");

// Evaluate many times against different contexts — no redundant parsing
for (Map<String, Object> row : dataRows) {
    boolean passed = parser.evaluateBoolean(ast, EvaluationContext.of(row));
}

// All three result types are supported with pre-parsed AST
boolean boolResult  = parser.evaluateBoolean(ast, ctx);
double  dblResult   = parser.evaluateDouble(ast, ctx);
EvaluationResult r  = parser.evaluate(ast, ctx);
```

### IN with a static list

```java
boolean isAllowed = parser.evaluateBoolean(
    "[role] IN ('admin', 'moderator', 'editor')",
    EvaluationContext.of("role", "admin")
); // → true
```

### IN with a dynamic collection from context

```java
var ctx = EvaluationContext.of(Map.of(
    "status",   "active",
    "allowed",  List.of("active", "trial", "premium")
));

boolean result = parser.evaluateBoolean("[status] IN [allowed]", ctx); // → true
```

### Custom functions

Define a `CustomFunctionRegistry` bean. Custom functions receive evaluated `double[]` arguments
and the active `EvaluationContext`:

```java
@Bean
public CustomFunctionRegistry customFunctionRegistry() {
    return CustomFunctionRegistry.builder()
        .register("TAX", (args, ctx) ->
            args[0] * 0.15)
        .register("DISCOUNT", (args, ctx) -> {
            String tier = (String) ctx.get("customer_tier");
            return args[0] * ("premium".equals(tier) ? 0.8 : 0.95);
        })
        .build();
}
```

Then use in expressions:

```java
double tax = parser.evaluateDouble(
    "TAX([price])",
    EvaluationContext.of("price", 100.0)
); // → 15.0

double discounted = parser.evaluateDouble(
    "DISCOUNT([price])",
    EvaluationContext.of(Map.of("price", 100.0, "customer_tier", "premium"))
); // → 80.0
```

**Registration rules:**
- Custom function names must match `[a-zA-Z_][a-zA-Z_0-9]*`
- Names conflicting with built-ins (`abs`, `round`, `floor`, `ceil`, `min`, `max`, `pow`) are rejected at registration time
- Duplicate names (case-insensitive) are rejected at registration time

### Validate expression syntax

Validate without evaluating — useful for admin UIs where users author expressions:

```java
ValidationResult result = parser.validate("[age] > 18 AND [status] == 'active'");
result.isValid();       // → true
result.errorMessage();  // → Optional.empty()

ValidationResult bad = parser.validate("[age] >");
bad.isValid();          // → false
bad.errorMessage();     // → Optional.of("Parse error at line 1:7: ...")
```

## Built-in Functions

| Function | Args | Behaviour |
|----------|------|-----------|
| `abs(x)` | 1 | `Math.abs(x)` |
| `round(x)` | 1 | `(double) Math.round(x)` |
| `floor(x)` | 1 | `Math.floor(x)` |
| `ceil(x)` | 1 | `Math.ceil(x)` |
| `min(x, y)` | 2 | `Math.min(x, y)` |
| `max(x, y)` | 2 | `Math.max(x, y)` |
| `pow(x, y)` | 2 | `Math.pow(x, y)` |

Function names are resolved case-insensitively: `ABS([x])` and `abs([x])` are equivalent.

## Exception Handling

| Exception | When thrown |
|-----------|-------------|
| `ExpressionParseException` | Null/blank input or syntax error in `parse()` |
| `ExpressionEvaluationException` | Unknown field, division by zero, unknown function, wrong argument count, type mismatch, non-List field for `IN [field]` |
| `IllegalArgumentException` | Invalid custom function name at `CustomFunctionRegistry.Builder.register()` |

## EvaluationContext

Supported field value types: `Double`, `Integer`, `Long`, `BigDecimal` (all coerced to `double`),
`String`, `Boolean`, `List<Object>` (for `IN [field]` operator).

```java
// Single field
EvaluationContext.of("age", 25.0)

// Multiple fields
EvaluationContext.of(Map.of("age", 25.0, "status", "active"))

// Empty context (expressions with no field references)
EvaluationContext.empty()
```

Field names are **case-sensitive**: `[Age]` and `[age]` are distinct fields.

## Module Structure

| Module | Description |
|--------|-------------|
| `data-expression-parser-core` | Pure Java — ANTLR4 grammar, AST, parser, evaluator. No Spring dependency |
| `data-expression-parser-spring-boot-starter` | Spring Boot autoconfiguration. Depends on core |

The core module compiles and runs without Spring on the classpath. Consumers not using
Spring Boot can use `DataExpressionParser` directly:

```java
var evaluator = new ExpressionEvaluator();
var parser    = new DataExpressionParser(evaluator);
```

With custom functions:

```java
var registry = CustomFunctionRegistry.builder()
    .register("TAX", (args, ctx) -> args[0] * 0.15)
    .build();
var evaluator = new ExpressionEvaluator(registry);
var parser    = new DataExpressionParser(evaluator, registry);
```

## Technology Stack

| Concern | Technology |
|---------|-----------|
| Language | Java 21 LTS |
| Grammar | ANTLR4 4.13.2 |
| Framework | Spring Boot 3.5.x (starter only) |
| Build | Maven Multi-Module |
| Tests | JUnit 5 + AssertJ + Mockito |
| Commons | Apache Commons Lang 3 |

---

# Data Expression Parser

Переиспользуемая Java-библиотека для парсинга и вычисления бизнес-выражений над именованными полями данных.
Распространяется как Spring Boot Starter — добавьте одну зависимость и получите `DataExpressionParser`
как готовый Spring-бин.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-green?style=flat-square&logo=springboot)
![Maven](https://img.shields.io/badge/Build-Maven_Multi--Module-blue?style=flat-square&logo=apachemaven)
![Version](https://img.shields.io/badge/Version-1.2.0-brightgreen?style=flat-square)
![Tests](https://img.shields.io/badge/Tests-273_passing-success?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square)

273 тестов | 3 спринта | Maven Multi-Module (core + starter)

## Методология

Проект создан с использованием **Spec-Driven Development (SDD)** через [Spec Kit](https://github.com/speckit/speckit)
и [Claude Code](https://docs.anthropic.com/en/docs/claude-code).

Каждая фича проходила структурированный SDD-цикл:

- **Specify** — спецификация фичи на естественном языке с пользовательскими историями и критериями приёмки
- **Clarify** — структурированные вопросы-ответы для устранения неоднозначностей перед планированием
- **Plan** — архитектурный план с проверкой соответствия конституции
- **Checklist** — проверка качества перед реализацией
- **Tasks** — упорядоченный по зависимостям список задач с группировкой по фазам
- **Implement** — генерация кода по списку задач
- **Analyze** — пост-имплементационный обзор против спецификации и конституции

Библиотека разработана за 3 спринта, каждый из которых прошёл полный цикл.

## Возможности

- **Ссылки на поля** — синтаксис `[field_name]` резолвит значения из runtime-контекста
- **Арифметика** — `+`, `-`, `*`, `/`, `%`, `^`, `**` с корректным приоритетом операторов
- **Сравнения** — `>`, `<`, `>=`, `<=`, `==`, `!=` для чисел и строк
- **Логические операторы** — `AND`, `OR`, `NOT` (регистронезависимые)
- **IN / NOT IN** — проверка вхождения в статический список или динамическую коллекцию из контекста
- **Встроенные функции** — `abs`, `round`, `floor`, `ceil`, `min`, `max`, `pow`
- **Пользовательские функции** — регистрация именованных функций с доступом к `EvaluationContext`
- **Валидация синтаксиса** — проверка корректности выражения без вычисления
- **Spring Boot Starter** — zero-config автоконфигурация через `@ConditionalOnMissingBean`
- **Потокобезопасность** — stateless парсер, безопасен как singleton Spring-бин

## Поддерживаемый синтаксис

### Операторы (по приоритету, от низкого к высокому)

| Уровень | Операторы | Пример |
|---------|-----------|--------|
| Логическое OR | `OR` | `[a] > 1 OR [b] > 1` |
| Логическое AND | `AND` | `[a] > 1 AND [b] > 1` |
| Логическое NOT | `NOT` | `NOT [is_blocked]` |
| Сравнение | `>` `<` `>=` `<=` `==` `!=` | `[age] >= 18` |
| IN / NOT IN | `IN` `NOT IN` | `[status] IN ('active', 'trial')` |
| Сложение | `+` `-` | `[price] + [tax]` |
| Умножение | `*` `/` `%` | `[price] * [qty]` |
| Степень | `^` `**` | `[x] ^ 2` |
| Унарный | `-` | `-[value]` |
| Первичный | `[field]` `'string'` `42` `true` | `[name]` |

### Ссылки на поля

Имена полей заключаются в квадратные скобки и поддерживают любые символы кроме `]` и перевода строки:
[age]
[first name]
[Сумма долга]
[amount (USD)]

### Строковые литералы

Только одинарные кавычки:
[status] == 'active'
[role] IN ('admin', 'moderator')

### Булевы литералы

Регистронезависимые: `true`, `TRUE`, `True`, `false`, `FALSE`, `False`

### IN с динамической коллекцией

Правая часть `IN` / `NOT IN` может быть ссылкой на поле, содержащее `List<Object>`:
[status] IN [allowed_statuses]
[code] NOT IN [blocked_codes]

## Быстрый старт

### 1. Добавить зависимость

```xml
<dependency>
    <groupId>ru.zahaand</groupId>
    <artifactId>data-expression-parser-spring-boot-starter</artifactId>
    <version>1.2.0</version>
</dependency>
```

### 2. Внедрить и использовать

```java
@Autowired
DataExpressionParser parser;
```

## Использование

### Вычисление булевого выражения

```java
var ctx = EvaluationContext.of(Map.of(
    "age",    25.0,
    "status", "active"
));

boolean result = parser.evaluateBoolean(
    "[age] > 18 AND [status] == 'active'",
    ctx
); // → true
```

### Вычисление арифметического выражения

```java
var ctx = EvaluationContext.of(Map.of(
    "price",    100.0,
    "qty",      3.0,
    "discount", 0.1
));

double total = parser.evaluateDouble(
    "[price] * [qty] * (1 - [discount])",
    ctx
); // → 270.0
```

### Парсинг один раз — вычисление много раз

Парсинг — дорогая операция; вычисление — дешёвая. Разберите выражение один раз и переиспользуйте AST:

```java
// Парсинг один раз
Expression ast = parser.parse("[age] > 18 AND [status] == 'active'");

// Вычисление много раз с разными контекстами — без лишнего парсинга
for (Map<String, Object> row : dataRows) {
    boolean passed = parser.evaluateBoolean(ast, EvaluationContext.of(row));
}

// Все три типа результата поддерживаются с предварительно разобранным AST
boolean boolResult  = parser.evaluateBoolean(ast, ctx);
double  dblResult   = parser.evaluateDouble(ast, ctx);
EvaluationResult r  = parser.evaluate(ast, ctx);
```

### IN со статическим списком

```java
boolean isAllowed = parser.evaluateBoolean(
    "[role] IN ('admin', 'moderator', 'editor')",
    EvaluationContext.of("role", "admin")
); // → true
```

### IN с динамической коллекцией из контекста

```java
var ctx = EvaluationContext.of(Map.of(
    "status",  "active",
    "allowed", List.of("active", "trial", "premium")
));

boolean result = parser.evaluateBoolean("[status] IN [allowed]", ctx); // → true
```

### Пользовательские функции

Определите бин `CustomFunctionRegistry`. Пользовательские функции получают вычисленные аргументы `double[]`
и активный `EvaluationContext`:

```java
@Bean
public CustomFunctionRegistry customFunctionRegistry() {
    return CustomFunctionRegistry.builder()
        .register("TAX", (args, ctx) ->
            args[0] * 0.15)
        .register("DISCOUNT", (args, ctx) -> {
            String tier = (String) ctx.get("customer_tier");
            return args[0] * ("premium".equals(tier) ? 0.8 : 0.95);
        })
        .build();
}
```

Затем используйте в выражениях:

```java
double tax = parser.evaluateDouble(
    "TAX([price])",
    EvaluationContext.of("price", 100.0)
); // → 15.0

double discounted = parser.evaluateDouble(
    "DISCOUNT([price])",
    EvaluationContext.of(Map.of("price", 100.0, "customer_tier", "premium"))
); // → 80.0
```

**Правила регистрации:**
- Имена пользовательских функций должны соответствовать `[a-zA-Z_][a-zA-Z_0-9]*`
- Имена, конфликтующие со встроенными функциями (`abs`, `round`, `floor`, `ceil`, `min`, `max`, `pow`), отклоняются при регистрации
- Дублирующиеся имена (регистронезависимо) отклоняются при регистрации

### Валидация синтаксиса выражения

Проверка без вычисления — полезно для admin-интерфейсов, где пользователи вводят выражения:

```java
ValidationResult result = parser.validate("[age] > 18 AND [status] == 'active'");
result.isValid();       // → true
result.errorMessage();  // → Optional.empty()

ValidationResult bad = parser.validate("[age] >");
bad.isValid();          // → false
bad.errorMessage();     // → Optional.of("Parse error at line 1:7: ...")
```

## Встроенные функции

| Функция | Аргументы | Поведение |
|---------|-----------|-----------|
| `abs(x)` | 1 | `Math.abs(x)` |
| `round(x)` | 1 | `(double) Math.round(x)` |
| `floor(x)` | 1 | `Math.floor(x)` |
| `ceil(x)` | 1 | `Math.ceil(x)` |
| `min(x, y)` | 2 | `Math.min(x, y)` |
| `max(x, y)` | 2 | `Math.max(x, y)` |
| `pow(x, y)` | 2 | `Math.pow(x, y)` |

Имена функций резолвятся регистронезависимо: `ABS([x])` и `abs([x])` эквивалентны.

## Обработка исключений

| Исключение | Когда бросается |
|------------|-----------------|
| `ExpressionParseException` | Null/пустой ввод или синтаксическая ошибка в `parse()` |
| `ExpressionEvaluationException` | Неизвестное поле, деление на ноль, неизвестная функция, неверное количество аргументов, несовместимые типы, не-List поле для `IN [field]` |
| `IllegalArgumentException` | Недопустимое имя пользовательской функции в `CustomFunctionRegistry.Builder.register()` |

## EvaluationContext

Поддерживаемые типы значений полей: `Double`, `Integer`, `Long`, `BigDecimal` (все приводятся к `double`),
`String`, `Boolean`, `List<Object>` (для оператора `IN [field]`).

```java
// Одно поле
EvaluationContext.of("age", 25.0)

// Несколько полей
EvaluationContext.of(Map.of("age", 25.0, "status", "active"))

// Пустой контекст (выражения без ссылок на поля)
EvaluationContext.empty()
```

Имена полей **чувствительны к регистру**: `[Age]` и `[age]` — разные поля.

## Структура модулей

| Модуль | Описание |
|--------|----------|
| `data-expression-parser-core` | Чистая Java — грамматика ANTLR4, AST, парсер, вычислитель. Без зависимости от Spring |
| `data-expression-parser-spring-boot-starter` | Spring Boot автоконфигурация. Зависит от core |

Core-модуль компилируется и запускается без Spring в classpath. Потребители без Spring Boot
могут использовать `DataExpressionParser` напрямую:

```java
var evaluator = new ExpressionEvaluator();
var parser    = new DataExpressionParser(evaluator);
```

С пользовательскими функциями:

```java
var registry = CustomFunctionRegistry.builder()
    .register("TAX", (args, ctx) -> args[0] * 0.15)
    .build();
var evaluator = new ExpressionEvaluator(registry);
var parser    = new DataExpressionParser(evaluator, registry);
```

## Стек технологий

| Область | Технология |
|---------|-----------|
| Язык | Java 21 LTS |
| Грамматика | ANTLR4 4.13.2 |
| Фреймворк | Spring Boot 3.5.x (только starter) |
| Сборка | Maven Multi-Module |
| Тесты | JUnit 5 + AssertJ + Mockito |
| Утилиты | Apache Commons Lang 3 |
