package com.collabflow.shared.error;

/** The request clashes with existing data, e.g. an email that is already registered. Becomes a 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
