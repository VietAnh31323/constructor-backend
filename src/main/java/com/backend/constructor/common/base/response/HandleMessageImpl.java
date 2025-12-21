package com.backend.constructor.common.base.response;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class HandleMessageImpl implements HandleMessage {
    private final MessageSource messageSource;

    @Override
    public String getMessage(String key, Exception ex) {
        String message;
        try {
            Locale locale = LocaleContextHolder.getLocale();
            message = messageSource.getMessage(ex.getMessage(), null, locale);
        } catch (Exception e) {
            message = ex.getMessage();
        }
        return message;
    }
}
