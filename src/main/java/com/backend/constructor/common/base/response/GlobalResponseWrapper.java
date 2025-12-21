package com.backend.constructor.common.base.response;


import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Locale;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalResponseWrapper implements ResponseBodyAdvice<Object> {
    private final MessageSource messageSource;

    @Override
    public boolean supports(MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        if (returnType.getContainingClass().getPackageName().contains("springdoc") || returnType.getContainingClass().getPackageName().contains("swagger")) {
            return false;
        }
        return !Response.class.isAssignableFrom(returnType.getParameterType())
                && MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  @NonNull MethodParameter returnType,
                                  @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NonNull ServerHttpRequest request,
                                  @NonNull ServerHttpResponse response) {

        try {
            // Nếu controller tự trả Response → không wrap nữa
            if (body instanceof Response) {
                return body;
            }
            // Lấy messageKey từ ThreadLocal
            String messageKey = MessageResponseContext.getResourceMessage();
            // Không có messageKey → dùng default
            if (messageKey == null || messageKey.isBlank()) {
                return SuccessResponse.ok(body);
            }
            String message;
            try {
                Locale locale = LocaleContextHolder.getLocale();
                message = messageSource.getMessage(messageKey, null, locale);
            } catch (Exception ex) {
                message = messageKey;
            }
            return SuccessResponse.ok(message, body);

        } finally {
            MessageResponseContext.clear();
        }
    }
}