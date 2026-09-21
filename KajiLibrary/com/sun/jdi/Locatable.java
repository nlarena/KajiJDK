package com.sun.jdi;

/**
 * What has a position in the code.
 *
 * @since 1.3
 */
public interface Locatable {

    /**
     * The location.
     *
     * @return the result
     */
    Location location();
}
