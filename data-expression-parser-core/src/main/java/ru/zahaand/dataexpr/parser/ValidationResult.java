package ru.zahaand.dataexpr.parser;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * Immutable result of a {@link DataExpressionParser#validate(String)} call.
 *
 * <p>A valid result indicates the expression is syntactically correct.
 * An invalid result carries an error message with the ANTLR line and column position
 * of the first syntax error.
 *
 * <p>Note: validation checks syntax only. Expressions referencing undefined functions
 * or fields are syntactically valid and return {@link #valid()}.
 */
public final class ValidationResult {

    private static final ValidationResult VALID = new ValidationResult(true, null);

    private final boolean valid;
    private final String errorMessage;

    private ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    /**
     * Returns a result indicating syntactically correct expression.
     *
     * @return a valid {@code ValidationResult}
     */
    public static ValidationResult valid() {
        return VALID;
    }

    /**
     * Returns a result indicating a syntax error.
     *
     * @param errorMessage the error message including line and column position;
     *                     must not be {@code null} or blank
     * @return an invalid {@code ValidationResult}
     * @throws IllegalArgumentException if {@code errorMessage} is {@code null} or blank
     */
    public static ValidationResult invalid(String errorMessage) {
        if (StringUtils.isBlank(errorMessage)) {
            throw new IllegalArgumentException("Error message must not be null or blank");
        }
        return new ValidationResult(false, errorMessage);
    }

    /**
     * Returns {@code true} if the expression is syntactically correct.
     *
     * @return {@code true} if valid, {@code false} otherwise
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns the error message when the expression is syntactically invalid.
     *
     * <p>The message format is: {@code "Parse error at line <L>:<C>: <description>"}
     *
     * @return an {@link java.util.Optional} containing the error message if invalid,
     *         or {@link java.util.Optional#empty()} if valid
     */
    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }
}
