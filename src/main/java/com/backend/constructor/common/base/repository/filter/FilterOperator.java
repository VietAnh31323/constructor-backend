package com.backend.constructor.common.base.repository.filter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FilterOperator {
    EQUAL(" = "),
    NOT_EQUAL(" != "),
    IN(" in "),
    NOT_IN(" not in "),
    LIKE(" like "),
    NOT_LIKE(" not like "),
    IS(" is "),
    IS_NOT(" is not "),
    GREATER_THAN(" > "),
    LESS_THAN(" < "),
    GREATER_THAN_OR_EQUAL(" >= "),
    LESS_THAN_OR_EQUAL(" <= ");

    private final String value;

    public String value() {
        return this.value;
    }
}