package in.av.qe.user;

import in.av.qe.api.MicroService;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.parsing.Parser;
import io.restassured.specification.RequestSpecification;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;

import static in.av.qe.utils.InfrastructureConstants.*;
import static io.restassured.RestAssured.given;

public class QaUser {

    protected RequestSpecification rs;

    private QaUser(@Nonnull String login, @Nonnull String password) {
        RestAssured.reset();
        RestAssured.defaultParser = Parser.JSON;
        RestAssured.config = RestAssured.config().objectMapperConfig((new ObjectMapperConfig(ObjectMapperType.GSON)));
        this.rs = given().auth().preemptive().basic(login, password)
                .accept(ContentType.JSON).contentType(ContentType.JSON);
    }

    public QaUser() { testUser(); }

    public static QaUser testUser() {
        return new QaUser(user,password);
    }

    public QaUser calls() {
        this.rs.baseUri(baseUrlPart);
        return this;
    }

    public <T extends MicroService> T service(Class<T> clazz) {
        T ms;
        try {
            ms = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new AssertionError(e.getMessage(), e);
        }
        this.rs.basePath(ms.appPrefix());
        ms.setRequestSpecification(this.rs);
        return ms;
    }
}
