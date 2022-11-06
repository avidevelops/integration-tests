package in.av.qe.utils;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import in.av.qe.helpers.QaLogger;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.oneOf;

public class ITRestAssured {

    private static Logger logger = QaLogger.getQaLogger();
    public static final String jsonAcceptType = "application/json";
    public static boolean isDebug = false;

    public static <T> T extractObject(Class<T> type, ValidatableResponse vr) {
        return extractObject(type, vr.extract().response().asString());
    }

    public static <T> T extractObject(Class<T> type, String body) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(body, type);
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

    public static ValidatableResponse readByGet(RequestSpecification rs, String url, int statusCode,
                                                @Nullable ContentType contentType, @Nullable String acceptValue) {
        if (acceptValue != null)
            rs.accept(acceptValue);
        else
            rs.accept(JSON);

        if (contentType != null)
            rs.contentType(contentType);
        else
            rs.contentType("");

        ValidatableResponse vr = rs.get(url).then().log().ifValidationFails();
        if (vr.extract().statusCode() == 401) {
            logger.error("[AUTHORIZATION FAILED, IMMEDIATELY EXIT TESTS]]");
            System.exit(401);
        }
        return vr.statusCode(statusCode);
    }

    public static ValidatableResponse getWithExpectingAuthorizationFailed(RequestSpecification rs, String url) {
        return rs.get(url).then().statusCode(403).log().ifValidationFails();
    }

    public static Pair<Integer, Long> getResponseCodeAndTime(RequestSpecification rs, String url) {
        rs.accept(JSON).contentType(JSON);
        Response response = rs.get(url);
        return Pair.of(response.getStatusCode(), response.getTime());
    }

    public static ValidatableResponse createByPost(RequestSpecification rs, String url, Object value,
                                                   @Nullable ContentType contentType, Integer... statusCodes) {
        ValidatableResponse vr = rs.contentType(JSON)
                .body(value).accept(JSON)
                .post(url).then().log().ifValidationFails()
                .body(notNullValue());

        if (contentType != null) {
            vr = vr.contentType(contentType);
        }
        if (vr.extract().statusCode() == 401) {
            logger.error("[AUTHORIZATION FAILED, IMMEDIATELY EXIT TESTS]]");
            System.exit(401);
        }
        if (statusCodes != null && statusCodes.length > 0) {
            vr.statusCode(oneOf(statusCodes));
        }
        return vr;
    }

    public static ValidatableResponse postWithExpectingAuthorizationFailed(RequestSpecification rs, String url, Object value) {
        return rs.body(value).post(url).then().statusCode(403).log().ifValidationFails();
    }

    public static ValidatableResponse postWithUploadExpectingJson(RequestSpecification rs, String url, String filePath) {
        return rs.contentType(ContentType.MULTIPART).multiPart(new File(filePath))
                .post(url).then().statusCode(200).log().ifValidationFails().contentType(JSON);
    }

    public static ValidatableResponse updateByPut(RequestSpecification rs, String url, Object value,
                                                  ContentType requestType, @Nullable ContentType responseType, Integer... statusCodes) {
        ValidatableResponse vr = rs.contentType(requestType)
                .body(value).put(url).then().log().ifValidationFails();

        if (responseType != null) {
            vr = vr.contentType(responseType);
        }
        if (vr.extract().statusCode() == 401) {
            logger.error("[AUTHORIZATION FAILED, IMMEDIATELY EXIT TESTS]]");
            System.exit(401);
        }
        if (statusCodes != null && statusCodes.length > 0) {
            vr.statusCode(oneOf(statusCodes));
        }
        return vr;
    }

    public static ValidatableResponse deleteByDelete(RequestSpecification rs, String url, Object value, Integer... statusCodes) {
        ValidatableResponse vr = rs.contentType(JSON)
                .body(value).delete(url).then().log().ifValidationFails().body(notNullValue());

        if (vr.extract().statusCode() == 401) {
            logger.error("[AUTHORIZATION FAILED, IMMEDIATELY EXIT TESTS]]");
            System.exit(401);
        }
        if (statusCodes != null && statusCodes.length > 0) {
            vr.statusCode(oneOf(statusCodes));
        }
        return vr;
    }

    public static ValidatableResponse updateByPatch(RequestSpecification rs, String url, Object value, Integer statusCodes) {
        ValidatableResponse vr = rs.contentType(JSON)
                .body(value).accept(JSON).patch(url).then().log().ifValidationFails().body(notNullValue()).statusCode(statusCodes);

        if (vr.extract().statusCode() == 401) {
            logger.error("[AUTHORIZATION FAILED, IMMEDIATELY EXIT TESTS]]");
            System.exit(401);
        }

        return vr;
    }

}
