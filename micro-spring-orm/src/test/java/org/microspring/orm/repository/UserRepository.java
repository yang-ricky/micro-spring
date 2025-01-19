package org.microspring.orm.repository;

import org.microspring.orm.entity.User;
import java.util.List;

@OrmRepository
public interface UserRepository extends CrudRepository<User, Long> {
    List<User> findByName(String name);
    List<User> findByUsername(String username);
    List<User> findByNameAndUsername(String name, String username);
    List<User> findByNameOrUsername(String name, String username);
    List<User> findByUsernameAndNameOrId(String username, String name, Long id);
} 