package com.backend.constructor.common.base.repository.filter;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FilterFunction {
    private String functionName;
    private String[] fieldNames;
}