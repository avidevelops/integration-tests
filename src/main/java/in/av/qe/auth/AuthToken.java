package in.av.qe.auth;

import lombok.Getter;

import java.time.Instant;

public class AuthToken {
    @Getter
    private final String value;
    private final Instant expiresAt;

    public AuthToken(String value, long expiresInSeconds) {
        this.value = value;
        this.expiresAt = Instant.now().plusSeconds(expiresInSeconds);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public String bearerHeader() {
        return "Bearer " + value;
    }

    @Override
    public String toString() {
        return "AuthToken[expires=" + expiresAt + "]";
    }

}
