package org.microspring.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 简化版的 DataSource 接口
 */
public interface DataSource {
    /**
     * 获取数据库连接
     */
    Connection getConnection() throws SQLException;

    /**
     * 获取数据库URL
     */
    String getUrl();

    /**
     * 获取用户名
     */
    String getUsername();

    /**
     * 获取密码
     */
    String getPassword();

    /**
     * 获取驱动类名
     */
    String getDriverClassName();
}