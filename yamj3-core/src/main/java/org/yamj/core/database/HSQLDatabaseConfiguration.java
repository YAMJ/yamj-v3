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
import org.hsqldb.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@Profile(DatabaseType.HSQL)
public class HSQLDatabaseConfiguration extends AbstractDatabaseConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(HSQLDatabaseConfiguration.class);

    @Value("${yamj3.database.port:9001}")
    protected int port;
    
    @Lazy(false)
    @Bean(destroyMethod="shutdown")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Server hsqlServer() {
        LOG.debug("Starting HSQL server");
        
        Server hsqlServer = new Server();
        hsqlServer.setLogWriter(null);
        hsqlServer.setSilent(true);
        hsqlServer.setNoSystemExit(true);

        StringBuilder path = new StringBuilder()
            .append("file:")
            .append(System.getProperty("yamj3.home", "."))
            .append("/database/yamj3;user=yamj3;password=yamj3"); //NOSONAR
        hsqlServer.setDatabaseName(0, YAMJ3);
        hsqlServer.setDatabasePath(0, path.toString());
        
        hsqlServer.setPort(port);
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
        basicDataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        basicDataSource.setUrl("jdbc:hsqldb:hsql://localhost:"+port+"/yamj3");
        basicDataSource.setUsername(YAMJ3);
        basicDataSource.setPassword(YAMJ3);
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

        populateDatabase(basicDataSource, "update_hsql.sql");

        return basicDataSource;
    }
    
    @Override
    protected Properties hibernateProperties() {
        Properties props = new Properties();
        props.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        props.put("hibernate.show_sql", Boolean.valueOf(showSql));
        props.put("hibernate.generate_statistics", Boolean.valueOf(generateStatistics));
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.connection.isolation", Integer.valueOf(TRANSACTION_READ_COMMITTED));
        props.put("hibernate.use_sql_comments", Boolean.FALSE);
        props.put("hibernate.cache.use_query_cache", Boolean.FALSE);
        props.put("hibernate.cache.use_second_level_cache", Boolean.FALSE);
        props.put("hibernate.connection.CharSet", "utf8");
        props.put("hibernate.connection.characterEncoding", "utf8");
        props.put("hibernate.connection.useUnicode", Boolean.FALSE);
        return props;
    }    
}

