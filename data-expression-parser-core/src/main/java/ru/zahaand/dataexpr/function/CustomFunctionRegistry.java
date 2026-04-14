package ru.zahaand.dataexpr.function;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Immutable registry of consumer-defined {@link ExpressionFunction} instances,
 * keyed by case-insensitive function name.
 *
 * <p>Build a registry using the {@link Builder}:
 * <pre>{@code
 * CustomFunctionRegistry registry = CustomFunctionRegistry.builder()
 *     .register("TAX",    (args, ctx) -> args[0] * 0.15)
 *     .register("ROUND2", (args, ctx) -> Math.round(args[0] * 100.0) / 100.0)
 *     .build();
 * }</pre>
 *
 * <p>For Spring Boot, declare this as a {@code @Bean}. The autoconfiguration will
 * inject it into {@link ru.zahaand.dataexpr.parser.DataExpressionParser} automatically:
 * <pre>{@code
 * @Bean
 * public CustomFunctionRegistry customFunctionRegistry() {
 *     return CustomFunctionRegistry.builder()
 *         .register("TAX", (args, ctx) -> args[0] * 0.15)
 *         .build();
 * }
 * }</pre>
 *
 * <p>During evaluation, custom functions take precedence over built-ins by name lookup order.
 * Built-in names ({@code abs}, {@code round}, {@code floor}, {@code ceil}, {@code min},
 * {@code max}, {@code pow}) cannot be overridden — registration of a conflicting name
 * throws {@link IllegalArgumentException} at build time.
 */
public final class CustomFunctionRegistry {

    private static final Logger log = LoggerFactory.getLogger(CustomFunctionRegistry.class);

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");

    private static final CustomFunctionRegistry EMPTY = new CustomFunctionRegistry(Collections.emptyMap());

    private final Map<String, ExpressionFunction> functions;

    private CustomFunctionRegistry(Map<String, ExpressionFunction> functions) {
        this.functions = functions;
    }

    /**
     * Returns an empty registry with no custom functions.
     *
     * <p>This is the default used by autoconfiguration when no consumer {@code @Bean} is defined.
     * An empty registry causes the evaluator to fall back to built-in functions only.
     *
     * @return an empty {@code CustomFunctionRegistry}
     */
    public static CustomFunctionRegistry empty() {
        return EMPTY;
    }

    /**
     * Returns a new {@link Builder} for constructing a {@code CustomFunctionRegistry}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Looks up a custom function by name (case-insensitive).
     *
     * @param name the function name to look up; must not be {@code null}
     * @return the registered {@link ExpressionFunction}, or {@code null} if not found
     */
    public ExpressionFunction find(String name) {
        if (name == null) {
            return null;
        }
        return functions.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Builder for {@link CustomFunctionRegistry}.
     *
     * <p>Each call to {@link #register(String, ExpressionFunction)} validates the name immediately
     * (fail-fast). The registry becomes immutable after {@link #build()} is called.
     */
    public static final class Builder {

        private final Map<String, ExpressionFunction> entries = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Registers a custom function under the given name.
         *
         * <p>Names are stored and looked up case-insensitively (lowercased via {@code Locale.ROOT}).
         *
         * <p>Validation rules (checked in order at registration time):
         * <ol>
         *   <li>Name must not be {@code null} or blank.</li>
         *   <li>Name must match {@code ^[a-zA-Z_][a-zA-Z_0-9]*$} (valid grammar identifier).</li>
         *   <li>Name must not conflict with any built-in function name (case-insensitive).</li>
         *   <li>Name must not already be registered on this builder (case-insensitive).</li>
         *   <li>Function must not be {@code null}.</li>
         * </ol>
         *
         * @param name     the function name; must be a valid identifier, not a built-in, not a duplicate
         * @param function the function implementation; must not be {@code null}
         * @return this builder (for chaining)
         * @throws IllegalArgumentException if any validation rule is violated
         */
        public Builder register(String name, ExpressionFunction function) {
            if (StringUtils.isBlank(name)) {
                String msg = "Function name must not be null or blank";
                log.error("Custom function registration failed: {}", msg);
                throw new IllegalArgumentException(msg);
            }
            if (!IDENTIFIER_PATTERN.matcher(name).matches()) {
                String msg = "Function name '" + name + "' is not a valid identifier. Must match [a-zA-Z_][a-zA-Z_0-9]*";
                log.error("Custom function registration failed for name '{}': {}", name, msg);
                throw new IllegalArgumentException(msg);
            }
            String key = name.toLowerCase(Locale.ROOT);
            if (BuiltinFunctionRegistry.BUILTIN_NAMES.contains(key)) {
                String msg = "Function name '" + name + "' conflicts with built-in function";
                log.error("Custom function registration failed for name '{}': {}", name, msg);
                throw new IllegalArgumentException(msg);
            }
            if (entries.containsKey(key)) {
                String msg = "Custom function '" + name + "' is already registered";
                log.error("Custom function registration failed for name '{}': {}", name, msg);
                throw new IllegalArgumentException(msg);
            }
            if (function == null) {
                String msg = "Function must not be null";
                log.error("Custom function registration failed for name '{}': {}", name, msg);
                throw new IllegalArgumentException(msg);
            }
            entries.put(key, function);
            return this;
        }

        /**
         * Builds and returns an immutable {@link CustomFunctionRegistry} from the registered functions.
         *
         * @return a new immutable registry
         */
        public CustomFunctionRegistry build() {
            return new CustomFunctionRegistry(Collections.unmodifiableMap(new LinkedHashMap<>(entries)));
        }
    }
}
