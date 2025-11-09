package com.backend.constructor.core.port.mapper;

import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.AccountEntity;
import com.backend.constructor.core.domain.enums.AuthScheme;
import com.backend.constructor.user.dto.request.SignUpRequest;
import com.backend.constructor.user.dto.response.AccountDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.security.oauth2.jwt.Jwt;

@Mapper(config = DefaultConfigMapper.class,
        imports = {AuthScheme.class})
public interface AccountMapper extends EntityMapper<AccountEntity, AccountDto> {
    AccountEntity toEntity(SignUpRequest request);

    @Mapping(target = "username", source = "token.subject")
    @Mapping(target = "tokenType", expression = "java(AuthScheme.BEARER.getValue())")
    @Mapping(target = "token", source = "token.tokenValue")
    @Mapping(target = "refreshToken", source = "refreshToken.tokenValue")
    @Mapping(target = "tokenExpiry", source = "token.expiresAt")
    @Mapping(target = "refreshTokenExpiry", source = "refreshToken.expiresAt")
    AccountDto toDto(Jwt token, Jwt refreshToken);

    @Mapping(target = "username", source = "token.subject")
    @Mapping(target = "tokenType", expression = "java(AuthScheme.BEARER.getValue())")
    @Mapping(target = "token", source = "token.tokenValue")
    @Mapping(target = "tokenExpiry", source = "token.expiresAt")
    AccountDto toDto(Jwt token);
}
