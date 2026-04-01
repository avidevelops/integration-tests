package in.av.qe.auth;

import lombok.Getter;

import java.time.Instant;

/**
 * Immutable value object representing a bearer token with expiry awareness.
 *
 * <p>Wraps a raw token string alongside its expiry timestamp. All token consumers
 * use {@link #isExpired()} before using the value, ensuring the framework never
 * silently sends a stale credential.
 * The {@link #bearerHeader()} convenience method formats the
 * token for direct use as an HTTP Authorization header value.
 */
public class AuthToken {
    @Getter
    private final String value;
    private final Instant expiresAt;

    /**
     * Creates an AuthToken that expires after the given number of seconds.
     *
     * @param value           the raw token string (JWT or opaque bearer value)
     * @param expiresInSeconds number of seconds until the token expires,
     *                         as reported by the issuer (e.g. expires_in field).
     *                         The expiresAt instant is computed from Instant.now()
     *                         at construction time.
     */
    public AuthToken(String value, long expiresInSeconds) {
        this.value = value;
        this.expiresAt = Instant.now().plusSeconds(expiresInSeconds);
    }

    /**
     * Returns true if the token has passed its expiry instant.
     *
     * <p>Called by {@link TokenCache#get(String)} before returning a cached token.
     * If true, the caller must discard the cached entry and re-authenticate.
     *
     * @return true if Instant.now() is after expiresAt, false otherwise
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Returns the token formatted as an HTTP Authorization header value.
     *
     * <p>Example output: {@code "Bearer eyJ0eXAiOiJKV1Qi..."}
     * <p>Use directly as the value of the {@code Authorization} header:
     * <pre>
     *   headers.put("Authorization", token.bearerHeader());
     * </pre>
     *
     * @return "Bearer " + value
     */
    public String bearerHeader() {
        return "Bearer " + value;
    }

    @Override
    public String toString() {
        return "AuthToken[expires=" + expiresAt + "]";
    }

}
