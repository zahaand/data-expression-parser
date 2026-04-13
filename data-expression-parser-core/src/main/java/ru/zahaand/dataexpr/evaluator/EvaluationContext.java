package ru.zahaand.dataexpr.evaluator;

import ru.zahaand.dataexpr.exception.ExpressionEvaluationException;

import java.util.Map;

public final class EvaluationContext {

    private final Map<String, Object> fields;

    private EvaluationContext(Map<String, Object> fields) {
        this.fields = Map.copyOf(fields);
    }

    public static EvaluationContext empty() {
        return new EvaluationContext(Map.of());
    }

    public static EvaluationContext of(String name, Object value) {
        if (value == null) {
            throw new ExpressionEvaluationException(
                    "Field value must not be null for field: '" + name + "'");
        }
        return new EvaluationContext(Map.of(name, value));
    }

    public static EvaluationContext of(Map<String, Object> fields) {
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (entry.getValue() == null) {
                throw new ExpressionEvaluationException(
                        "Field value must not be null for field: '" + entry.getKey() + "'");
            }
        }
        return new EvaluationContext(fields);
    }

    public Object get(String fieldName) {
        Object value = fields.get(fieldName);
        if (value == null && !fields.containsKey(fieldName)) {
            throw new ExpressionEvaluationException(
                    "Unknown field: '" + fieldName + "'");
        }
        return value;
    }
}
