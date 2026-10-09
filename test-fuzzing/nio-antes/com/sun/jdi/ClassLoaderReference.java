package com.sun.jdi;

import java.util.List;

/**
 * A class loader of the debugged machine.
 *
 * @since 1.3
 */
public interface ClassLoaderReference extends ObjectReference {

    /**
     * The defined classes.
     *
     * @return the result
     */
    List<ReferenceType> definedClasses();

    /**
     * The visible classes.
     *
     * @return the result
     */
    List<ReferenceType> visibleClasses();
}
