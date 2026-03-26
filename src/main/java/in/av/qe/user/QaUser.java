package in.av.qe.user;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import in.av.qe.api.MicroService;
import in.av.qe.auth.AuthToken;
import in.av.qe.auth.MicrosoftImplicitAuth;
import in.av.qe.auth.SsoTokenExchanger;
import in.av.qe.auth.TokenCache;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static in.av.qe.utils.InfrastructureConstants.*;

public class QaUser {

    private final String userKey;    // cache key — typically username
    private final Playwright playwright;
    private final Map<String, String> commonHeaders;

    private QaUser(@Nonnull String login, @Nonnull String password) {
        this.userKey = login;

        Map<String, String> env = new HashMap<>();
//        env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");

        this.playwright = Playwright.create(
                new Playwright.CreateOptions().setEnv(env)
        );

        // Resolve the custom SSO token (cached or fresh)
        AuthToken customToken = resolveToken();

        this.commonHeaders = new HashMap<>();
        commonHeaders.put("Accept", "application/json");
        commonHeaders.put("Content-Type", "application/json");
        commonHeaders.put("Authorization", customToken.bearerHeader());
    }

    public QaUser() {
        this(user, password);
    }

    public static QaUser testUser() {
        return new QaUser(user, password);
    }

    public QaUser calls() {
        return this;
    }

    public <T extends MicroService> T service(Class<T> clazz) {
        T ms = instantiate(clazz);
        String serviceBaseUrl = baseUrlPart + ms.appPrefix();

        APIRequestContext requestContext = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL(serviceBaseUrl)
                        .setExtraHTTPHeaders(commonHeaders)
        );

        ms.setRequestContext(requestContext);
        return ms;
    }

    private static String basicAuthHeader(String login, String password) {
        String value = login + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    // ----------------------------------------------------------------
    // Auth chain: cache → MS implicit → SSO exchange
    // ----------------------------------------------------------------

    private AuthToken resolveToken() {
        // 1. Check cache — avoids re-auth on every service() call
        AuthToken cached = TokenCache.get(userKey);
        if (cached != null) return cached;

        // 2. Step 1: MS Implicit flow (needs a browser)
        MicrosoftImplicitAuth msAuth = new MicrosoftImplicitAuth(playwright);
        AuthToken msToken = msAuth.acquireAuthToken(user, password);

        // 3. Step 2: SSO exchange (pure HTTP)
        SsoTokenExchanger exchanger = new SsoTokenExchanger(playwright.request());
        AuthToken customToken = exchanger.exchange(msToken);

        // 4. Cache the custom token for subsequent calls
        TokenCache.put(userKey, customToken);
        return customToken;
    }

    private <T extends MicroService> T instantiate(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
            throw new AssertionError(e.getMessage(), e);
        }
    }
}
