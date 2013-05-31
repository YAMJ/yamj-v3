package org.yamj.filescanner;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext applicationContext = null;

    /**
     * Created during the initialisation of Spring
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // Assign the ApplicationContext into a static variable
        this.applicationContext = applicationContext;
    }

    /**
     * Get the application context
     *
     * @return
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Check that the application context is configured
     * @return
     */
    public static boolean isConfigured() {
        return (applicationContext != null);
    }

    /**
     * Get a bean using it's class and name
     *
     * @param <T>
     * @param T
     * @param beanName
     * @return
     */
    public static <T> T getBean(Class T, String beanName) throws BeansException {
        return (T) applicationContext.getBean(beanName);
    }
}