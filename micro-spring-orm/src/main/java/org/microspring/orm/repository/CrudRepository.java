package org.microspring.orm.repository;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public interface CrudRepository<T, ID extends Serializable> extends Repository<T, ID> {
    
    T save(T entity);
    
    Optional<T> findById(ID id);
    
    boolean existsById(ID id);
    
    List<T> findAll();
    
    long count();
    
    void deleteById(ID id);
    
    void delete(T entity);
    
    void deleteAll();
    
    // 批量保存
    List<T> saveAll(List<T> entities);
    
    // 批量删除
    void deleteAll(List<T> entities);
    
    // 批量查询
    List<T> findAllById(List<ID> ids);
} 