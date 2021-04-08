package org.worker.replicaspring.ademo;

import org.worker.replicaspring.annotation.HnService;

@HnService
public class DemoServiceImpl implements DemoService {
    @Override
    public void test() {
        System.out.println("test aop");
    }
}
