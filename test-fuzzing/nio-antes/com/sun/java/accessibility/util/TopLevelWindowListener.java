package com.sun.java.accessibility.util;

import java.awt.Window;
import java.util.EventListener;

/**
 * It finds out when a top-level window appears or disappears.
 *
 * <h2>Why only the top-level ones</h2>
 *
 * <p>Because they are the roots of the accessibility tree. An assistive technology walks from
 * there downwards, so finding out about the windows is enough in order to know what trees there
 * are -- and subscribing to each component would be impossible in an application of a real
 * size.
 *
 * <p>The notice of destruction matters as much as the one of creation: without it, a screen
 * reader would keep references to trees that no longer exist.
 */
public interface TopLevelWindowListener extends EventListener {

    /** A top-level window appeared. */
    void topLevelWindowCreated(Window w);

    /** One disappeared. */
    void topLevelWindowDestroyed(Window w);
}
