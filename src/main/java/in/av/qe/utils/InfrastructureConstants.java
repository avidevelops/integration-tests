package in.av.qe.utils;

public class InfrastructureConstants {

    public static final String baseUrlPart;
    public static final String user;
    public static final String password;
    public static final String userId;

    // MS OAuth Implicit flow URL (full authorize URL with client_id, scope, redirect_uri)
    public static final String msAuthUrl;

    // Internal SSO base URL
    public static final String ssoUrl;

    static {
        baseUrlPart = getProperty("qa.baseUrl");
        user        = getProperty("qa.user");
        password    = getProperty("qa.password");
        userId      = getProperty("qa.userId");
        msAuthUrl   = getProperty("qa.auth.msAuthUrl");
        ssoUrl      = getProperty("qa.auth.ssoUrl");
    }

    private static String getProperty(String name) {
        return PropertyLoader.loadProperty(name);
    }
}
