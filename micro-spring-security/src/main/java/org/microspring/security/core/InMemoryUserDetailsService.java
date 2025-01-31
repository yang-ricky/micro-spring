package org.microspring.security.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UserDetailsService的内存实现
 */
public class InMemoryUserDetailsService implements UserDetailsService {
    private final Map<String, UserDetails> users = new ConcurrentHashMap<>();

    public void createUser(UserDetails user) {
        users.put(user.getUsername(), user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = users.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return user;
    }
} 