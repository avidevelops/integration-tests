package in.av.qe.api;

import in.av.qe.utils.ServicePath;
import io.restassured.specification.RequestSpecification;

import static in.av.qe.utils.ITRestAssured.isDebug;

public abstract class MicroService {

    protected RequestSpecification rs;

    protected ServicePath servicePath;

    public MicroService(ServicePath servicePath) { this.servicePath = servicePath; }

    public void setRequestSpecification(RequestSpecification rs) {
        this.rs = rs.log().method().and().log().uri();
        if (isDebug) {
            this.rs.log().all();
        }
    }

    public String appPrefix() { return  this.servicePath.getAppPrefix(); }

    public RequestSpecification getRequestSpecification() {
        return this.rs;
    }
}
