package org.microspring.jdbc;

import org.microspring.transaction.annotation.Transactional;
import org.microspring.stereotype.Component;
import java.sql.SQLException;
import org.microspring.beans.factory.annotation.Autowired;
import java.sql.ResultSet;
import org.microspring.jdbc.RowMapper;
import org.microspring.beans.factory.annotation.Qualifier;

@Component("transactionalUserService")
@Transactional
public class TransactionalUserService implements ITransactionalUserService {
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public TransactionalUserService(@Qualifier("testJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public void createUsers(String... usernames) throws SQLException {
        for (String username : usernames) {
            if ("error".equals(username)) {
                throw new RuntimeException("Simulated error");
            }
            jdbcTemplate.executeUpdate("INSERT INTO users1 (USERNAME) VALUES (?)", username);
        }
    }
    
    @Override
    public int countUsers() throws SQLException {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users1",
            new RowMapper<Integer>() {
                @Override
                public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return rs.getInt(1);
                }
            }
        );
    }
} 