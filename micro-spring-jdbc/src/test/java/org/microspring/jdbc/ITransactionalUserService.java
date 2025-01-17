package org.microspring.jdbc;

import java.sql.SQLException;

public interface ITransactionalUserService {
    void createUsers(String... names) throws SQLException;
    int countUsers() throws SQLException;
} 