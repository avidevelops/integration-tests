package in.av.qe.auth;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TokenCache {
    private static final ConcurrentHashMap<String, AuthToken> CACHE = new ConcurrentHashMap<>();

    private TokenCache() {}

    public static AuthToken get(String userKey) {
        AuthToken authToken = CACHE.get(userKey);
        if ( authToken!= null && !authToken.isExpired()) {
            log.debug("[TOKEN CACHE] Cache hit for user: {}", userKey);
            return authToken;
        }
        log.info("[TOKEN CACHE] Cache miss for user: {}. Re-authneticating.", userKey);
        return null;
    }

    public static void put(String userKey, AuthToken authToken) {
        CACHE.put(userKey, authToken);
        log.info("[TOKEN CACHE] Token cached for user {} | {} ", userKey, authToken);
    }

    public static void invalidate(String userKey) {
        CACHE.remove(userKey);
        log.info("[TOKEN CACHE] Token invalidated for user: {}", userKey);
    }

    public static void invalidateAll() {
        CACHE.clear();
        log.info("[TOKEN CACHE] all tokens cleared");
    }
}
