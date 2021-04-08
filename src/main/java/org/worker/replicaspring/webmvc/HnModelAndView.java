package org.worker.replicaspring.webmvc;

import java.util.HashMap;
import java.util.Map;

public class HnModelAndView {
    private String view;
    private Map<String, ?> model = new HashMap<>();

    public HnModelAndView(String view) {
        this.view = view;
    }

    public HnModelAndView(String view, Map<String, ?> model) {
        this.view = view;
        this.model = model;
    }

    public Map<String, ?> getModel() {
        return model;
    }

    public String getView() {
        return view;
    }
}
