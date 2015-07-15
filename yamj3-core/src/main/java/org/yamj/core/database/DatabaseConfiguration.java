package org.yamj.core.database;

import static java.sql.Connection.TRANSACTION_READ_COMMITTED;

import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.yamj.core.hibernate.AuditInterceptor;

@Configuration
@EnableTransactionManagement
public class DatabaseConfiguration  {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseConfiguration.class);

    @Value("${yamj3.database.driver}")
    private String driverClassName;

    @Value("${yamj3.database.dialect}")
    private String dialect;

    @Value("${yamj3.database.url}")
    private String url;

    @Value("${yamj3.database.username}")
    private String username;

    @Value("${yamj3.database.password}")
    private String password;

    @Value("${yamj3.database.showSql:false}")
    private boolean showSql;

    @Value("${yamj3.database.statistics:false}")
    private boolean generateStatistics;

    @Value("${yamj3.database.auto:update}")
    private String hbm2ddlAuto;

    @Value("${yamj3.database.poolPreparedStatements:true}")
    private boolean poolPreparedStatements;

    
    @Value("${yamj3.database.validationQuery:null}")
    private String validationQuery;

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

    
    @Bean(destroyMethod="close")
    public DataSource dataSource() {
        LOG.trace("Create new data source");
        
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(driverClassName);
        basicDataSource.setUrl(url);
        basicDataSource.setUsername(username);
        basicDataSource.setPassword(password);
        basicDataSource.setValidationQuery(validationQuery);
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
    
    @Bean(destroyMethod="destroy")
    public FactoryBean<SessionFactory> sessionFactory() {
        LocalSessionFactoryBean sessionFactoryBean = new LocalSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource());
        sessionFactoryBean.setEntityInterceptor(new AuditInterceptor());
        sessionFactoryBean.setPackagesToScan("org.yamj.core.database.model");
        
        Properties props = new Properties();
        props.put("hibernate.dialect", dialect);
        props.put("hibernate.show_sql", showSql);
        props.put("hibernate.generate_statistics", generateStatistics);
        props.put("hibernate.hbm2ddl.auto", hbm2ddlAuto);
        props.put("hibernate.connection.isolation", 4);
        props.put("hibernate.use_sql_comments", true);
        props.put("hibernate.cache.use_query_cache", false);
        props.put("hibernate.cache.use_second_level_cache", false);
        props.put("hibernate.connection.CharSet", "utf8");
        props.put("hibernate.connection.characterEncoding", "utf8");
        props.put("hibernate.connection.useUnicode", false);
        sessionFactoryBean.setHibernateProperties(props);
        
        return sessionFactoryBean;
    }
    
    @Bean
    public PlatformTransactionManager transactionManager() throws Exception {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager(sessionFactory().getObject());
        transactionManager.setDefaultTimeout(30);
        return transactionManager;
    }
}

