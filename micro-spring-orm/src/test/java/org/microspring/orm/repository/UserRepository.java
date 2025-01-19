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
    List<User> findByNameLike(String nameLike);
    List<User> findByAgeBetween(int minAge, int maxAge);
    List<User> findByAgeGreaterThan(int age);
    List<User> findByNameLikeAndAgeGreaterThan(String nameLike, int age);
    List<User> findByUsernameIn(List<String> usernames);
    List<User> findByEmailIsNull();
    List<User> findByNameLike(String nameLike, Pageable pageable);
    List<User> findByAgeGreaterThan(int age, Sort sort);
} 