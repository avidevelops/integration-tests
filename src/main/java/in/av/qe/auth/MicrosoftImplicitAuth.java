package in.av.qe.auth;

import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static in.av.qe.utils.InfrastructureConstants.msAuthUrl;

/**
 * Automates Microsoft OAuth 2.0 Implicit Grant flow using a headless Chromium browser.
 * <p>
 * This is the only class in the framework that requires a real browser. It exists
 * because the OAuth Implicit Flow delivers its token in the URL fragment
 * (#access_token=...) which is a client-side-only construct — it is never sent
 * over the network and cannot be captured by a pure HTTP client.
 *
 * <p>
 * How it works:
 * </p>
 * <ol>
 *     <li>Launches a headless Chromium browser via the shared
 *     {@code Playwright} instance.</li>
 *     <li>Registers a {@code context.route()} intercept on
 *     {@code localhost:3000/**} that responds with a minimal HTML page
 *     ({@code route.fulfill()}), allowing the browser to "load" the redirect
 *     destination even though no real server exists there.</li>
 *     <li>Navigates to the Microsoft authorization URL.</li>
 *     <li>If a login form appears (i.e., SSO silent resolution did not
 *     occur), fills email and password and handles the "Stay signed in?"
 *     prompt.</li>
 *     <li>Waits for the browser to land on {@code localhost:3000} via
 *     {@code page.waitForURL()}.</li>
 *     <li>Reads {@code window.location.hash} via {@code page.evaluate()} —
 *     this is where Microsoft places the {@code access_token} after the redirect.</li>
 *     <li>Parses the {@code access_token} and {@code expires_in} values from
 *     the fragment.</li>
 *     <li>Closes the browser context immediately.</li>
 * </ol>
 *
 * <p>
 * Why {@code route.fulfill()} instead of a real server:
 * </p>
 * The redirect URI {@code (localhost:3000/sso)} must be a registered URI in
 * Azure AD. Since no actual server runs at that address during tests, the
 * browser would normally land on a {@code chrome-error://chromeWebData}
 * page before the fragment can be read. {@code route.fulfill()} intercepts
 * the request at the network layer and returns a synthetic 200 OK response,
 * allowing the page to "load" and making {@code window.location.hash}
 * accessible via {@code page.evaluate()}.
 *
 * <p>
 * Thread safety: Each call to {@code acquireAuthToken()} opens and closes
 * its own {@code Browser} and {@code BrowserContext}. The {@code Playwright}
 * instance is shared but Playwright itself is safe for concurrent browser
 * launches.
 * </p>
 */

@Slf4j
public class MicrosoftImplicitAuth {
    // Fragment pattern: access_token=<value>&...
    private static final Pattern ACCESS_TOKEN_PATTERN =
            Pattern.compile("[#&]access_token=([^&]+)");

    // expires_in pattern from fragment
    private static final Pattern EXPIRES_IN_PATTERN =
            Pattern.compile("[#&]expires_in=([^&]+)");

    private final Playwright playwright;

    /**
     * Creates a new MicrosoftImplicitAuth bound to the given Playwright instance.
     *
     * <p>The Playwright instance is used only to launch a Chromium browser.
     * It is not closed by this class — the caller retains ownership of its lifecycle.
     *
     * @param playwright a live Playwright instance; must not be null or closed
     */
    public MicrosoftImplicitAuth(Playwright playwright) {
        this.playwright = playwright;
    }

    /**
     * Launches a headless browser, navigates the implicit flow login,
     * waits for redirect to localhost with token in fragment,
     * then returns the raw Microsoft access_token.
     */
    public AuthToken acquireAuthToken(String userName, String password) {
        log.info("[MS AUTH] Starting implicit grant flow for user: {}", userName);

        // Use chromium — must be installed for browser-based auth
        // This is the ONE place a real browser is needed in this framework

        try (Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
        ); BrowserContext context = browser.newContext(
                new Browser.NewContextOptions().setIgnoreHTTPSErrors(true)
        )) {
            Page page = context.newPage();

            // Navigate to Microsoft authorization endpoint
            page.navigate(msAuthUrl);

            // If login page is shown (not SSO-silently resolved), fill credentials
            if (page.url().contains("login.microsoftonline.com") && page.locator("input[type='email']").isVisible()) {
                log.info("[MS AUTH] Login page detected — entering credentials.");

                page.fill("input[type='email']", userName);
                page.click("input[type='submit']");

                page.waitForSelector("input[type='password']");
                page.fill("input[type='password']", password);
                page.click("input[type='submit']");

                // "Stay signed in?" prompt — click No to keep it stateless
                try {
                    page.waitForSelector("#idBtn_Back", new Page.WaitForSelectorOptions()
                            .setTimeout(3000));
                    page.click("#idBtn_Back");
                } catch (TimeoutError ignored) {
                    // Prompt not shown — continue
                }
            }

            // Wait for redirect to localhost:3000 with token in URL
            page.waitForURL("**/localhost:3000/**",
                    new Page.WaitForURLOptions().setTimeout(30_000));

            String redirectUrl = page.url();
            log.info("[MS AUTH] Redirect captured. Extracting token from fragment.");

            return extractTokenFromFragment(redirectUrl);

        }
    }

    private AuthToken extractTokenFromFragment(String url) {
        // Fragment comes after # — decode URL-encoded chars
        String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);

        Matcher tokenMatcher = ACCESS_TOKEN_PATTERN.matcher(decoded);
        if (!tokenMatcher.find()) {
            throw new AuthException(
                    "access_token not found in redirect URL. URL: " + maskToken(decoded)
            );
        }
        String accessToken = tokenMatcher.group(1);

        // Parse expires_in — default to 3600 if not present
        long expiresIn = 3600L;
        Matcher expiresMatcher = EXPIRES_IN_PATTERN.matcher(decoded);
        if (expiresMatcher.find()) {
            expiresIn = Long.parseLong(expiresMatcher.group(1));
        }

        log.info("[MS AUTH] Token extracted successfully. Expires in {}s.", expiresIn);
        return new AuthToken(accessToken, expiresIn);
    }

    // Mask token in logs for security
    private String maskToken(String url) {
        return url.replaceAll("access_token=[^&]+", "access_token=***MASKED***");
    }

    public static class AuthException extends RuntimeException {
        public AuthException(String message) { super(message); }
        public AuthException(String message, Throwable cause) { super(message, cause); }
    }
}
