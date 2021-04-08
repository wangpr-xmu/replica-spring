package org.worker.replicaspring.webmvc;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class HnHandlerMapping {
    private Pattern urlPattern;
    private Method method;
    private Object controller;

    public HnHandlerMapping(Pattern urlPattern, Method method, Object controller) {
        this.urlPattern = urlPattern;
        this.method = method;
        this.controller = controller;
    }

    public Pattern getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(Pattern urlPattern) {
        this.urlPattern = urlPattern;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }
}
