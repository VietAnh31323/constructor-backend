package com.backend.constructor.common.base.entity;

public interface EntityID<T> {
    T getId();

    void setId(T id);
}