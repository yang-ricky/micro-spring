package org.microspring.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DriverManagerDataSource implements DataSource {
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private Properties connectionProperties = new Properties();
    
    private String loginTimeout = "30"; // 默认30秒

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public void setLoginTimeout(String timeout) {
        this.loginTimeout = timeout;
    }

    public void setConnectionProperties(Properties connectionProperties) {
        this.connectionProperties = connectionProperties;
    }

    public void init() {
        if (driverClassName != null && !driverClassName.isEmpty()) {
            try {
                Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load JDBC driver class: " + driverClassName, e);
            }
        }
        
        // 设置登录超时，在这里进行类型转换
        try {
            int timeout = Integer.parseInt(loginTimeout);
            DriverManager.setLoginTimeout(timeout);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid login timeout value: " + loginTimeout, e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        Properties props = new Properties();
        // 先设置连接属性
        if (connectionProperties != null) {
            props.putAll(connectionProperties);
        }
        // 再设置用户名密码，这样不会被连接属性覆盖
        if (username != null) {
            props.setProperty("user", username);
        }
        if (password != null) {
            props.setProperty("password", password);
        }
        
        try {
            Connection conn = DriverManager.getConnection(url, props);
            
            // 应用连接属性
            if (props.containsKey("autoCommit")) {
                boolean autoCommit = Boolean.parseBoolean(props.getProperty("autoCommit"));
                conn.setAutoCommit(autoCommit);
            }
            
            return conn;
        } catch (SQLException e) {
            String msg = "Failed to connect to database. URL: " + url + ", User: " + username;
            System.err.println("[ERROR] " + msg + ": " + e.getMessage());
            throw new SQLException(msg, e);
        }
    }

    public String getConnectionDetails() {
        return String.format("URL: %s, Username: %s, Driver: %s", 
            url, username, driverClassName);
    }
}