package org.microspring.orm.repository;

import org.microspring.orm.entity.User;
import java.util.List;

@OrmRepository
public interface UserRepository extends CrudRepository<User, Long> {
    List<User> findByName(String name);
    List<User> findByUsername(String username);
} 