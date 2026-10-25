
package com.payshield.identity.dto.response;

public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;

    public AuthResponse() {
    }

    public AuthResponse(String accessToken) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = 900; // 15 minutes in seconds
    }

    public AuthResponse(
            String accessToken,
            String tokenType,
            long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
