package in.av.qe.user;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import in.av.qe.api.MicroService;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static in.av.qe.utils.InfrastructureConstants.*;

public class QaUser {

    private final Playwright playwright;
    private final Map<String, String> commonHeaders;

    private QaUser(@Nonnull String login, @Nonnull String password) {
        Map<String, String> env = new HashMap<>();
        env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");

        this.playwright = Playwright.create(
                new Playwright.CreateOptions().setEnv(env)
        );
        this.commonHeaders = new HashMap<>();
        commonHeaders.put("Accept", "application/json");
        commonHeaders.put("Content-Type", "application/json");
        commonHeaders.put("Authorization", basicAuthHeader(login, password));
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
        T ms;
        try {
            ms = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new AssertionError(e.getMessage(), e);
        }
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
}
