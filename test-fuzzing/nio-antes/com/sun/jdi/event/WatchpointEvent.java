package com.sun.jdi.event;

import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;

/**
 * A field that was being watched was touched.
 *
 * <p>{@link #valueCurrent} is the value it had; in the modification variant there is also the one
 * that is going to be left.
 *
 * @since 1.3
 */
public interface WatchpointEvent extends LocatableEvent {

    /**
     * The field.
     *
     * @return the result
     */
    Field field();

    /**
     * The object.
     *
     * @return the result
     */
    ObjectReference object();

    /**
     * The value current.
     *
     * @return the result
     */
    Value valueCurrent();
}
