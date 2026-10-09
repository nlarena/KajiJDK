package com.sun.java.accessibility.util;

import java.awt.Component;

/**
 * It listens to AWT events of <strong>any</strong> component, without registering with any.
 *
 * <h2>What it resolves</h2>
 *
 * <p>The normal registration is per component: {@code button.addActionListener(...)}. An
 * assistive technology cannot do that -- it does not know the components of an application it
 * did not write, and walking them in order to register with each one would fail with those that
 * are created afterwards.
 *
 * <p>This class turns it around: it hooks itself to the process's event queue via
 * {@link EventQueueMonitor} and hands out to whoever has registered here. A listener put in once
 * receives the events of all the components, present and future.
 *
 * <h2>The cost, which it is worth keeping in mind</h2>
 *
 * <p>Every event of the process goes through this handing out. {@code MouseMotion} in particular
 * arrives hundreds of times per second, and that is why it is separated from {@code Mouse}:
 * registering for it costs much more than for the others.
 *
 * <p>See {@link SwingEventMonitor} for the events of Swing's own.
 */
public class AWTEventMonitor {

    /** Shared with {@link SwingEventMonitor}, which inherits from this class. */
    protected static final AccessibilityListenerList listenerList =
            new AccessibilityListenerList();

    public AWTEventMonitor() {
    }

    /**
     * The component that has the focus, or {@code null}.
     *
     * <p>It comes out of the focused window {@link EventQueueMonitor} knows about: it is the datum
     * that answers "where is the user standing".
     */
    public static Component getComponentWithFocus() {
        EventQueueMonitor.maybeInitialize();
        java.awt.Window w = EventQueueMonitor.getTopLevelWindowWithFocus();
        return w == null ? null : w.getFocusOwner();
    }

    /** It listens to the {@code Component}-type events of any component. */
    public static void addComponentListener(java.awt.event.ComponentListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.ComponentListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeComponentListener(java.awt.event.ComponentListener l) {
        listenerList.remove(java.awt.event.ComponentListener.class, l);
    }

    /** It listens to the {@code Container}-type events of any component. */
    public static void addContainerListener(java.awt.event.ContainerListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.ContainerListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeContainerListener(java.awt.event.ContainerListener l) {
        listenerList.remove(java.awt.event.ContainerListener.class, l);
    }

    /** It listens to the {@code Focus}-type events of any component. */
    public static void addFocusListener(java.awt.event.FocusListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.FocusListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeFocusListener(java.awt.event.FocusListener l) {
        listenerList.remove(java.awt.event.FocusListener.class, l);
    }

    /** It listens to the {@code Key}-type events of any component. */
    public static void addKeyListener(java.awt.event.KeyListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.KeyListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeKeyListener(java.awt.event.KeyListener l) {
        listenerList.remove(java.awt.event.KeyListener.class, l);
    }

    /** It listens to the {@code Mouse}-type events of any component. */
    public static void addMouseListener(java.awt.event.MouseListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.MouseListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeMouseListener(java.awt.event.MouseListener l) {
        listenerList.remove(java.awt.event.MouseListener.class, l);
    }

    /** It listens to the {@code MouseMotion}-type events of any component. */
    public static void addMouseMotionListener(java.awt.event.MouseMotionListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.MouseMotionListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeMouseMotionListener(java.awt.event.MouseMotionListener l) {
        listenerList.remove(java.awt.event.MouseMotionListener.class, l);
    }

    /** It listens to the {@code Window}-type events of any component. */
    public static void addWindowListener(java.awt.event.WindowListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.WindowListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeWindowListener(java.awt.event.WindowListener l) {
        listenerList.remove(java.awt.event.WindowListener.class, l);
    }

    /** It listens to the {@code Action}-type events of any component. */
    public static void addActionListener(java.awt.event.ActionListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.ActionListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeActionListener(java.awt.event.ActionListener l) {
        listenerList.remove(java.awt.event.ActionListener.class, l);
    }

    /** It listens to the {@code Adjustment}-type events of any component. */
    public static void addAdjustmentListener(java.awt.event.AdjustmentListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.AdjustmentListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeAdjustmentListener(java.awt.event.AdjustmentListener l) {
        listenerList.remove(java.awt.event.AdjustmentListener.class, l);
    }

    /** It listens to the {@code Item}-type events of any component. */
    public static void addItemListener(java.awt.event.ItemListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.ItemListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeItemListener(java.awt.event.ItemListener l) {
        listenerList.remove(java.awt.event.ItemListener.class, l);
    }

    /** It listens to the {@code Text}-type events of any component. */
    public static void addTextListener(java.awt.event.TextListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.TextListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeTextListener(java.awt.event.TextListener l) {
        listenerList.remove(java.awt.event.TextListener.class, l);
    }
}
