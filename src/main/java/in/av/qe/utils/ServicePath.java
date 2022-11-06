package in.av.qe.utils;

public enum ServicePath {

    BOOKS("/BookStore/v1");

    private final String appPrefix;

    ServicePath(final String appPrefix) { this.appPrefix = appPrefix; }

    public String getAppPrefix() {
        return appPrefix;
    }
}
