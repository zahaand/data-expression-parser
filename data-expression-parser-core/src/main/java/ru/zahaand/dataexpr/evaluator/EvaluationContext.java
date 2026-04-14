package ru.zahaand.dataexpr.evaluator;

import ru.zahaand.dataexpr.exception.ExpressionEvaluationException;

import java.util.Map;

/**
 * Immutable map of named field values supplied to expression evaluation.
 *
 * <p>Field values may be of type {@link Double}, {@link Integer}, {@link Long},
 * {@link java.math.BigDecimal} (all coerced to {@code double} during arithmetic),
 * {@link String}, {@link Boolean}, or {@link java.util.List} (for the {@code IN [field]} operator).
 *
 * <p>Field name lookup is <strong>case-sensitive</strong>: {@code [Age]} and {@code [age]}
 * refer to distinct fields.
 *
 * <p>{@code null} values are not permitted. Passing {@code null} as a field value at
 * construction time throws {@link ExpressionEvaluationException}.
 */
public final class EvaluationContext {

    private final Map<String, Object> fields;

    private EvaluationContext(Map<String, Object> fields) {
        this.fields = Map.copyOf(fields);
    }

    /**
     * Returns an empty context with no field values.
     *
     * <p>Suitable for expressions that contain no field references (e.g. {@code 2 + 2}).
     *
     * @return an empty {@code EvaluationContext}
     */
    public static EvaluationContext empty() {
        return new EvaluationContext(Map.of());
    }

    /**
     * Creates a context containing a single field.
     *
     * @param name  the field name; must not be {@code null} or blank
     * @param value the field value; must not be {@code null}
     * @return a new {@code EvaluationContext} with one entry
     * @throws ExpressionEvaluationException if {@code value} is {@code null}
     */
    public static EvaluationContext of(String name, Object value) {
        if (value == null) {
            throw new ExpressionEvaluationException(
                    "Field value must not be null for field: '" + name + "'");
        }
        return new EvaluationContext(Map.of(name, value));
    }

    /**
     * Creates a context from a map of field name-value pairs.
     *
     * <p>The map is copied defensively; subsequent changes to the source map do not affect
     * this context.
     *
     * @param fields the field name-value pairs; must not be {@code null}; values must not be {@code null}
     * @return a new {@code EvaluationContext}
     * @throws ExpressionEvaluationException if any value in {@code fields} is {@code null}
     */
    public static EvaluationContext of(Map<String, Object> fields) {
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (entry.getValue() == null) {
                throw new ExpressionEvaluationException(
                        "Field value must not be null for field: '" + entry.getKey() + "'");
            }
        }
        return new EvaluationContext(fields);
    }

    /**
     * Returns the value associated with the given field name.
     *
     * @param fieldName the field name to look up; case-sensitive
     * @return the field value; never {@code null}
     * @throws ExpressionEvaluationException if the field name is not present in this context
     */
    public Object get(String fieldName) {
        Object value = fields.get(fieldName);
        if (value == null && !fields.containsKey(fieldName)) {
            throw new ExpressionEvaluationException(
                    "Unknown field: '" + fieldName + "'");
        }
        return value;
    }
}
