package com.collabflow.shared;

/**
 * One field that changed, as text, for the audit log: {@code status: TO_DO -> IN_PROGRESS}.
 *
 * <p>Text rather than the real types, because one log row holds changes to dates, enums, names
 * and lists of people. Storing them as strings keeps one shape for all of them, and what the log
 * needs is what a person reads, not something to calculate with.
 */
public record FieldChange(String field, String oldValue, String newValue) {

    public static FieldChange of(String field, Object oldValue, Object newValue) {
        return new FieldChange(field, text(oldValue), text(newValue));
    }

    /** A field that had no value before, e.g. when the item is created. */
    public static FieldChange set(String field, Object newValue) {
        return new FieldChange(field, null, text(newValue));
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
