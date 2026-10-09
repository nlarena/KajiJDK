package com.sun.java.accessibility.util;

import java.util.EventListener;

/**
 * It finds out when the graphical interface already exists.
 *
 * <h2>Why it is necessary to wait</h2>
 *
 * <p>An assistive technology -- a screen reader -- starts <strong>before</strong> the
 * application has windows: the VM loads it at the beginning, and at that moment there is nothing
 * to read. Consulting the interface there gives empty, and consulting again in a loop is wasting
 * time.
 *
 * <p>This notice resolves that: it arrives only once, when the first top-level window appears.
 * Registering after that has already happened gives nothing, and that is why it is convenient to
 * consult {@link EventQueueMonitor#isGUIInitialized} first.
 */
public interface GUIInitializedListener extends EventListener {

    /** There is a graphical interface now. */
    void guiInitialized();
}
