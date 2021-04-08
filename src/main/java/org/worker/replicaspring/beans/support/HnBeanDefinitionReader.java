package org.worker.replicaspring.beans.support;

import org.worker.replicaspring.annotation.HnController;
import org.worker.replicaspring.annotation.HnService;
import org.worker.replicaspring.beans.config.HnBeanDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class HnBeanDefinitionReader {

    private List<String> beanClassNames = new ArrayList<>();
    private Properties properties = new Properties();

    public HnBeanDefinitionReader(String... contextConfigLocation) {
        //读取配置信息
        loadConfig(contextConfigLocation);
        //扫描相关类
        doScanning(properties.getProperty("scan-package"));
    }

    private void doScanning(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File file = new File(url.getFile());
        for(File f : file.listFiles()) {
            if(f.isDirectory()) {
                doScanning(scanPackage + "." + f.getName());
            }else {
                if(!f.getName().endsWith(".class")) continue;
                beanClassNames.add(scanPackage + "." + f.getName().replace(".class", ""));
            }
        }
    }

    private void loadConfig(String[] contextConfigLocation) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation[0]);
        try {
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<HnBeanDefinition> loadBeanDefinitions() {
        List<HnBeanDefinition> beanDefinitions = new ArrayList<>();
        beanClassNames.forEach((beanClassName) -> {
            try {
                Class<?> beanClass = Class.forName(beanClassName);
                if(beanClass.isAnnotationPresent(HnController.class)) {
                    beanDefinitions.add(createBeanDefinition(toFirstCharLowerCase(beanClass), beanClass.getName()));
                }else if(beanClass.isAnnotationPresent(HnService.class)) {
                    String beanName = beanClass.getAnnotation(HnService.class).value();
                    if("".equals(beanName)) {
                        beanName = toFirstCharLowerCase(beanClass);
                    }
                    beanDefinitions.add(createBeanDefinition(beanName, beanClass.getName()));
                    for(Class<?> inter : beanClass.getInterfaces()) {
                        beanDefinitions.add(createBeanDefinition(inter.getName(), beanClass.getName()));
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        });

        return beanDefinitions;
    }

    private HnBeanDefinition createBeanDefinition(String factoryName, String className) {
        HnBeanDefinition beanDefinition = new HnBeanDefinition();
        beanDefinition.setBeanClassName(className);
        beanDefinition.setFactoryBeanName(factoryName);
        return beanDefinition;
    }


    private String toFirstCharLowerCase(Class<?> aClass) {
        char[] chars = aClass.getSimpleName().toCharArray();
        chars[0] += 32;
        return new String(chars);
    }

    public Properties getConfig() {
        return this.properties;
    }
}
