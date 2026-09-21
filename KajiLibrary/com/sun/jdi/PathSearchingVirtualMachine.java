package com.sun.jdi;

import java.util.List;

/**
 * A VM whose classpath can be consulted.
 *
 * @since 1.3
 */
public interface PathSearchingVirtualMachine extends VirtualMachine {

    /**
     * The class path.
     *
     * @return the result
     */
    List<String> classPath();

    /**
     * The boot class path.
     *
     * @return the result
     */
    List<String> bootClassPath();

    /**
     * The base directory.
     *
     * @return the result
     */
    String baseDirectory();
}
