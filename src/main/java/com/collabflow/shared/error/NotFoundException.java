package com.collabflow.shared.error;

/**
 * Thrown when something doesn't exist, or the caller isn't allowed to know it exists
 * (e.g. a task in another team). The API turns it into a 404 response.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
