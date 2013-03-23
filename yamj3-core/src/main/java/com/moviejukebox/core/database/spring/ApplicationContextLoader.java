package com.moviejukebox.core.database.spring;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ApplicationContextLoader {

    protected ConfigurableApplicationContext applicationContext;

    public ConfigurableApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Loads application context. Override this method to change how the
     * application context is loaded.
     * 
     * @param configLocations
     *            configuration file locations
     */
    protected void loadApplicationContext(String... configLocations) {
        applicationContext = new ClassPathXmlApplicationContext(configLocations);
        applicationContext.registerShutdownHook();
    }

    /**
     * Injects dependencies into the object. Override this method if you need
     * full control over how dependencies are injected.
     * 
     * @param main
     *            object to inject dependencies into
     */
    protected void injectDependencies(Object main) {
        getApplicationContext().getBeanFactory().autowireBeanProperties(
                main, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
    }

    /**
     * Loads application context, then injects dependencies into the object.
     * 
     * @param main
     *            object to inject dependencies into
     * @param configLocations
     *            configuration file locations
     */
    public void load(Object main, String... configLocations) {
        loadApplicationContext(configLocations);
        injectDependencies(main);
    }
}