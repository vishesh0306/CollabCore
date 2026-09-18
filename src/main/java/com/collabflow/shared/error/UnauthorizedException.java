package com.collabflow.shared.error;

/** The caller couldn't be identified, e.g. wrong email or password at login. Becomes a 401. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
