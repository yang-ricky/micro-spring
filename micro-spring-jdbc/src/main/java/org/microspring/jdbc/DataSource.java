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
}