package org.microspring.jdbc;

import org.junit.Before;
import org.junit.Test;
import org.microspring.context.ApplicationContext;
import org.microspring.context.support.ClassPathXmlApplicationContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.*;

public class DataSourceTest {
    
    private ApplicationContext context;
    private DataSource dataSource;
    
    @Before
    public void init() {
        context = new ClassPathXmlApplicationContext("datasource-test.xml");
        dataSource = (DataSource) context.getBean("dataSource");
        assertNotNull("DataSource should not be null", dataSource);
        
        initDatabase();
    }
    
    private void initDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "name VARCHAR(50) NOT NULL," +
                        "age INT)");
            
            stmt.execute("DELETE FROM users");
            stmt.execute("INSERT INTO users (name, age) VALUES ('Tom', 20), ('Jerry', 18)");
            
        } catch (Exception e) {
            fail("Failed to initialize test database: " + e.getMessage());
        }
    }
    
    @Test
    public void testSuccessfulConnection() {
        try (Connection conn = dataSource.getConnection()) {
            assertTrue("Connection should be valid", conn.isValid(5));
        } catch (SQLException e) {
            fail("Should connect successfully: " + e.getMessage());
        }
    }
    
    @Test(expected = RuntimeException.class)
    public void testConnectionFailure() {
        DriverManagerDataSource badDs = new DriverManagerDataSource();
        badDs.setDriverClassName("org.h2.Driver");
        badDs.setUrl("jdbc:h2:tcp://nonexistent-host:9092/testdb");
        badDs.setUsername("sa");
        badDs.setPassword("");
        badDs.setLoginTimeout("1");
        
        badDs.init();
        
        DataSourcePostProcessor processor = new DataSourcePostProcessor();
        processor.postProcessAfterInitialization(badDs, "badDataSource");
    }
    
    @Test
    public void testConnectionProperties() {
        DriverManagerDataSource ds = (DriverManagerDataSource) dataSource;
        Properties props = new Properties();
        props.setProperty("autoCommit", "false");
        ds.setConnectionProperties(props);
        
        try (Connection conn = ds.getConnection()) {
            assertFalse("Auto-commit should be false", conn.getAutoCommit());
        } catch (SQLException e) {
            fail("Should connect successfully with properties: " + e.getMessage());
        }
    }
    
    @Test
    public void testBasicQuery() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            
            assertTrue(rs.next());
            int count = rs.getInt(1);
            assertEquals("Should have 2 test users", 2, count);
            
        } catch (Exception e) {
            fail("Failed to execute test query: " + e.getMessage());
        }
    }
}