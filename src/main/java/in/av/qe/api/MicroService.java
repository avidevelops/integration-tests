package in.av.qe.api;

import com.microsoft.playwright.APIRequestContext;
import in.av.qe.utils.ServicePath;

import static in.av.qe.utils.ITRestPlay.isDebug;

public abstract class MicroService {

    protected APIRequestContext requestContext;

    protected ServicePath servicePath;

    public MicroService(ServicePath servicePath) { this.servicePath = servicePath; }

    public void setRequestContext(APIRequestContext requestContext) {
        this.requestContext = requestContext;
        if (isDebug) {
            System.out.printf("Initialized APIRequestContext for base path: %s%n", appPrefix());
        }
    }

    public String appPrefix() { return  this.servicePath.getAppPrefix(); }

    public APIRequestContext getRequestSpecification() {
        return this.requestContext;
    }
}
