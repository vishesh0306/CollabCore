package com.collabflow.shared.error;

/** The caller is known and may see the thing, but isn't allowed to do this to it. Becomes a 403. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
