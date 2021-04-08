package org.worker.replicaspring.webmvc;

import org.worker.replicaspring.annotation.HnRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class HnHandlerAdapter {
    public HnModelAndView handle(HttpServletRequest request, HttpServletResponse response, HnHandlerMapping handlerMapping) throws InvocationTargetException, IllegalAccessException {
        //保存形参列表
        Map<String, Integer> paramIndexMapping = new HashMap<>();
        //通过运行时的状态去拿
        Annotation[][] pa = handlerMapping.getMethod().getParameterAnnotations();

        for(int i = 0; i < pa.length; i++) {
            for(Annotation an : pa[i]) {
                if(an instanceof HnRequestParam){
                    String paramName = ((HnRequestParam) an).value();
                    if(!"".equals(paramName.trim())){
                        paramIndexMapping.put(paramName,i);
                    }
                }
            }
        }


        //初始化一下
        Class<?> [] paramTypes = handlerMapping.getMethod().getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramterType = paramTypes[i];
            if(paramterType == HttpServletRequest.class || paramterType == HttpServletResponse.class){
                paramIndexMapping.put(paramterType.getName(),i);
            }
        }

        //去拼接实参列表
        Map<String,String[]> params = request.getParameterMap();

        Object [] paramValues = new Object[paramTypes.length];

        for (Map.Entry<String,String[]> param : params.entrySet()) {
            String value = Arrays.toString(params.get(param.getKey()))
                    .replaceAll("\\[|\\]","")
                    .replaceAll("\\s+",",");

            if(!paramIndexMapping.containsKey(param.getKey())){continue;}

            int index = paramIndexMapping.get(param.getKey());

            //允许自定义的类型转换器Converter
            paramValues[index] = castStringValue(value,paramTypes[index]);
        }

        if(paramIndexMapping.containsKey(HttpServletRequest.class.getName())){
            int index = paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[index] = request;
        }

        if(paramIndexMapping.containsKey(HttpServletResponse.class.getName())){
            int index = paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[index] = response;
        }

        Object result = handlerMapping.getMethod().invoke(handlerMapping.getController(),paramValues);
        if(result == null || result instanceof Void){return null;}

        boolean isModelAndView = handlerMapping.getMethod().getReturnType() == HnModelAndView.class;
        if(isModelAndView){
            return (HnModelAndView) result;
        }

        return null;
    }


    private Object castStringValue(String value, Class<?> paramType) {
        if(String.class == paramType){
            return value;
        }else if(Integer.class == paramType){
            return Integer.valueOf(value);
        }else if(Double.class == paramType){
            return Double.valueOf(value);
        }else {
            if(value != null){
                return value;
            }
            return null;
        }

    }

}
