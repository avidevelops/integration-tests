package in.av.qe.utils;

import java.io.IOException;
import java.util.Properties;

public class PropertyLoader {
    private static final String PROP_FILE = "/application.properties";

    private PropertyLoader() {}

    public static String loadProperty(String name) {
        Properties properties = new Properties();
        try {
            properties.load(PropertyLoader.class.getResourceAsStream(PROP_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String value = "";
        if (name != null)
            value = properties.getProperty(name);
        return value;
    }
}
