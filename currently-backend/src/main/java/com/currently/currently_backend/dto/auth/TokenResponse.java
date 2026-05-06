package com.currently.currently_backend.dto.auth;

/*
 * DTO: TokenResponse
 * Purpose: Sends the JWT token back to React after successful Login or Register.
 */
public class TokenResponse {
    private final String token;

    public TokenResponse(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
