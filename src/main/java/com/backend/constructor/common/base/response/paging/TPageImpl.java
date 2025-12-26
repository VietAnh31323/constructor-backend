package com.backend.constructor.common.base.response.paging;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

@RequiredArgsConstructor
public class TPageImpl<T> implements TPage<T>, Page<T> {
    private final Page<T> delegate;

    @JsonProperty(index = 0)
    @Override
    @NonNull
    public List<T> getContent() {
        return this.delegate.getContent();
    }

    @JsonProperty(index = 1)
    @Override
    public int getPage() {
        return this.delegate.getNumber();
    }

    @JsonProperty(index = 2)
    @Override
    public int getSize() {
        return this.delegate.getSize();
    }

    @JsonProperty(value = "sort", index = 3)
    @Override
    public String getTSort() {
        return this.delegate.getSort().toString();
    }

    @JsonProperty(index = 4)
    @Override
    public long getTotalElements() {
        return this.delegate.getTotalElements();
    }

    @JsonProperty(index = 5)
    @Override
    public int getTotalPages() {
        return this.delegate.getTotalPages();
    }

    @JsonProperty(index = 6)
    @Override
    public int getNumberOfElements() {
        return this.delegate.getNumberOfElements();
    }

    // ===== Hidden Fields (Not serialized to JSON) =====

    @JsonIgnore
    @Override
    @NonNull
    public Sort getSort() {
        return this.delegate.getSort();
    }

    @JsonIgnore
    @Override
    public boolean hasContent() {
        return this.delegate.hasContent();
    }

    @JsonIgnore
    @Override
    @NonNull
    public Pageable getPageable() {
        return this.delegate.getPageable();
    }

    @JsonIgnore
    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @JsonIgnore
    @Override
    public int getNumber() {
        return this.delegate.getNumber();
    }

    @JsonIgnore
    @Override
    public boolean isFirst() {
        return this.delegate.isFirst();
    }

    @JsonIgnore
    @Override
    public boolean isLast() {
        return this.delegate.isLast();
    }

    @JsonIgnore
    @Override
    public boolean hasNext() {
        return this.delegate.hasNext();
    }

    @JsonIgnore
    @Override
    public boolean hasPrevious() {
        return this.delegate.hasPrevious();
    }

    @JsonIgnore
    @Override
    @NonNull
    public Pageable nextPageable() {
        return this.delegate.nextPageable();
    }

    @JsonIgnore
    @Override
    @NonNull
    public Pageable previousPageable() {
        return this.delegate.previousPageable();
    }

    @JsonIgnore
    @Override
    @NonNull
    public <U> Page<U> map(@NonNull Function<? super T, ? extends U> converter) {
        return this.delegate.map(converter);
    }

    @JsonIgnore
    @Override
    @NonNull
    public Iterator<T> iterator() {
        return this.delegate.iterator();
    }
}