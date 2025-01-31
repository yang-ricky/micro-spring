package org.microspring.security.crypto.password;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt密码编码器实现
 * 注意：不要在生产环境使用MD5等弱加密算法
 */
public class BCryptPasswordEncoder implements PasswordEncoder {
    private final int strength;

    /**
     * 创建默认强度(10)的BCrypt密码编码器
     */
    public BCryptPasswordEncoder() {
        this(10);
    }

    /**
     * 创建指定强度的BCrypt密码编码器
     *
     * @param strength BCrypt强度，范围4-31，越大越安全但也越慢
     */
    public BCryptPasswordEncoder(int strength) {
        if (strength < 4 || strength > 31) {
            throw new IllegalArgumentException("BCrypt strength must be between 4 and 31");
        }
        this.strength = strength;
    }

    @Override
    public String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Raw password cannot be null");
        }
        String salt = BCrypt.gensalt(strength);
        return BCrypt.hashpw(rawPassword, salt);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, encodedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
} 