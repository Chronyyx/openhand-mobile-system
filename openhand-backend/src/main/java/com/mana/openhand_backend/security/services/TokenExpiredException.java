package com.mana.openhand_backend.security.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TokenExpiredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TokenExpiredException(String token, String message) {
        super(String.format("Failed for [%s]: %s", token, message));
    }
}
