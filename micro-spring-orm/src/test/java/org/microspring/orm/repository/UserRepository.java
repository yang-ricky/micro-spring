package org.microspring.orm.repository;


import org.microspring.orm.entity.User;

@OrmRepository
public interface UserRepository extends CrudRepository<User, Long> {
} 