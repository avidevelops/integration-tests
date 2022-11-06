package in.av.qe.utils;

public class InfrastructureConstants {
    public static final String baseUrlPart;
    public static final String user;
    public static final String password;
    public static final String userId;

    static {
        baseUrlPart = "https://demoqa.com";
        user = getProperty("qa.user");
        password = getProperty("qa.password");
        userId = getProperty("qa.userId");
    }

    private static String getProperty(String name) {
        return PropertyLoader.loadProperty(name);
    }
}
