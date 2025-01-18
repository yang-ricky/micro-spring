package org.microspring.jdbc;

/**
 * 扩展的 DataSource 接口
 * 继承自标准的 javax.sql.DataSource，增加了一些便于配置的方法
 */
public interface DataSource extends javax.sql.DataSource {
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