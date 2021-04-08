package org.worker.replicaspring.ademo;

import org.worker.replicaspring.annotation.HnAutowired;
import org.worker.replicaspring.annotation.HnController;
import org.worker.replicaspring.annotation.HnRequestMapping;
import org.worker.replicaspring.annotation.HnRequestParam;
import org.worker.replicaspring.webmvc.HnModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@HnController
public class DemoController {

    @HnAutowired
    private DemoService demoService;

    @HnRequestMapping("/first.html")
    public HnModelAndView hello(@HnRequestParam("teacher") String teacher) {
        Map<String,Object> model = new HashMap<String,Object>();
        model.put("teacher", teacher);
        model.put("data", "data");
        model.put("token", "123456");
        demoService.test();
        return new HnModelAndView("first.html",model);
    }
}
