package com.example.telecom.api;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** The paged list shape every list endpoint returns. */
public record PageResponse<T>(List<T> content, int page, int totalPages, long totalElements, boolean hasNext) {

    public PageResponse {
        content = List.copyOf(content);
    }

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }
}
