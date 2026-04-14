package ru.zahaand.dataexpr.function;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class CustomFunctionRegistry {

    private static final Logger log = LoggerFactory.getLogger(CustomFunctionRegistry.class);

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");

    private static final CustomFunctionRegistry EMPTY = new CustomFunctionRegistry(Collections.emptyMap());

    private final Map<String, ExpressionFunction> functions;

    private CustomFunctionRegistry(Map<String, ExpressionFunction> functions) {
        this.functions = functions;
    }

    public static CustomFunctionRegistry empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ExpressionFunction find(String name) {
        if (name == null) {
            return null;
        }
        return functions.get(name.toLowerCase(Locale.ROOT));
    }

    public static final class Builder {

        private final Map<String, ExpressionFunction> entries = new LinkedHashMap<>();

        private Builder() {
        }

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

        public CustomFunctionRegistry build() {
            return new CustomFunctionRegistry(Collections.unmodifiableMap(new LinkedHashMap<>(entries)));
        }
    }
}
