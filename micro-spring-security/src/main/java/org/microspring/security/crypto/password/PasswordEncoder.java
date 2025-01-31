package org.microspring.security.crypto.password;

/**
 * 密码编码器接口
 */
public interface PasswordEncoder {
    /**
     * 对原始密码进行编码
     *
     * @param rawPassword 原始密码
     * @return 编码后的密码
     */
    String encode(String rawPassword);

    /**
     * 验证原始密码是否匹配已编码的密码
     *
     * @param rawPassword 原始密码
     * @param encodedPassword 已编码的密码
     * @return 是否匹配
     */
    boolean matches(String rawPassword, String encodedPassword);
} 