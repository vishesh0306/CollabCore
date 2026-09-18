package com.collabflow.shared;

import java.util.List;

import org.springframework.data.domain.Page;

/** One page of a longer list. Pages are numbered from 0. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
