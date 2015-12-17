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

import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@Profile("h2")
public class H2DatabaseConfiguration extends AbstractDatabaseConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(H2DatabaseConfiguration.class);

    @Value("${yamj3.database.port:9092}")
    protected int port;

    @Lazy(false)
    @Bean(destroyMethod="shutdown")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @DependsOn("dataSource")
    public Server h2Server() throws SQLException {
        LOG.debug("Starting H2 tcp server");
        
        Server h2Server = Server.createTcpServer(
            "-tcpPort", Integer.toString(port),
            "-tcpAllowOthers",
            "-tcpDaemon");
        h2Server.start();
        
        LOG.info("Started H2 tcp server on port {}", h2Server.getPort());
        return h2Server;
    }
    
    @Override
    @Bean
    public DataSource dataSource() {
        LOG.trace("Create new data source");
        
        JdbcDataSource dataSource = new JdbcDataSource();
        StringBuilder url = new StringBuilder()
            .append("jdbc:h2:")
            .append(System.getProperty("yamj3.home", "."))
            .append("/database/yamj3;AUTO_SERVER=TRUE");
        dataSource.setUrl(url.toString());
        
        dataSource.setUser(YAMJ3);
        dataSource.setPassword(YAMJ3);
        
        populateDatabase(dataSource, "update_h2.sql");
        
        return dataSource;
    }
    
    @Override
    protected Properties hibernateProperties() {
        Properties props = new Properties();
        props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
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
        return props;
    }    
}

