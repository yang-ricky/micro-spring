package org.microspring.jdbc;

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
    
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * 执行更新操作（INSERT、UPDATE、DELETE）
     */
    public int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // 设置参数
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            
            return ps.executeUpdate();
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
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
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
        }
    }
} 