package org.ai.clinic.example.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @Qualifier("readOnlyJdbcTemplate")
    public JdbcTemplate readOnlyJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(new ReadOnlyDataSourceWrapper(dataSource));
    }
}
