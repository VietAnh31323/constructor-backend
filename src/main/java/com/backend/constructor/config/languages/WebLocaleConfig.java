package com.backend.constructor.config.languages;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
@Slf4j
public class WebLocaleConfig implements WebMvcConfigurer {

    private static final List<Locale> SUPPORTED_LOCALES = List.of(
            Locale.forLanguageTag("vi"),
            Locale.forLanguageTag("en")
    );

    @Bean
    public LocaleResolver localeResolver() {
        return new AcceptHeaderLocaleResolver() {
            @Override
            @NonNull
            public Locale resolveLocale(@NonNull HttpServletRequest request) {
                String headerLang = request.getHeader("Accept-Language");
                if (!StringUtils.hasText(headerLang)) {
                    return Locale.forLanguageTag("vi"); // Mặc định là tiếng Việt
                }
                List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(headerLang);
                Locale matchedLocale = Locale.lookup(ranges, SUPPORTED_LOCALES);
                return matchedLocale != null ? matchedLocale : Locale.forLanguageTag("vi");
            }
        };
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages"); // ví dụ: i18n/messages_vi.properties
        source.setDefaultEncoding("UTF-8");
        source.setUseCodeAsDefaultMessage(true);
        return source;
    }
}
