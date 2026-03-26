package in.av.qe.auth;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static in.av.qe.utils.InfrastructureConstants.ssoUrl;

/**
 * Exchanges a Microsoft access_token for your internal SSO custom token.
 * <p>
 * POST {}baseUrl}/sso/authorize
 * Content-Type: application/x-www-form-urlencoded
 * Body: access_token=<ms_token>
 * <p>
 * Returns the custom Bearer token to use in all API calls.
 */
@Slf4j
public class SsoTokenExchanger {
    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("\"(?:token|access_token|customToken)\"\\s*:\\s*\"([^\"]+)\"");

    // Default expiry if SSO doesn't tell us — 1 hour
    private static final long DEFAULT_EXPIRY_SECONDS = 3600L;

    private final APIRequest apiRequest;

    public SsoTokenExchanger(APIRequest apiRequest) {
        this.apiRequest = apiRequest;
    }

    /**
     * Posts MS token to internal SSO, returns the custom token.
     */
    public AuthToken exchange(AuthToken msToken) {
        log.info("[SSO] Exchanging MS token for internal SSO token at: {}", ssoUrl);

        APIRequestContext context = apiRequest.newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL(ssoUrl)
                        .setExtraHTTPHeaders(Map.of("Accept", "application/json"))
        );

        // Form-encoded body: access_token=<value>
        String formBody = "access_token=" + encode(msToken.getValue());

        APIResponse response = context.post("/authorize",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/x-www-form-urlencoded")
                        .setData(formBody)
        );

        if (response.status() != 200) {
            throw new MicrosoftImplicitAuth.AuthException(
                    "SSO token exchange failed. Status: " + response.status() +
                            " Body: " + response.text()
            );
        }

        String body = response.text();
        log.info("[SSO] Exchange successful.");
        return parseCustomToken(body);
    }

    private AuthToken parseCustomToken(String responseBody) {
        Matcher matcher = TOKEN_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            throw new MicrosoftImplicitAuth.AuthException(
                    "Custom token not found in SSO response. Body: " + responseBody
            );
        }
        String customToken = matcher.group(1);

        // If your SSO response also returns expires_in, parse it here
        // For now using default
        return new AuthToken(customToken, DEFAULT_EXPIRY_SECONDS);
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
