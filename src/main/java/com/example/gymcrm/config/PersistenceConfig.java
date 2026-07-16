package com.example.gymcrm.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
public class PersistenceConfig {

    @Bean
    public DataSource dataSource(Environment environment) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(environment.getProperty("gym.db.driver", "org.h2.Driver"));
        dataSource.setUrl(environment.getProperty(
                "gym.db.url", "jdbc:h2:mem:gymcrm;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"));
        dataSource.setUsername(environment.getProperty("gym.db.username", "sa"));
        dataSource.setPassword(environment.getProperty("gym.db.password", ""));
        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource, Environment environment) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.example.gymcrm.domain");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties properties = new Properties();
        properties.setProperty(
                "hibernate.hbm2ddl.auto", environment.getProperty("gym.hibernate.ddl-auto", "create-drop"));
        properties.setProperty(
                "hibernate.show_sql", environment.getProperty("gym.hibernate.show-sql", "false"));
        properties.setProperty("hibernate.format_sql", "true");
        factory.setJpaProperties(properties);
        return factory;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
