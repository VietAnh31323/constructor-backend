package com.backend.constructor.core.service;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class HelperService {

    private HelperService() {
    }

    public static String joinName(String... parts) {
        return StringUtils.joinWith(" ", (Object[]) parts);
    }

    public static <K, V> V getData(Map<K, V> map, K id) {
        if (Objects.isNull(id) || Objects.isNull(map)) return null;
        return map.get(id);
    }

    public static <T> void addIfNotNull(Set<T> ids, T id) {
        if (Objects.nonNull(id)) ids.add(id);
    }
}

