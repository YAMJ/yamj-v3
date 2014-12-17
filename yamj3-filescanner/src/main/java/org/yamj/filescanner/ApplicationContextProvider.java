/*
 *      Copyright (c) 2004-2014 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.filescanner;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext applicationContext = null;

    /**
     * Created during the initialization of Spring
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    @SuppressWarnings("static-access")
    public void setApplicationContext(ApplicationContext applicationContext) {
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
     *
     * @return
     */
    public static boolean isConfigured() {
        return (applicationContext != null);
    }

    /**
     * Get a bean using it's class and name
     *
     * @param <T>
     * @param beanName
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> T getBean(String beanName) throws BeansException {
        return (T) applicationContext.getBean(beanName);
    }
}
