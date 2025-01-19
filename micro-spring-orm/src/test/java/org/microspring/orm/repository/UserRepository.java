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
    @Query("from User where name like ?1 and age > ?2")
    List<User> findUsersByCustomQuery(String namePattern, int minAge);
    @Query("from User u where u.age > ?1 order by u.age desc")
    List<User> findOldestUsers(int minAge);
    @Query("from User u where u.age between ?1 and ?2")
    List<User> findUsersByAgeRange(int minAge, int maxAge, Pageable pageable);
} 