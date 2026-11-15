package com.backend.constructor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private static final String PREFIX_ROLE = "ROLE_";
    private static final String PREFIX_SCOPE = "SCOPE_";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String scopeClaim = jwt.getClaimAsString("scope");
        if (!StringUtils.hasText(scopeClaim)) {
            return new JwtAuthenticationToken(jwt, Collections.emptyList(), jwt.getSubject());
        }

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (String raw : scopeClaim.split("[,\\s]+")) {
            String auth = raw.trim();
            if (auth.isEmpty()) {
                continue;
            }
            authorities.add(new SimpleGrantedAuthority(auth));
            if (!auth.startsWith(PREFIX_SCOPE)) {
                authorities.add(new SimpleGrantedAuthority(PREFIX_SCOPE + auth));
            }
            if (!auth.startsWith(PREFIX_ROLE)) {
                authorities.add(new SimpleGrantedAuthority(PREFIX_ROLE + auth));
            }
        }

        log.debug("Converted JWT authorities for {}: {}", jwt.getSubject(), authorities);
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
