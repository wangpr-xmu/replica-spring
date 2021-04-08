package org.worker.replicaspring.webmvc;

import org.worker.replicaspring.ademo.DemoService;
import org.worker.replicaspring.annotation.*;
import org.worker.replicaspring.context.ApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HnDispatchServlet extends HttpServlet {
    private ApplicationContext applicationContext;
    private Map<String, Method> handlerMapping = new HashMap<>();
    private List<HnHandlerMapping> handlerMappings = new ArrayList<>();
    private Map<HnHandlerMapping, HnHandlerAdapter> handlerAdapterMap = new HashMap<>();
    private List<HnViewResolver> viewResolvers = new ArrayList<>();
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //委派模式
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            try {
                processDispatch(req,resp,new HnModelAndView("500"));
            } catch (Exception e1) {
                e1.printStackTrace();
                resp.getWriter().write("500 Exception,Detail : " + Arrays.toString(e.getStackTrace()));
            }
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        //1. 获取handlerMapping
        HnHandlerMapping handlerMapping = getHandlerMapping(req);

        if(handlerMapping == null) {
            processDispatch(req,resp,new HnModelAndView("404"));
            return;
        }

        //2. 获取handlerAdapter
        HnHandlerAdapter handlerAdapter = getHandlerAdapter(handlerMapping);

        //3. 封装ModelAndView
        HnModelAndView modelAndView = handlerAdapter.handle(req, resp, handlerMapping);

        //4. resolve ModelAndView

        processDispatch(req, resp, modelAndView);
    }

    private void processDispatch(HttpServletRequest req, HttpServletResponse resp, HnModelAndView modelAndView) throws Exception {
        if(null == modelAndView){return;}
        if(this.viewResolvers.isEmpty()){return;}

        for (HnViewResolver viewResolver : this.viewResolvers) {
            HnView view = viewResolver.resolveViewName(modelAndView.getView());
            //直接往浏览器输出
            view.render(modelAndView.getModel(),req,resp);
            return;
        }
    }

    private HnHandlerAdapter getHandlerAdapter(HnHandlerMapping handlerMapping) {
        return this.handlerAdapterMap.get(handlerMapping);
    }

    private HnHandlerMapping getHandlerMapping(HttpServletRequest req) {
        if(this.handlerMappings.isEmpty()){return  null;}
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");

        for (HnHandlerMapping mapping : handlerMappings) {
            Matcher matcher = mapping.getUrlPattern().matcher(url);
            if(!matcher.matches()){continue;}
            return mapping;
        }
        return null;

    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        //初始化spring IoC容器
        applicationContext = new ApplicationContext(config.getInitParameter("contextConfigLocation"));
        System.out.println("===========================hello");
        //初始化spring 九大组件
        initStrategies(applicationContext);
//        initHandlerMapping();

        System.out.println(applicationContext.getBean(DemoService.class));
        System.out.println(applicationContext.getBean(DemoService.class));
        System.out.println("=============Spring init finish==============");
    }

    private void initStrategies(ApplicationContext context) {
//        //多文件上传的组件
//        initMultipartResolver(context);
//        //初始化本地语言环境
//        initLocaleResolver(context);
//        //初始化模板处理器
//        initThemeResolver(context);
        //handlerMapping
        initHandlerMappings(context);
        //初始化参数适配器
        initHandlerAdapters(context);
//        //初始化异常拦截器
//        initHandlerExceptionResolvers(context);
//        //初始化视图预处理器
//        initRequestToViewNameTranslator(context);
        //初始化视图转换器
        initViewResolvers(context);
//        //FlashMap管理器
//        initFlashMapManager(context);
    }

    private void initViewResolvers(ApplicationContext context) {
        String templateRoot = context.getConfig().getProperty("templateRoot");

        String templateRootPath = this.getClass().getClassLoader().getResource(templateRoot).getFile();

        File rootFile = new File(templateRootPath);
        for (File file : rootFile.listFiles()) {
            this.viewResolvers.add(new HnViewResolver(templateRoot));
        }
    }

    private void initHandlerAdapters(ApplicationContext context) {
        for(HnHandlerMapping handlerMapping : handlerMappings) {
            handlerAdapterMap.put(handlerMapping, new HnHandlerAdapter());
        }
    }

    private void initHandlerMappings(ApplicationContext context) {
        if(this.applicationContext.getBeanDefinitionCount() == 0) {return;}

        for(String beanDefinitionName : this.applicationContext.getBeanDefinitionNames()) {
            Object instance = this.applicationContext.getBean(beanDefinitionName);
            Class<?> clazz = instance.getClass();
            if(!clazz.isAnnotationPresent(HnController.class)) {
                continue;
            }

            String baseUrl = "";
            if(clazz.isAnnotationPresent(HnRequestMapping.class)) {
                baseUrl = instance.getClass().getAnnotation(HnRequestMapping.class).value().trim();
            }

            for(Method m : instance.getClass().getMethods()) {
                if(!m.isAnnotationPresent(HnRequestMapping.class)) {
                    continue;
                }
                String regex = ("/" + baseUrl + "/" + m.getAnnotation(HnRequestMapping.class).value().replaceAll("\\*",".*")).replaceAll("/+","/");
                Pattern urlPattern = Pattern.compile(regex);
                handlerMappings.add(new HnHandlerMapping(urlPattern, m, instance));
            }

        }
    }
}
