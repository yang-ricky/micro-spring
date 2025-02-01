package org.microspring.redis.protocol;

/**
 * Enum representing different Redis response types according to RESP protocol
 */
public enum ResponseType {
    SIMPLE_STRING,  // For +OK\r\n
    ERROR,         // For -ERR message\r\n
    INTEGER,       // For :1000\r\n
    BULK_STRING,   // For $3\r\nfoo\r\n
    ARRAY          // For *2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n
} 