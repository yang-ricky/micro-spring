package org.microspring.security.config;

import org.microspring.security.core.Authentication;
import org.microspring.security.core.GrantedAuthority;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 安全规则类
 */
public class SecurityRule {
    private final String[] patterns;
    private final boolean isRegex;
    private final List<Pattern> compiledPatterns;
    
    private Set<String> requiredRoles = new HashSet<>();
    private Set<String> requiredAuthorities = new HashSet<>();
    private String requiredIpPattern;
    private boolean requireAuthenticated;
    private boolean permitAll;

    public SecurityRule(String[] patterns, boolean isRegex) {
        this.patterns = patterns;
        this.isRegex = isRegex;
        this.compiledPatterns = isRegex ? 
            Arrays.stream(patterns).map(Pattern::compile).collect(Collectors.toList()) :
            new ArrayList<>();
    }

    public void setRequiredRole(String role) {
        this.requiredRoles = Collections.singleton(role);
    }

    public void setRequiredRoles(String[] roles) {
        this.requiredRoles = new HashSet<>(Arrays.asList(roles));
    }

    public void setRequiredAuthority(String authority) {
        this.requiredAuthorities = Collections.singleton(authority);
    }

    public void setRequiredIpPattern(String ipPattern) {
        this.requiredIpPattern = ipPattern;
    }

    public void setRequireAuthenticated(boolean requireAuthenticated) {
        this.requireAuthenticated = requireAuthenticated;
    }

    public void setPermitAll(boolean permitAll) {
        this.permitAll = permitAll;
    }

    public boolean matches(String path) {
        if (isRegex) {
            return compiledPatterns.stream().anyMatch(p -> p.matcher(path).matches());
        }
        return Arrays.stream(patterns).anyMatch(pattern -> pathMatches(pattern, path));
    }

    private boolean pathMatches(String pattern, String path) {
        // 简单的ant风格路径匹配
        String[] patternParts = pattern.split("/");
        String[] pathParts = path.split("/");
        
        if (!pattern.endsWith("/**") && patternParts.length != pathParts.length) {
            return false;
        }

        for (int i = 0; i < patternParts.length && i < pathParts.length; i++) {
            String patternPart = patternParts[i];
            String pathPart = pathParts[i];
            
            if (patternPart.equals("**")) {
                return true;
            }
            if (!patternPart.equals("*") && !patternPart.equals(pathPart)) {
                return false;
            }
        }
        
        return true;
    }

    public boolean checkAccess(Authentication authentication, String remoteAddr) {
        if (permitAll) {
            return true;
        }

        if (requireAuthenticated && !authentication.isAuthenticated()) {
            return false;
        }

        if (!requiredRoles.isEmpty() && !hasAnyRole(authentication)) {
            return false;
        }

        if (!requiredAuthorities.isEmpty() && !hasAnyAuthority(authentication)) {
            return false;
        }

        if (requiredIpPattern != null && !checkIpAddress(remoteAddr)) {
            return false;
        }

        return true;
    }

    private boolean hasAnyRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(requiredRoles::contains);
    }

    private boolean hasAnyAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(requiredAuthorities::contains);
    }

    private boolean checkIpAddress(String remoteAddr) {
        // 简单的IP地址匹配，支持CIDR
        if (requiredIpPattern.contains("/")) {
            return checkIpRange(remoteAddr, requiredIpPattern);
        }
        return requiredIpPattern.equals(remoteAddr);
    }

    private boolean checkIpRange(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            String network = parts[0];
            int prefix = Integer.parseInt(parts[1]);

            byte[] networkBytes = ipToBytes(network);
            byte[] ipBytes = ipToBytes(ip);
            
            int mask = -1 << (32 - prefix);
            
            int network1 = ((networkBytes[0] & 0xFF) << 24) |
                          ((networkBytes[1] & 0xFF) << 16) |
                          ((networkBytes[2] & 0xFF) << 8)  |
                          (networkBytes[3] & 0xFF);
            
            int ip1 = ((ipBytes[0] & 0xFF) << 24) |
                     ((ipBytes[1] & 0xFF) << 16) |
                     ((ipBytes[2] & 0xFF) << 8)  |
                     (ipBytes[3] & 0xFF);
            
            return (network1 & mask) == (ip1 & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] ipToBytes(String ip) {
        String[] parts = ip.split("\\.");
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) Integer.parseInt(parts[i]);
        }
        return bytes;
    }
} 