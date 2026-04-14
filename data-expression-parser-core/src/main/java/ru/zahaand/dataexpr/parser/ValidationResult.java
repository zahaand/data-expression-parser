package ru.zahaand.dataexpr.parser;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public final class ValidationResult {

    private static final ValidationResult VALID = new ValidationResult(true, null);

    private final boolean valid;
    private final String errorMessage;

    private ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public static ValidationResult valid() {
        return VALID;
    }

    public static ValidationResult invalid(String errorMessage) {
        if (StringUtils.isBlank(errorMessage)) {
            throw new IllegalArgumentException("Error message must not be null or blank");
        }
        return new ValidationResult(false, errorMessage);
    }

    public boolean isValid() {
        return valid;
    }

    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }
}
