package com.collabflow.shared.error;

/** The request is well-formed but can't be accepted, e.g. a wrong current password. Becomes a 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
