package com.boschtech.orderservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import static org.junit.jupiter.api.Assertions.*;

class DataSourceConfigTest {

    private final DataSourceConfig config = new DataSourceConfig();

    private DataSourceProperties propsWithUrl(String url) {
        DataSourceProperties props = new DataSourceProperties();
        props.setUrl(url);
        return props;
    }

    @Test
    void shouldConvertPostgresUrlWithCredentials() {
        DataSourceProperties props = propsWithUrl("postgres://myuser:mypass@ep-cool-host.us-east-2.aws.neon.tech/neondb?sslmode=require");
        config.convertUrl(props);

        assertEquals("jdbc:postgresql://ep-cool-host.us-east-2.aws.neon.tech/neondb?sslmode=require", props.getUrl());
        assertEquals("myuser", props.getUsername());
        assertEquals("mypass", props.getPassword());
    }

    @Test
    void shouldConvertPostgresqlUrlWithCredentials() {
        DataSourceProperties props = propsWithUrl("postgresql://user:pass@host.neon.tech:5432/db");
        config.convertUrl(props);

        assertEquals("jdbc:postgresql://host.neon.tech:5432/db", props.getUrl());
        assertEquals("user", props.getUsername());
        assertEquals("pass", props.getPassword());
    }

    @Test
    void shouldConvertPostgresqlUrlWithoutCredentials() {
        DataSourceProperties props = propsWithUrl("postgresql://host.neon.tech/db?sslmode=require");
        config.convertUrl(props);

        assertEquals("jdbc:postgresql://host.neon.tech/db?sslmode=require", props.getUrl());
    }

    @Test
    void shouldConvertPostgresUrlWithUsernameOnly() {
        DataSourceProperties props = propsWithUrl("postgres://onlyuser@host.neon.tech/db");
        config.convertUrl(props);

        assertEquals("jdbc:postgresql://host.neon.tech/db", props.getUrl());
        assertEquals("onlyuser", props.getUsername());
    }

    @Test
    void shouldPassThroughJdbcUrl() {
        DataSourceProperties props = propsWithUrl("jdbc:postgresql://localhost:5432/testdb");
        config.convertUrl(props);

        assertEquals("jdbc:postgresql://localhost:5432/testdb", props.getUrl());
    }

    @Test
    void shouldHandleNullUrl() {
        DataSourceProperties props = propsWithUrl(null);
        assertDoesNotThrow(() -> config.convertUrl(props));
        assertNull(props.getUrl());
    }

    @Test
    void shouldOmitPortWhenNotSpecified() {
        DataSourceProperties props = propsWithUrl("postgresql://u:p@host.neon.tech/db");
        config.convertUrl(props);

        assertFalse(props.getUrl().contains(":-1"));
        assertEquals("jdbc:postgresql://host.neon.tech/db", props.getUrl());
    }

    @Test
    void shouldOmitQueryWhenNotPresent() {
        DataSourceProperties props = propsWithUrl("postgres://u:p@host.neon.tech/mydb");
        config.convertUrl(props);

        assertEquals("jdbc:postgresql://host.neon.tech/mydb", props.getUrl());
    }

    @Test
    void shouldFallbackForMalformedPostgresUrl() {
        // A URL with spaces causes URISyntaxException
        DataSourceProperties props = propsWithUrl("postgres://bad url with spaces");
        config.convertUrl(props);

        assertEquals("jdbc:postgresql://bad url with spaces", props.getUrl());
    }

    @Test
    void shouldFallbackForMalformedPostgresqlUrl() {
        DataSourceProperties props = propsWithUrl("postgresql://bad url with spaces");
        config.convertUrl(props);

        assertEquals("jdbc:postgresql://bad url with spaces", props.getUrl());
    }

    @Test
    void shouldNotModifyUnrecognizedSchemeInFallback() {
        // A non-postgres, non-jdbc URL that also fails URI parsing
        DataSourceProperties props = propsWithUrl("mysql://bad url with spaces");
        config.convertUrl(props);

        // Falls into catch block but neither startsWith matches, so URL unchanged
        assertEquals("mysql://bad url with spaces", props.getUrl());
    }
}
