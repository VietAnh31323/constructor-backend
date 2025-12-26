package com.backend.constructor.common.base.response.paging;

import lombok.NonNull;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TPage<T> extends Page<T> {
    /**
     * Get page content
     */
    @NonNull
    List<T> getContent();

    /**
     * Get current page number (0-based)
     */
    int getPage();

    /**
     * Get page size
     */
    int getSize();

    /**
     * Get sort information as string
     */
    String getTSort();

    /**
     * Get total number of elements
     */
    long getTotalElements();

    /**
     * Get total number of pages
     */
    int getTotalPages();

    /**
     * Get number of elements in current page
     */
    int getNumberOfElements();
}