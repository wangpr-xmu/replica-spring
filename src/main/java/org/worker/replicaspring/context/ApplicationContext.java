package org.worker.replicaspring.context;

import org.worker.replicaspring.annotation.HnAutowired;
import org.worker.replicaspring.annotation.HnController;
import org.worker.replicaspring.annotation.HnService;
import org.worker.replicaspring.aop.HnJdkDynamicProxy;
import org.worker.replicaspring.aop.config.HnAopConfig;
import org.worker.replicaspring.aop.support.HnAdvisedSupport;
import org.worker.replicaspring.beans.HnBeanWrapper;
import org.worker.replicaspring.beans.config.HnBeanDefinition;
import org.worker.replicaspring.beans.support.HnBeanDefinitionReader;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Spring 上下文
 */
public class ApplicationContext {

    private HnBeanDefinitionReader reader;

    private Map<String, HnBeanDefinition> beanDefinitionMap = new HashMap<>();

    private Map<String, Object> factoryBeanObjectCache = new HashMap<>();

    private Map<String, HnBeanWrapper> factoryBeanWrapperCache = new HashMap<>();

    public ApplicationContext(String... contextConfigLocation) {
        //加载配置
        reader = new HnBeanDefinitionReader(contextConfigLocation);
        //封装BeanDefinition
        List<HnBeanDefinition> beanDefinitions = reader.loadBeanDefinitions();
        //注册BeanDefinition
        registerBeanDefinition(beanDefinitions);
        //自动装配
        doAutowired();
    }

    private void doAutowired() {
        for(Map.Entry<String, HnBeanDefinition> entry : beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            getBean(beanName);
        }
    }


    public Object getBean(String beanName) {
        //1. 获取BeanDefinition
        HnBeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        //2. 反射实例化
        Object instance = initialInstance(beanName, beanDefinition);
        //3. 包装beanWrapper
        HnBeanWrapper hnBeanWrapper = new HnBeanWrapper(instance);
        //4. 保存到IoC容器
        factoryBeanWrapperCache.put(beanName, hnBeanWrapper);
        //5. 执行依赖注入
        populateBean(beanName, beanDefinition, hnBeanWrapper);
        return hnBeanWrapper.getWrapperInstance();
    }

    private void populateBean(String beanName, HnBeanDefinition beanDefinition, HnBeanWrapper hnBeanWrapper) {
        Object instance = hnBeanWrapper.getWrapperInstance();

        Class<?> clazz = instance.getClass();

        if(!(clazz.isAnnotationPresent(HnController.class) || clazz.isAnnotationPresent(HnService.class))) {
            return;
        }

        for(Field field : clazz.getDeclaredFields()) {
            if(!field.isAnnotationPresent(HnAutowired.class)) {
                continue;
            }

            String fieldBeanName = field.getAnnotation(HnAutowired.class).value();
            if("".equals(fieldBeanName)) {
                fieldBeanName = field.getType().getName();
            }

            try {
                if(this.factoryBeanWrapperCache.get(fieldBeanName) == null){
                    continue;
                }
                field.setAccessible(true);
                field.set(instance, factoryBeanWrapperCache.get(fieldBeanName).getWrapperInstance());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private Object initialInstance(String beanName, HnBeanDefinition beanDefinition) {
        String beanClassName = beanDefinition.getBeanClassName();
        Object result = null;
        try {
            if(this.factoryBeanObjectCache.containsKey(beanName)) {
                return this.factoryBeanObjectCache.get(beanName);
            }else {
                Class<?> clazz = Class.forName(beanClassName);
                result = clazz.newInstance();
                //==================AOP开始=========================
                //如果满足条件，就直接返回Proxy对象
                //1、加载AOP的配置文件
                HnAdvisedSupport config = instantionAopConfig(beanDefinition);
                config.setTargetClass(clazz);
                config.setTarget(result);

                //判断规则，要不要生成代理类，如果要就覆盖原生对象
                //如果不要就不做任何处理，返回原生对象
                if(config.pointCutMath()){
                    result = new HnJdkDynamicProxy(config).getProxy();
                }

                this.factoryBeanObjectCache.put(beanName, result);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return result;
    }

    private HnAdvisedSupport instantionAopConfig(HnBeanDefinition beanDefinition) {
        HnAopConfig config = new HnAopConfig();
        config.setPointCut(this.reader.getConfig().getProperty("pointCut"));
        config.setAspectClass(this.reader.getConfig().getProperty("aspectClass"));
        config.setAspectBefore(this.reader.getConfig().getProperty("aspectBefore"));
        config.setAspectAfter(this.reader.getConfig().getProperty("aspectAfter"));
        config.setAspectAfterThrow(this.reader.getConfig().getProperty("aspectAfterThrow"));
        config.setAspectAfterThrowingName(this.reader.getConfig().getProperty("aspectAfterThrowingName"));
        return new HnAdvisedSupport(config);
    }

    private void registerBeanDefinition(List<HnBeanDefinition> beanDefinitions) {
        beanDefinitions.forEach((beanDefinition) -> {
            beanDefinitionMap.put(beanDefinition.getBeanClassName(), beanDefinition);
            beanDefinitionMap.put(beanDefinition.getFactoryBeanName(), beanDefinition);
        });
    }
    public Object getBean(Class beanClass){
        return getBean(beanClass.getName());
    }

    public int getBeanDefinitionCount() {
        return this.beanDefinitionMap.size();
    }

    public String[] getBeanDefinitionNames() {
        return this.beanDefinitionMap.keySet().toArray(new String[this.beanDefinitionMap.size()]);
    }

    public Properties getConfig() {
        return this.reader.getConfig();
    }
}
