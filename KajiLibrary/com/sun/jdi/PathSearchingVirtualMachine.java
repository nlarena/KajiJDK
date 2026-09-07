package com.sun.jdi;

import java.util.List;

/**
 * Una VM cuyo classpath se puede consultar.
 *
 * @since 1.3
 */
public interface PathSearchingVirtualMachine extends VirtualMachine {

    /**
     * El class path.
     *
     * @return el resultado
     */
    List<String> classPath();

    /**
     * El boot class path.
     *
     * @return el resultado
     */
    List<String> bootClassPath();

    /**
     * El base directory.
     *
     * @return el resultado
     */
    String baseDirectory();
}
