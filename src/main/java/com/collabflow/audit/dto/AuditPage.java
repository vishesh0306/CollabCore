package com.collabflow.audit.dto;

import java.util.List;

/**
 * One page of the log, newest first.
 *
 * <p>There is no page number and no total. The log can hold millions of rows, and counting them
 * on every request is the slowest thing the query could do. Instead each page hands back
 * {@code nextBefore}: pass it as {@code before} to get the next page. Null means there is no more.
 */
public record AuditPage(List<AuditEntryResponse> items, Long nextBefore) {

    public static AuditPage of(List<AuditEntryResponse> items, int askedFor) {
        Long next = items.size() < askedFor ? null : items.get(items.size() - 1).id();
        return new AuditPage(items, next);
    }
}
