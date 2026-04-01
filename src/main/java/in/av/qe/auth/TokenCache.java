package in.av.qe.auth;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, in-memory token cache keyed by user identity.
 *
 * <p>Prevents redundant authentication flows when multiple tests run for the same
 * user within a single test suite execution. Backed by a {@code ConcurrentHashMap}
 * so it is safe to call from parallel test threads without external synchronization.
 *
 * <p>The cache is keyed by the user's login (typically an email address). A cached
 * entry is considered valid only if {@code AuthToken.isExpired()} returns {@code false}
 * at the time of retrieval. Expired entries are treated as cache misses.
 */
@Slf4j
public class TokenCache {
    private static final ConcurrentHashMap<String, AuthToken> CACHE = new ConcurrentHashMap<>();

    private TokenCache() {}

    /**
     * Returns a valid cached token for the given user key, or null if none exists.
     *
     * <p>Returns null in two cases:
     * <ul>
     *   <li>No token has been cached for this user key yet.</li>
     *   <li>A token exists but {@link AuthToken#isExpired()} returns true.</li>
     * </ul>
     *
     * <p>Callers (typically {@link in.av.qe.user.QaUser}) must trigger a full
     * authentication flow when this method returns null.
     *
     * @param userKey unique user identifier — typically the login/email string
     * @return a non-expired AuthToken, or null on cache miss or expiry
     */
    public static AuthToken get(String userKey) {
        AuthToken authToken = CACHE.get(userKey);
        if ( authToken!= null && !authToken.isExpired()) {
            log.debug("[TOKEN CACHE] Cache hit for user: {}", userKey);
            return authToken;
        }
        log.info("[TOKEN CACHE] Cache miss for user: {}. Re-authneticating.", userKey);
        return null;
    }

    /**
     * Stores a token in the cache under the given user key.
     *
     * <p>Replaces any existing entry for the same key. Called after a successful
     * authentication flow to persist the freshly acquired token for reuse.
     *
     * @param userKey   unique user identifier matching the key used with {@link #get}
     * @param authToken the freshly acquired token to cache
     */
    public static void put(String userKey, AuthToken authToken) {
        CACHE.put(userKey, authToken);
        log.info("[TOKEN CACHE] Token cached for user {} | {} ", userKey, authToken);
    }

    /**
     * Removes the cached token for a specific user.
     *
     * <p>Forces the next call to {@link #get} for this user to miss, triggering
     * a full re-authentication. Useful in tests that deliberately test token
     * expiry or session invalidation scenarios.
     *
     * @param userKey the user key whose token should be evicted
     */
    public static void invalidate(String userKey) {
        CACHE.remove(userKey);
        log.info("[TOKEN CACHE] Token invalidated for user: {}", userKey);
    }

    /**
     * Clears all cached tokens.
     *
     * <p>Intended for use in test teardown or before a full suite re-run where
     * a clean authentication state is required for all users.
     */
    public static void invalidateAll() {
        CACHE.clear();
        log.info("[TOKEN CACHE] all tokens cleared");
    }
}
