/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
package org.yamj.core;

import java.util.List;
import java.util.Properties;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.yamj.common.tools.PropertyTools;

@Configuration
@EnableScheduling
@ComponentScan("org.yamj.core")
public class YamjConfiguration extends WebMvcConfigurationSupport {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**").addResourceLocations("WEB-INF/images/").setCachePeriod(60);
        registry.addResourceHandler("/css/**").addResourceLocations("WEB-INF/css/").setCachePeriod(60);
        registry.addResourceHandler("/favicon.ico").addResourceLocations("WEB-INF/images/favicon.ico/").setCachePeriod(60);
        registry.addResourceHandler("/fonts/**").addResourceLocations("WEB-INF/fonts/").setCachePeriod(60);
        registry.addResourceHandler("/less/**").addResourceLocations("WEB-INF/less/").setCachePeriod(60);
        registry.addResourceHandler("/scss/**").addResourceLocations("WEB-INF/scss/").setCachePeriod(60);
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }
 
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter(new YamjObjectMapper()));
    }

    @Bean
    public InternalResourceViewResolver getInternalResourceViewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/pages/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    @Bean
    public static PropertyPlaceholderConfigurer propertyConfigurer() {
        final String yamjHome = System.getProperty("yamj3.home", ".");
        
        PropertyPlaceholderConfigurer configurer = new PropertyTools();
        configurer.setIgnoreResourceNotFound(true);
        configurer.setLocations(
               new ClassPathResource("/yamj3-core-static.properties"),
               new FileSystemResource(yamjHome + "/config/yamj3-core-static.properties"),
               new FileSystemResource(yamjHome + "/config/yamj3-core-static.user.properties"));
        return configurer;
    }

    @Bean
    public FactoryBean<Properties> dynamicProperties() {
        PropertiesFactoryBean factoryBean = new PropertiesFactoryBean();
        factoryBean.setLocation(new ClassPathResource("/yamj3-core-dynamic.properties"));
        return factoryBean;
    }

    @Bean
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver commonsMultipartResolver = new CommonsMultipartResolver();
        commonsMultipartResolver.setDefaultEncoding("UTF-8");
        commonsMultipartResolver.setMaxUploadSize(50000000);
        return commonsMultipartResolver;
    }
}
