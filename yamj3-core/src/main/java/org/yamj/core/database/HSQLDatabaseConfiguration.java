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

import static java.sql.Connection.TRANSACTION_READ_COMMITTED;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.hibernate.SessionFactory;
import org.hsqldb.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.yamj.core.hibernate.AuditInterceptor;

@Configuration
@EnableTransactionManagement
@Profile("hsql")
public class HSQLDatabaseConfiguration extends AbstractDatabaseConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(HSQLDatabaseConfiguration.class);

    @Value("${yamj3.database.showSql:false}")
    private boolean showSql;

    @Value("${yamj3.database.statistics:false}")
    private boolean generateStatistics;

    @Value("${yamj3.database.poolPreparedStatements:true}")
    private boolean poolPreparedStatements;

    @Value("${yamj3.database.connections.initialSize:5}")
    private int initialSize;

    @Value("${yamj3.database.connections.maxActive:5}")
    private int maxActive;

    @Value("${yamj3.database.connections.minIdle:2}")
    private int minIdle;

    @Value("${yamj3.database.connections.maxIdle:10}")
    private int maxIdle;

    @Value("${yamj3.database.connections.maxWait:500}")
    private long maxWait;

    @Value("${yamj3.database.connections.minEvictableIdleTimeMillis:1800000}")
    private long minEvictableIdleTimeMillis;

    @Value("${yamj3.database.connections.timeBetweenEvictionRunsMillis:1800000}")
    private long timeBetweenEvictionRunsMillis;

    @Value("${yamj3.database.connections.numTestsPerEvictionRun:3}")
    private int numTestsPerEvictionRun;

    @Value("${yamj3.database.connections.testOnBorrow:true}")
    private boolean testOnBorrow;

    @Value("${yamj3.database.connections.testWhileIdle:true}")
    private boolean testWhileIdle;

    @Value("${yamj3.database.connections.testOnReturn:true}")
    private boolean testOnReturn;

    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Bean(destroyMethod="shutdown")
    public Server hsqlServer() {
        LOG.debug("Starting HSQL server");
        
        Server hsqlServer = new Server();
        //hsqlServer.setLogWriter(new PrintWriter(System.err));
        hsqlServer.setLogWriter(null);
        hsqlServer.setSilent(true);

        StringBuffer path = new StringBuffer().append("file:");
        path.append(System.getProperty("yamj3.home", ".")).append("/database/yamj3");

        hsqlServer.setDatabaseName(0, "yamj3");
        hsqlServer.setDatabasePath(0, path.toString());

        hsqlServer.setPort(9001); // default port
        hsqlServer.setDaemon(true);
        hsqlServer.start();
        
        LOG.info("Started HSQL server on port {}", hsqlServer.getPort());
        return hsqlServer;
    }
    
    @Override
    @Bean(destroyMethod="close")
    @DependsOn("hsqlServer")
    public DataSource dataSource() {
        LOG.trace("Create new data source");
        
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName("org.hsqldb.jdbcDriver");
        basicDataSource.setUrl("jdbc:hsqldb:hsql://localhost:9001/yamj3");
        basicDataSource.setUsername("sa");
        basicDataSource.setPassword("");
        basicDataSource.setValidationQuery("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
        basicDataSource.setPoolPreparedStatements(poolPreparedStatements);
        
        basicDataSource.setInitialSize(initialSize);
        basicDataSource.setMaxActive(maxActive);
        basicDataSource.setMinIdle(minIdle);
        basicDataSource.setMaxIdle(maxIdle);
        basicDataSource.setMaxWait(maxWait);
        
        basicDataSource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        basicDataSource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        basicDataSource.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        
        basicDataSource.setTestOnBorrow(testOnBorrow);
        basicDataSource.setTestWhileIdle(testWhileIdle);
        basicDataSource.setTestOnReturn(testOnReturn);
        
        basicDataSource.setDefaultTransactionIsolation(TRANSACTION_READ_COMMITTED);

        return basicDataSource;
    }
    
    @Override
    @Bean(destroyMethod="destroy")
    public FactoryBean<SessionFactory> sessionFactory() {
        LocalSessionFactoryBean sessionFactoryBean = new LocalSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource());
        sessionFactoryBean.setEntityInterceptor(new AuditInterceptor());
        sessionFactoryBean.setPackagesToScan("org.yamj.core.database.model");
        
        Properties props = new Properties();
        props.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        props.put("hibernate.show_sql", showSql);
        props.put("hibernate.generate_statistics", generateStatistics);
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.connection.isolation", TRANSACTION_READ_COMMITTED);
        props.put("hibernate.use_sql_comments", false);
        props.put("hibernate.cache.use_query_cache", false);
        props.put("hibernate.cache.use_second_level_cache", false);
        props.put("hibernate.connection.CharSet", "utf8");
        props.put("hibernate.connection.characterEncoding", "utf8");
        props.put("hibernate.connection.useUnicode", false);
        sessionFactoryBean.setHibernateProperties(props);
        
        return sessionFactoryBean;
    }    
}

