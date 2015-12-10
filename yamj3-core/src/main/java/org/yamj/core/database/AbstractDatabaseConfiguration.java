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
package org.yamj.core.database;

import java.util.Properties;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.yamj.core.hibernate.AuditInterceptor;

public abstract class AbstractDatabaseConfiguration implements DatabaseConfiguration {
    
    @Value("${yamj3.database.showSql:false}")
    protected boolean showSql;

    @Value("${yamj3.database.statistics:false}")
    protected boolean generateStatistics;

    @Value("${yamj3.database.poolPreparedStatements:true}")
    protected boolean poolPreparedStatements;

    @Value("${yamj3.database.connections.initialSize:5}")
    protected int initialSize;

    @Value("${yamj3.database.connections.maxActive:5}")
    protected int maxActive;

    @Value("${yamj3.database.connections.minIdle:2}")
    protected int minIdle;

    @Value("${yamj3.database.connections.maxIdle:10}")
    protected int maxIdle;

    @Value("${yamj3.database.connections.maxWait:500}")
    protected long maxWait;

    @Value("${yamj3.database.connections.minEvictableIdleTimeMillis:1800000}")
    protected long minEvictableIdleTimeMillis;

    @Value("${yamj3.database.connections.timeBetweenEvictionRunsMillis:1800000}")
    protected long timeBetweenEvictionRunsMillis;

    @Value("${yamj3.database.connections.numTestsPerEvictionRun:3}")
    protected int numTestsPerEvictionRun;

    @Value("${yamj3.database.connections.testOnBorrow:true}")
    protected boolean testOnBorrow;

    @Value("${yamj3.database.connections.testWhileIdle:true}")
    protected boolean testWhileIdle;

    @Value("${yamj3.database.connections.testOnReturn:true}")
    protected boolean testOnReturn;

    @Bean
    @Override
    public PlatformTransactionManager transactionManager() {
        try {
            HibernateTransactionManager transactionManager = new HibernateTransactionManager(sessionFactory().getObject());
            transactionManager.setDefaultTimeout(30);
            return transactionManager;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to retrieve session factory", ex);
        }
    }
    
    @Override
    @Bean(destroyMethod="destroy")
    public FactoryBean<SessionFactory> sessionFactory() {
        LocalSessionFactoryBean sessionFactoryBean = new LocalSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource());
        sessionFactoryBean.setEntityInterceptor(new AuditInterceptor());
        sessionFactoryBean.setPackagesToScan("org.yamj.core.database.model");
        sessionFactoryBean.setHibernateProperties(hibernateProperties());
        return sessionFactoryBean;
    }
        
    protected abstract Properties hibernateProperties();
}

