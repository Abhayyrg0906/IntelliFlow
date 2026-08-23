package com.intelliflow.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.intelliflow.exception.DatabaseException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DBUtil {
    private static HikariDataSource dataSource;

    static {
        Properties props = new Properties();
        try (InputStream is = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is == null) {
                throw new DatabaseException("db.properties file not found in classpath");
            }
            props.load(is);
        } catch (IOException e) {
            throw new DatabaseException("Failed to load db.properties file", e);
        }

        // Try loading git-ignored db-local.properties
        try (InputStream isLocal = DBUtil.class.getClassLoader().getResourceAsStream("db-local.properties")) {
            if (isLocal != null) {
                props.load(isLocal);
            }
        } catch (IOException ignored) {}

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("db.url"));

        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASS");

        String propUser = props.getProperty("db.username");
        String propPass = props.getProperty("db.password");

        String resolvedUser = (envUser != null && !envUser.trim().isEmpty()) ? envUser : resolvePlaceholder(propUser, "DB_USER");
        String resolvedPass = (envPass != null && !envPass.trim().isEmpty()) ? envPass : resolvePlaceholder(propPass, "DB_PASS");

        config.setUsername(resolvedUser);
        config.setPassword(resolvedPass);
        
        // Driver specification is optional for modern JDBC but good for explicit documentation
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Pool configurations
        int maxSize = Integer.parseInt(props.getProperty("db.pool.maxSize", "10"));
        long idleTimeout = Long.parseLong(props.getProperty("db.pool.idleTimeout", "300000"));
        long connTimeout = Long.parseLong(props.getProperty("db.pool.connectionTimeout", "20000"));

        config.setMaximumPoolSize(maxSize);
        config.setIdleTimeout(idleTimeout);
        config.setConnectionTimeout(connTimeout);

        // Optimizations for MySQL performance
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        try {
            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            // Log or wrap the exception: database connection fails at start
            throw new DatabaseException("Failed to initialize database connection pool", e);
        }
    }

    private DBUtil() {}

    /**
     * Retrieves a pooled MySQL connection.
     * Caller is responsible for closing the connection (preferably using try-with-resources).
     *
     * @return Connection object
     * @throws DatabaseException if connection retrieval fails
     */
    public static Connection getConnection() throws DatabaseException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to obtain database connection from pool", e);
        }
    }

    /**
     * Closes the connection pool datasource.
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private static String resolvePlaceholder(String value, String envName) {
        if (value == null) return null;
        if (value.equals("${" + envName + "}")) {
            String envVal = System.getenv(envName);
            if (envVal != null && !envVal.trim().isEmpty()) {
                return envVal;
            }
        }
        return value;
    }
}
