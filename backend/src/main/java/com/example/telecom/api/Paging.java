package com.example.telecom.api;

import org.springframework.data.domain.PageRequest;

/** Clamps client paging parameters to safe values. */
public final class Paging {

    public static final int MAX_PAGE_SIZE = 100;

    private Paging() {
    }

    public static PageRequest of(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, MAX_PAGE_SIZE)));
    }
}
