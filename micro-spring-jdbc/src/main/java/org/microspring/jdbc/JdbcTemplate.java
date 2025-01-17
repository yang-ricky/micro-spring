package org.microspring.jdbc;

import org.microspring.jdbc.transaction.JdbcTransactionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC操作模板类
 */
public class JdbcTemplate {
    
    private DataSource dataSource;
    private JdbcTransactionManager transactionManager;
    
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public void setTransactionManager(JdbcTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
    
    protected Connection getConnection() throws SQLException {
        if (transactionManager != null && transactionManager.hasCurrentConnection()) {
            return transactionManager.getCurrentConnection();
        }
        return dataSource.getConnection();
    }
    
    protected void releaseConnection(Connection conn) throws SQLException {
        if (transactionManager == null || !transactionManager.hasCurrentConnection()) {
            conn.close();  // 只在非事务连接时关闭
        }
    }
    
    /**
     * 执行更新操作（INSERT、UPDATE、DELETE）
     */
    public int executeUpdate(String sql, Object... args) throws SQLException {
        Connection conn = null;
        try {
            // 优先使用事务的 Connection
            if (transactionManager != null && transactionManager.hasCurrentConnection()) {
                conn = transactionManager.getCurrentConnection();
            } else {
                conn = dataSource.getConnection();
            }
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                for (int i = 0; i < args.length; i++) {
                    ps.setObject(i + 1, args[i]);
                }
                return ps.executeUpdate();
            } finally {
                ps.close();
            }
        } finally {
            // 只有非事务的 Connection 才需要关闭
            if (conn != null && (transactionManager == null || !transactionManager.hasCurrentConnection())) {
                conn.close();
            }
        }
    }
    
    /**
     * 查询单个对象
     */
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... params) throws SQLException {
        List<T> results = query(sql, rowMapper, params);
        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw new SQLException("Query returned more than one result");
        }
        return results.get(0);
    }
    
    /**
     * 查询对象列表
     */
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... params) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            // 优先使用事务的 Connection
            if (transactionManager != null && transactionManager.hasCurrentConnection()) {
                conn = transactionManager.getCurrentConnection();
            } else {
                conn = dataSource.getConnection();
            }
            ps = conn.prepareStatement(sql);
            
            // 设置参数
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                List<T> results = new ArrayList<>();
                int rowNum = 0;
                while (rs.next()) {
                    results.add(rowMapper.mapRow(rs, rowNum++));
                }
                return results;
            }
        } finally {
            if (ps != null) {
                ps.close();
            }
            // 只有非事务的 Connection 才需要关闭
            if (conn != null && (transactionManager == null || !transactionManager.hasCurrentConnection())) {
                conn.close();
            }
        }
    }
} 