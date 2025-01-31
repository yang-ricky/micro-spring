package org.microspring.security.core;

/**
 * 用户详细信息服务接口
 */
public interface UserDetailsService {
    /**
     * 根据用户名加载用户信息
     *
     * @param username 用户名
     * @return 用户详细信息
     * @throws UsernameNotFoundException 如果用户不存在
     */
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
} 