package org.worker.replicaspring.beans;

public class HnBeanWrapper {
    private Object wrapperInstance;
    private Class<?> wrapperClass;

    public HnBeanWrapper(Object wrapperInstance) {
        this.wrapperInstance = wrapperInstance;
        this.wrapperClass = this.wrapperInstance.getClass();
    }

    public Object getWrapperInstance() {
        return wrapperInstance;
    }

    public Class<?> getWrapperClass() {
        return wrapperClass;
    }
}
