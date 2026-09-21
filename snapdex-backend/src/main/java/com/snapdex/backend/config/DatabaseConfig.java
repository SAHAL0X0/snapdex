package com.snapdex.backend.config;

import java.net.URI;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    private final Environment env;

    public DatabaseConfig(Environment env) {
        this.env = env;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        // 1. Check for complete connection URLs (MYSQL_URL, DATABASE_URL, SPRING_DATASOURCE_URL)
        String rawUrl = getFirstNonEmpty(
                env.getProperty("SPRING_DATASOURCE_URL"),
                System.getenv("SPRING_DATASOURCE_URL"),
                env.getProperty("MYSQL_URL"),
                System.getenv("MYSQL_URL"),
                env.getProperty("DATABASE_URL"),
                System.getenv("DATABASE_URL")
        );

        String host = null;
        String port = null;
        String database = null;
        String username = null;
        String password = null;

        if (rawUrl != null && !rawUrl.trim().isEmpty() && !rawUrl.contains("${")) {
            rawUrl = rawUrl.trim();
            if (rawUrl.startsWith("mysql://")) {
                try {
                    URI uri = new URI(rawUrl);
                    host = uri.getHost();
                    port = uri.getPort() > 0 ? String.valueOf(uri.getPort()) : "3306";
                    String path = uri.getPath();
                    if (path != null && path.startsWith("/")) {
                        database = path.substring(1);
                    }
                    String userInfo = uri.getUserInfo();
                    if (userInfo != null) {
                        String[] parts = userInfo.split(":", 2);
                        username = parts[0];
                        if (parts.length > 1) {
                            password = parts[1];
                        }
                    }
                    logger.info("SnapDex DatabaseConfig -> Parsed mysql:// URL for host: {}, port: {}, database: {}", host, port, database);
                } catch (Exception e) {
                    logger.warn("Could not parse mysql:// URL, falling back to individual parameters: {}", e.getMessage());
                }
            } else if (rawUrl.startsWith("jdbc:mysql://")) {
                // If it's already a clean JDBC URL without unexpanded placeholders
                if (!rawUrl.contains("${") && !rawUrl.contains("}")) {
                    String user = getFirstNonEmpty(
                            env.getProperty("spring.datasource.username"),
                            System.getenv("MYSQLUSER"),
                            System.getenv("MYSQL_USER"),
                            "root"
                    );
                    String pass = getFirstNonEmpty(
                            env.getProperty("spring.datasource.password"),
                            System.getenv("MYSQLPASSWORD"),
                            System.getenv("MYSQL_PASSWORD"),
                            ""
                    );
                    logger.info("SnapDex DatabaseConfig -> Using provided clean JDBC URL: {}", rawUrl.replaceAll(":[^:@]+@", ":****@"));
                    return buildDataSource(rawUrl, clean(user, "root"), clean(pass, ""));
                }
            }
        }

        // 2. Resolve individual host and determine environment (production vs local)
        if (host == null) {
            host = getFirstNonEmpty(
                    env.getProperty("MYSQLHOST"),
                    System.getenv("MYSQLHOST"),
                    env.getProperty("MYSQL_HOST"),
                    System.getenv("MYSQL_HOST"),
                    "localhost"
            );
        }
        host = clean(host, "localhost");

        boolean isProduction = !host.equalsIgnoreCase("localhost") && !host.equals("127.0.0.1");

        // 3. Resolve port
        if (port == null) {
            port = getFirstNonEmpty(
                    env.getProperty("MYSQLPORT"),
                    System.getenv("MYSQLPORT"),
                    env.getProperty("MYSQL_PORT"),
                    System.getenv("MYSQL_PORT"),
                    "3306"
            );
        }
        port = clean(port, "3306");

        // 4. Resolve database name with strict sanitization
        if (database == null) {
            database = getFirstNonEmpty(
                    env.getProperty("MYSQLDATABASE"),
                    System.getenv("MYSQLDATABASE"),
                    env.getProperty("MYSQL_DATABASE"),
                    System.getenv("MYSQL_DATABASE")
            );
        }
        // Critical fix: If database name is null, empty, or literal "${MYSQLDATABASE}", replace it!
        database = cleanDatabaseName(database, isProduction);

        // 5. Resolve username
        if (username == null) {
            username = getFirstNonEmpty(
                    env.getProperty("MYSQLUSER"),
                    System.getenv("MYSQLUSER"),
                    env.getProperty("MYSQL_USER"),
                    System.getenv("MYSQL_USER"),
                    env.getProperty("spring.datasource.username"),
                    "root"
            );
        }
        username = clean(username, "root");

        // 6. Resolve password
        if (password == null) {
            password = getFirstNonEmpty(
                    env.getProperty("SPRING_DATASOURCE_PASSWORD"),
                    System.getenv("SPRING_DATASOURCE_PASSWORD"),
                    env.getProperty("MYSQLPASSWORD"),
                    System.getenv("MYSQLPASSWORD"),
                    env.getProperty("MYSQL_PASSWORD"),
                    System.getenv("MYSQL_PASSWORD"),
                    env.getProperty("spring.datasource.password")
            );
        }
        String defaultPassword = isProduction ? "" : "SnapdexDB@2026";
        password = clean(password, defaultPassword);

        // 7. Construct robust JDBC URL with automatic DB creation and timezone
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        logger.info("SnapDex DatabaseConfig -> Connecting to MySQL at {}:{}/{} as user '{}' (mode: {})",
                host, port, database, username, isProduction ? "RAILWAY_PRODUCTION" : "LOCAL_DEVELOPMENT");

        return buildDataSource(jdbcUrl, username, password);
    }

    private DataSource buildDataSource(String jdbcUrl, String username, String password) {
        return DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .build();
    }

    private String clean(String val, String defaultVal) {
        if (val == null) return defaultVal;
        String trimmed = val.trim();
        if (trimmed.isEmpty() || trimmed.contains("${") || trimmed.contains("}")) {
            return defaultVal;
        }
        return trimmed;
    }

    private String cleanDatabaseName(String val, boolean isProduction) {
        if (val == null) {
            return isProduction ? "railway" : "snapdex";
        }
        String trimmed = val.trim();
        // If placeholder was unexpanded or literal string '${MYSQLDATABASE}', sanitize:
        if (trimmed.isEmpty() || trimmed.contains("${") || trimmed.contains("}")) {
            return isProduction ? "railway" : "snapdex";
        }
        return trimmed;
    }

    private String getFirstNonEmpty(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v;
            }
        }
        return null;
    }
}
