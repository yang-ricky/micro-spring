package org.microspring.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 将ResultSet行数据映射为对象的接口
 */
public interface RowMapper<T> {
    /**
     * 将当前行的ResultSet数据映射为一个对象
     * @param rs ResultSet，已经指向当前行
     * @param rowNum 当前行号，从0开始
     * @return 映射后的对象
     */
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
} 