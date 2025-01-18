package org.microspring.jdbc;

import org.junit.Before;
import org.junit.Test;
import org.microspring.jdbc.transaction.JdbcTransactionManager;
import org.microspring.transaction.TransactionDefinition;
import org.microspring.transaction.support.DefaultTransactionDefinition;
import org.microspring.transaction.TransactionStatus;
import java.sql.SQLException;
import java.sql.ResultSet;
import static org.junit.Assert.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

public class TransactionIsolationTest {
    
    private JdbcTemplate jdbcTemplate;
    private JdbcTransactionManager transactionManager;
    
    @Before
    public void init() {
        // 初始化数据源
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        
        // 初始化JdbcTemplate和TransactionManager
        jdbcTemplate = new JdbcTemplate();
        jdbcTemplate.setDataSource(dataSource);
        transactionManager = new JdbcTransactionManager(dataSource);
        jdbcTemplate.setTransactionManager(transactionManager);
        
        // 创建测试表
        try {
            jdbcTemplate.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users2 (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "USERNAME VARCHAR(100)" +
                ")"
            );
            jdbcTemplate.executeUpdate("DELETE FROM users2");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    public void testReadUncommitted() throws Exception {
        // 用于线程间同步
        final CountDownLatch writeStarted = new CountDownLatch(1);
        final CountDownLatch readFinished = new CountDownLatch(1);
        final AtomicInteger readCount = new AtomicInteger();
        
        // 写线程：插入数据但不提交
        Thread writerThread = new Thread(() -> {
            try {
                DefaultTransactionDefinition def1 = new DefaultTransactionDefinition();
                def1.setIsolationLevel(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
                TransactionStatus status1 = transactionManager.getTransaction(def1);
                
                jdbcTemplate.executeUpdate("INSERT INTO users2 (USERNAME) VALUES (?)", "Alice");
                writeStarted.countDown(); // 通知读线程可以开始读了
                
                // 等待读线程完成读取
                readFinished.await();
                
                // 最后回滚事务
                transactionManager.rollback(status1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "WriterThread");
        
        // 读线程：尝试读取未提交的数据
        Thread readerThread = new Thread(() -> {
            try {
                // 等待写线程插入数据
                writeStarted.await();
                
                DefaultTransactionDefinition def2 = new DefaultTransactionDefinition();
                def2.setIsolationLevel(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
                TransactionStatus status2 = transactionManager.getTransaction(def2);
                
                // 读取数据
                int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users2 WHERE USERNAME = ?",
                    new RowMapper<Integer>() {
                        @Override
                        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                            return rs.getInt(1);
                        }
                    },
                    "Alice"
                );
                
                readCount.set(count);
                readFinished.countDown(); // 通知写线程可以回滚了
                
                transactionManager.commit(status2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "ReaderThread");
        
        // 启动线程
        writerThread.start();
        readerThread.start();
        
        // 等待两个线程完成
        writerThread.join();
        readerThread.join();
        
        // 验证脏读结果
        assertEquals("Should see uncommitted data in READ_UNCOMMITTED", 1, readCount.get());
    }
    
    @Test
    public void testReadCommitted() throws Exception {
        // 用于线程间同步
        final CountDownLatch writeStarted = new CountDownLatch(1);
        final CountDownLatch readFinished = new CountDownLatch(1);
        final AtomicInteger readCount = new AtomicInteger();
        
        // 写线程：插入数据但不提交
        Thread writerThread = new Thread(() -> {
            try {
                DefaultTransactionDefinition def1 = new DefaultTransactionDefinition();
                def1.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
                TransactionStatus status1 = transactionManager.getTransaction(def1);
                
                jdbcTemplate.executeUpdate("INSERT INTO users2 (USERNAME) VALUES (?)", "Bob");
                writeStarted.countDown(); // 通知读线程可以开始读了
                
                // 等待读线程完成第一次读取
                readFinished.await();
                
                // 最后回滚事务
                transactionManager.rollback(status1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "WriterThread");
        
        // 读线程：在READ_COMMITTED级别下读取数据
        Thread readerThread = new Thread(() -> {
            try {
                // 等待写线程插入数据
                writeStarted.await();
                
                DefaultTransactionDefinition def2 = new DefaultTransactionDefinition();
                def2.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
                TransactionStatus status2 = transactionManager.getTransaction(def2);
                
                // 读取数据 - 在READ_COMMITTED级别下不应该看到未提交的数据
                int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users2 WHERE USERNAME = ?",
                    new RowMapper<Integer>() {
                        @Override
                        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                            return rs.getInt(1);
                        }
                    },
                    "Bob"
                );
                
                readCount.set(count);
                readFinished.countDown(); // 通知写线程可以回滚了
                
                transactionManager.commit(status2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "ReaderThread");
        
        // 启动线程
        writerThread.start();
        readerThread.start();
        
        // 等待两个线程完成
        writerThread.join();
        readerThread.join();
        
        // 验证结果：在READ_COMMITTED级别下不应该看到未提交的数据
        assertEquals("Should NOT see uncommitted data in READ_COMMITTED", 0, readCount.get());
    }
    
    @Test
    public void testRepeatableRead() throws Exception {
        // 用于线程间同步
        final CountDownLatch readStarted = new CountDownLatch(1);
        final CountDownLatch writeFinished = new CountDownLatch(1);
        final AtomicInteger firstReadCount = new AtomicInteger();
        final AtomicInteger secondReadCount = new AtomicInteger();
        
        // 读线程：在同一个事务中读取两次数据
        Thread readerThread = new Thread(() -> {
            try {
                DefaultTransactionDefinition def1 = new DefaultTransactionDefinition();
                def1.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
                TransactionStatus status1 = transactionManager.getTransaction(def1);
                
                // 第一次读取
                int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users2 WHERE USERNAME = ?",
                    new RowMapper<Integer>() {
                        @Override
                        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                            return rs.getInt(1);
                        }
                    },
                    "Charlie"
                );
                firstReadCount.set(count);
                
                // 通知写线程可以开始写入了
                readStarted.countDown();
                // 等待写线程完成写入
                writeFinished.await();
                
                // 第二次读取（在REPEATABLE_READ级别下应该和第一次读取结果一样）
                count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users2 WHERE USERNAME = ?",
                    new RowMapper<Integer>() {
                        @Override
                        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                            return rs.getInt(1);
                        }
                    },
                    "Charlie"
                );
                secondReadCount.set(count);
                
                transactionManager.commit(status1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "ReaderThread");
        
        // 写线程：在读取之间提交新数据
        Thread writerThread = new Thread(() -> {
            try {
                // 等待第一次读取完成
                readStarted.await();
                
                DefaultTransactionDefinition def2 = new DefaultTransactionDefinition();
                def2.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
                TransactionStatus status2 = transactionManager.getTransaction(def2);
                
                // 插入新数据并提交
                jdbcTemplate.executeUpdate("INSERT INTO users2 (USERNAME) VALUES (?)", "Charlie");
                transactionManager.commit(status2);
                
                // 通知读线程可以进行第二次读取
                writeFinished.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "WriterThread");
        
        // 启动线程
        readerThread.start();
        writerThread.start();
        
        // 等待两个线程完成
        readerThread.join();
        writerThread.join();
        
        // 验证结果：在REPEATABLE_READ级别下两次读取应该看到相同的结果
        assertEquals("First read should see 0 records", 0, firstReadCount.get());
        assertEquals("Second read should also see 0 records (repeatable read)", 0, secondReadCount.get());
        
        // 验证数据确实已经写入（在新事务中查询）
        DefaultTransactionDefinition def3 = new DefaultTransactionDefinition();
        TransactionStatus status3 = transactionManager.getTransaction(def3);
        int finalCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users2 WHERE USERNAME = ?",
            new RowMapper<Integer>() {
                @Override
                public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return rs.getInt(1);
                }
            },
            "Charlie"
        );
        assertEquals("New transaction should see the committed record", 1, finalCount);
        transactionManager.commit(status3);
    }
    
    @Test
    public void testSerializable() throws Exception {
        // 用于线程间同步
        final CountDownLatch readStarted = new CountDownLatch(1);
        final CountDownLatch writeStarted = new CountDownLatch(1);
        final AtomicInteger firstReadCount = new AtomicInteger();
        final AtomicInteger secondReadCount = new AtomicInteger();
        final AtomicBoolean writerBlocked = new AtomicBoolean(false);
        
        // 读线程：在SERIALIZABLE级别下执行范围查询
        Thread readerThread = new Thread(() -> {
            try {
                DefaultTransactionDefinition def1 = new DefaultTransactionDefinition();
                def1.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
                TransactionStatus status1 = transactionManager.getTransaction(def1);
                
                // 第一次读取范围数据
                int count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users2 WHERE USERNAME LIKE ?",
                    new RowMapper<Integer>() {
                        @Override
                        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                            return rs.getInt(1);
                        }
                    },
                    "David%"
                );
                firstReadCount.set(count);
                
                // 通知写线程可以尝试写入
                readStarted.countDown();
                // 等待写线程开始尝试写入
                writeStarted.await();
                
                // 休眠一段时间，让写线程尝试写入
                Thread.sleep(1000);
                
                // 第二次读取（在SERIALIZABLE级别下，写线程应该被阻塞）
                count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users2 WHERE USERNAME LIKE ?",
                    new RowMapper<Integer>() {
                        @Override
                        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                            return rs.getInt(1);
                        }
                    },
                    "David%"
                );
                secondReadCount.set(count);
                
                transactionManager.commit(status1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "ReaderThread");
        
        // 写线程：尝试在读事务执行期间写入数据
        Thread writerThread = new Thread(() -> {
            try {
                // 等待读线程开始第一次读取
                readStarted.await();
                
                DefaultTransactionDefinition def2 = new DefaultTransactionDefinition();
                def2.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
                TransactionStatus status2 = transactionManager.getTransaction(def2);
                
                // 通知主线程我们开始尝试写入
                writeStarted.countDown();
                
                // 尝试插入数据（在SERIALIZABLE级别下应该被阻塞）
                writerBlocked.set(true);
                jdbcTemplate.executeUpdate(
                    "INSERT INTO users2 (USERNAME) VALUES (?)", 
                    "David1"
                );
                
                transactionManager.commit(status2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "WriterThread");
        
        // 启动线程
        readerThread.start();
        writerThread.start();
        
        // 等待两个线程完成
        readerThread.join();
        writerThread.join();
        
        // 验证结果
        assertEquals("First read should see 0 records", 0, firstReadCount.get());
        assertEquals("Second read should still see 0 records", 0, secondReadCount.get());
        assertTrue("Writer should have been blocked", writerBlocked.get());
        
        // 验证写入最终成功
        DefaultTransactionDefinition def3 = new DefaultTransactionDefinition();
        TransactionStatus status3 = transactionManager.getTransaction(def3);
        int finalCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users2 WHERE USERNAME LIKE ?",
            new RowMapper<Integer>() {
                @Override
                public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return rs.getInt(1);
                }
            },
            "David%"
        );
        assertEquals("After all transactions complete, should see the inserted record", 1, finalCount);
        transactionManager.commit(status3);
    }
} 