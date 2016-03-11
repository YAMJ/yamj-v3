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

import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.joda.JodaMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.config.LocaleService;

@Configuration
@ComponentScan("org.yamj.core")
public class YamjConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private LocaleService localeService;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/favicon.ico").addResourceLocations("WEB-INF/images/favicon.ico/").setCachePeriod(60);
        registry.addResourceHandler("/css/**").addResourceLocations("WEB-INF/css/").setCachePeriod(60);
        registry.addResourceHandler("/images/**").addResourceLocations("WEB-INF/images/").setCachePeriod(60);
        registry.addResourceHandler("/fonts/**").addResourceLocations("WEB-INF/fonts/").setCachePeriod(60);
        registry.addResourceHandler("/less/**").addResourceLocations("WEB-INF/less/").setCachePeriod(60);
        registry.addResourceHandler("/scss/**").addResourceLocations("WEB-INF/scss/").setCachePeriod(60);
        registry.addResourceHandler("/scripts/**").addResourceLocations("WEB-INF/scripts/").setCachePeriod(60);
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
        converters.add(new MappingJackson2HttpMessageConverter(
           new JodaMapper().registerModule(
               new Hibernate5Module().configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true))));
        
        ByteArrayHttpMessageConverter byteArrayHttpMessageConverter = new ByteArrayHttpMessageConverter();
        byteArrayHttpMessageConverter.setSupportedMediaTypes(Arrays.asList(new MediaType[]{MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG}));
        converters.add(byteArrayHttpMessageConverter);
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

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames("/WEB-INF/i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    @Bean
    @DependsOn("localeService")
    public LocaleResolver localeResolver() {
        SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        localeResolver.setDefaultLocale(localeService.getLocale());
        return localeResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("language");
        registry.addInterceptor(interceptor);
    }
}
