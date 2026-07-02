package com.example.gymcrm.config;

import com.example.gymcrm.domain.Trainee;
import com.example.gymcrm.domain.Trainer;
import com.example.gymcrm.domain.Training;
import com.example.gymcrm.domain.TrainingType;
import com.example.gymcrm.domain.User;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
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
@ComponentScan("com.example.gymcrm")
public class AppConfig {
    private static final String DEFAULT_DB_URL = "jdbc:h2:mem:gymcrm;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(System.getProperty("gym.db.driver", "org.h2.Driver"));
        dataSource.setUrl(System.getProperty("gym.db.url", DEFAULT_DB_URL));
        dataSource.setUsername(System.getProperty("gym.db.username", "sa"));
        dataSource.setPassword(System.getProperty("gym.db.password", ""));
        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setDatabasePlatform(System.getProperty("gym.hibernate.dialect", "org.hibernate.dialect.H2Dialect"));

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan(User.class.getPackageName());
        factory.setJpaProperties(jpaProperties());
        factory.setManagedClassNameFilter(className -> className.equals(User.class.getName())
                || className.equals(Trainee.class.getName())
                || className.equals(Trainer.class.getName())
                || className.equals(Training.class.getName())
                || className.equals(TrainingType.class.getName()));
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

    private Properties jpaProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.hbm2ddl.auto", System.getProperty("gym.hibernate.ddl-auto", "create-drop"));
        properties.put("hibernate.show_sql", System.getProperty("gym.hibernate.show-sql", "false"));
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.highlight_sql", "false");
        return properties;
    }
}
