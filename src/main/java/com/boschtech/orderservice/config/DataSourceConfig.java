package com.boschtech.orderservice.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Converts Neon/Heroku-style DATABASE_URL values (postgres:// or postgresql://)
 * into JDBC-compatible URLs (jdbc:postgresql://) and extracts embedded credentials.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String url = properties.getUrl();
        if (url != null && !url.startsWith("jdbc:")) {
            try {
                URI uri = new URI(url);
                String userInfo = uri.getUserInfo();
                if (userInfo != null) {
                    String[] parts = userInfo.split(":", 2);
                    properties.setUsername(parts[0]);
                    if (parts.length > 1) {
                        properties.setPassword(parts[1]);
                    }
                }
                String jdbcUrl = "jdbc:postgresql://" + uri.getHost()
                        + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                        + uri.getPath()
                        + (uri.getQuery() != null ? "?" + uri.getQuery() : "");
                properties.setUrl(jdbcUrl);
            } catch (Exception e) {
                // If parsing fails, prepend jdbc: and hope for the best
                if (url.startsWith("postgres://")) {
                    properties.setUrl("jdbc:postgresql://" + url.substring("postgres://".length()));
                } else if (url.startsWith("postgresql://")) {
                    properties.setUrl("jdbc:" + url);
                }
            }
        }
        return properties.initializeDataSourceBuilder().build();
    }
}
