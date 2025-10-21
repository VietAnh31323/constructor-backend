package com.backend.constructor.user.mapper;

import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.common.enums.TokenType;
import com.backend.constructor.user.dto.request.SignUpRequest;
import com.backend.constructor.user.dto.response.AccountDto;
import com.backend.constructor.user.entity.AccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.security.oauth2.jwt.Jwt;

@Mapper(
        config = DefaultConfigMapper.class,
        imports = {TokenType.class}
)
public interface AccountMapper extends EntityMapper<AccountEntity, AccountDto, AccountDto> {
    @Mapping(target = "avatar", ignore = true)
    AccountEntity toEntity(SignUpRequest request);

    @Mapping(target = "username", source = "token.subject")
    @Mapping(target = "tokenType", expression = "java(TokenType.Bearer.name())")
    @Mapping(target = "token", source = "token.tokenValue")
    @Mapping(target = "refreshToken", source = "refreshToken.tokenValue")
    @Mapping(target = "tokenExpiry", source = "token.expiresAt")
    @Mapping(target = "refreshTokenExpiry", source = "refreshToken.expiresAt")
    AccountDto toDto(Jwt token, Jwt refreshToken);

    @Mapping(target = "username", source = "token.subject")
    @Mapping(target = "tokenType", expression = "java(TokenType.Bearer.name())")
    @Mapping(target = "token", source = "token.tokenValue")
    @Mapping(target = "tokenExpiry", source = "token.expiresAt")
    AccountDto toDto(Jwt token);
}
