package org.microspring.redis.protocol;

/**
 * Represents a Redis command and provides RESP protocol formatting
 */
public class RedisCommand {
    private final String command;
    private final String[] args;

    public RedisCommand(String command, String... args) {
        this.command = command;
        this.args = args;
    }

    /**
     * Formats the command according to RESP protocol
     * Format: *<number of args + 1>\r\n$<command length>\r\n<command>\r\n$<arg length>\r\n<arg>\r\n...
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        // Total number of parts (command + args)
        sb.append('*').append(1 + args.length).append("\r\n");
        
        // Add command
        appendBulkString(sb, command);
        
        // Add arguments
        for (String arg : args) {
            appendBulkString(sb, arg);
        }
        
        return sb.toString();
    }

    private void appendBulkString(StringBuilder sb, String str) {
        sb.append('$').append(str.length()).append("\r\n")
          .append(str).append("\r\n");
    }

    @Override
    public String toString() {
        return String.format("RedisCommand{command='%s', args=%s}", command, String.join(", ", args));
    }
} 