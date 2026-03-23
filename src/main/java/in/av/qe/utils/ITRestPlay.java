package in.av.qe.utils;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import in.av.qe.helpers.QaLogger;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;

public class ITRestPlay {

    private static Logger logger = QaLogger.getQaLogger();
    private static final Gson GSON = new Gson();
    public static final String JSON_ACCEPT_TYPE = "application/json";
    public static boolean isDebug = false;

    public static <T> T extractObject(Class<T> type, APIResponse response) {
        return extractObject(type, response.text());
    }

    public static <T> T extractObject(Class<T> type, String body) {
        try {
            return GSON.fromJson(body, type);
        } catch (JsonParseException e) {
            e.printStackTrace();
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException |
                     InvocationTargetException | InstantiationException e1) {
                e1.printStackTrace();
                return null;
            }
        }
    }

    public static APIResponse readByGet(APIRequestContext requestContext, String url, int statusCode,
                                        @Nullable String contentType, @Nullable String acceptValue) {
        RequestOptions requestOptions = RequestOptions.create();
        if (acceptValue != null) {
            requestOptions.setHeader("Accept", acceptValue);
        } else {
            requestOptions.setHeader("Accept", JSON_ACCEPT_TYPE);
        }

        if (contentType != null)
            requestOptions.setHeader("content-type", contentType);
        else
            requestOptions.setHeader("content-type", "");

        APIResponse response = requestContext.get(url, requestOptions);
        return validate(response, statusCode, contentType);
    }

    public static APIResponse getWithExpectingAuthorizationFailed(APIRequestContext requestContext, String url) {
        APIResponse response = requestContext.get(url);
        return validate(response, 403, null);
    }

    public static Pair<Integer, Long> getResponseCodeAndTime(APIRequestContext apiRequestContext, String url) {
        long start = System.currentTimeMillis();
        APIResponse response = apiRequestContext.get(url, RequestOptions.create().setHeader("accept", JSON_ACCEPT_TYPE));
        long end = System.currentTimeMillis();
        return Pair.of(response.status(), end - start);
    }

    public static APIResponse createByPost(APIRequestContext requestContext, String url, Object value,
                                           @Nullable String contentType, Integer... statusCodes) {
        APIResponse response = requestContext.post(url, RequestOptions.create().setHeader("accept", JSON_ACCEPT_TYPE).setData(value));
        return validate(response, contentType, statusCodes);
    }

    public static APIResponse postWithExpectingAuthorizationFailed(APIRequestContext requestContext, String url, Object value) {
        APIResponse response = requestContext.post(url, RequestOptions.create().setData(value));
        return validate(response, 403, null);
    }

    public static APIResponse postWithUploadExpectingJson(APIRequestContext requestContext, String url, String filePath) {
        APIResponse response = requestContext.post(url, RequestOptions.create().setMultipart(
                FormData.create().set("file", Path.of(filePath))
        ));
        return validate(response, 200, JSON_ACCEPT_TYPE);
    }

    public static APIResponse updateByPut(APIRequestContext requestContext, String url, Object value,
                                          @Nullable String requestType, @Nullable String responseType, Integer... statusCodes) {
        RequestOptions options = RequestOptions.create().setData(value);
        if (requestType != null) {
            options.setHeader("Content-Type", requestType);
        }
        APIResponse response = requestContext.put(url, options);
        return validate(response, responseType, statusCodes);
    }

    public static APIResponse deleteByDelete(com.microsoft.playwright.APIRequestContext requestContext,
                                             String url,
                                             Object value,
                                             Integer... statusCodes) {
        APIResponse response = requestContext.delete(url,
                RequestOptions.create().setData(value));
        return validate(response, null, statusCodes);
    }

    public static APIResponse updateByPatch(com.microsoft.playwright.APIRequestContext requestContext,
                                            String url,
                                            Object value,
                                            Integer statusCode) {
        APIResponse response = requestContext.patch(url,
                RequestOptions.create()
                        .setHeader("Accept", JSON_ACCEPT_TYPE)
                        .setData(value));
        return validate(response, statusCode, null);
    }

    private static APIResponse validate(APIResponse response, @Nullable String expectedContentType, Integer... expectedStatusCodes) {
        if (isDebug) {
            logger.info("Response status: {}", response.status());
            logger.info("Response body: {}", response.body());
        }

        if (response.status() == 401) {
            logger.error("[AUTHORIZATION FAILED, IMMEDIATELY EXIT TESTS]");
            System.exit(401);
        }

        if (expectedStatusCodes != null && expectedStatusCodes.length > 0) {
            boolean matches = Arrays.stream(expectedStatusCodes).anyMatch(code -> code == response.status());
            if (!matches) {
                throw new AssertionError(
                        String.format("Expected status %s but got %s. Body: %s",
                                Arrays.toString(expectedStatusCodes), response.status(), response.text())
                );
            }
        }

        if (expectedContentType != null) {
            String actual = response.headers().getOrDefault("content-type", "");
            if (!actual.toLowerCase().contains(expectedContentType.toLowerCase())) {
                throw new AssertionError(
                        String.format("Expected content type containing '%s' but got '%s'",
                                expectedContentType, actual));
            }
        }

        return response;
    }

    private static APIResponse validate(APIResponse response, int statusCode, @Nullable String expectedContentType) {
        return validate(response, expectedContentType, statusCode);
    }
}
