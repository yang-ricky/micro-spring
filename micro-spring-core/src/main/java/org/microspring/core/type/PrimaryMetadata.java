package org.microspring.core.type;

/**
 * Interface to be implemented by bean metadata that carries primary information.
 */
public interface PrimaryMetadata {
    /**
     * Return whether the bean should be considered a primary candidate.
     */
    boolean isPrimary();
} 