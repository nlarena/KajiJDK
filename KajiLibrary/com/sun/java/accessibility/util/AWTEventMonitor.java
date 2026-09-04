package com.sun.java.accessibility.util;

import java.awt.Component;

/**
 * Escucha eventos de AWT de <strong>cualquier</strong> componente, sin registrarse en ninguno.
 *
 * <h2>Que resuelve</h2>
 *
 * <p>El registro normal es por componente: {@code boton.addActionListener(...)}. Una tecnologia de
 * asistencia no puede hacer eso — no conoce los componentes de una aplicacion que no escribio, y
 * recorrerlos para registrarse en cada uno fallaria con los que se creen despues.
 *
 * <p>Esta clase da la vuelta: se engancha a la cola de eventos del proceso via
 * {@link EventQueueMonitor} y reparte a quien se haya registrado aca. Un oyente puesto una vez
 * recibe los eventos de todos los componentes, presentes y futuros.
 *
 * <h2>El costo, que conviene tener presente</h2>
 *
 * <p>Todo evento del proceso pasa por este reparto. {@code MouseMotion} en particular llega
 * centenares de veces por segundo, y por eso esta separado de {@code Mouse}: registrarse en el
 * cuesta mucho mas que en los demas.
 *
 * <p>Ver {@link SwingEventMonitor} para los eventos propios de Swing.
 */
public class AWTEventMonitor {

    /** Compartida con {@link SwingEventMonitor}, que hereda de esta clase. */
    protected static final AccessibilityListenerList listenerList =
            new AccessibilityListenerList();

    public AWTEventMonitor() {
    }

    /**
     * El componente que tiene el foco, o {@code null}.
     *
     * <p>Sale de la ventana con foco que conoce {@link EventQueueMonitor}: es el dato que responde
     * "donde esta parado el usuario".
     */
    public static Component getComponentWithFocus() {
        EventQueueMonitor.maybeInitialize();
        java.awt.Window w = EventQueueMonitor.getTopLevelWindowWithFocus();
        return w == null ? null : w.getFocusOwner();
    }

    /** Escucha los eventos de tipo {@code Component} de cualquier componente. */
    public static void addComponentListener(java.awt.event.ComponentListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.ComponentListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeComponentListener(java.awt.event.ComponentListener l) {
        listenerList.remove(java.awt.event.ComponentListener.class, l);
    }

    /** Escucha los eventos de tipo {@code Container} de cualquier componente. */
    public static void addContainerListener(java.awt.event.ContainerListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.ContainerListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeContainerListener(java.awt.event.ContainerListener l) {
        listenerList.remove(java.awt.event.ContainerListener.class, l);
    }

    /** Escucha los eventos de tipo {@code Focus} de cualquier componente. */
    public static void addFocusListener(java.awt.event.FocusListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.FocusListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeFocusListener(java.awt.event.FocusListener l) {
        listenerList.remove(java.awt.event.FocusListener.class, l);
    }

    /** Escucha los eventos de tipo {@code Key} de cualquier componente. */
    public static void addKeyListener(java.awt.event.KeyListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.KeyListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeKeyListener(java.awt.event.KeyListener l) {
        listenerList.remove(java.awt.event.KeyListener.class, l);
    }

    /** Escucha los eventos de tipo {@code Mouse} de cualquier componente. */
    public static void addMouseListener(java.awt.event.MouseListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.MouseListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeMouseListener(java.awt.event.MouseListener l) {
        listenerList.remove(java.awt.event.MouseListener.class, l);
    }

    /** Escucha los eventos de tipo {@code MouseMotion} de cualquier componente. */
    public static void addMouseMotionListener(java.awt.event.MouseMotionListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.MouseMotionListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeMouseMotionListener(java.awt.event.MouseMotionListener l) {
        listenerList.remove(java.awt.event.MouseMotionListener.class, l);
    }

    /** Escucha los eventos de tipo {@code Window} de cualquier componente. */
    public static void addWindowListener(java.awt.event.WindowListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.WindowListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeWindowListener(java.awt.event.WindowListener l) {
        listenerList.remove(java.awt.event.WindowListener.class, l);
    }

    /** Escucha los eventos de tipo {@code Action} de cualquier componente. */
    public static void addActionListener(java.awt.event.ActionListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.ActionListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeActionListener(java.awt.event.ActionListener l) {
        listenerList.remove(java.awt.event.ActionListener.class, l);
    }

    /** Escucha los eventos de tipo {@code Adjustment} de cualquier componente. */
    public static void addAdjustmentListener(java.awt.event.AdjustmentListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.AdjustmentListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeAdjustmentListener(java.awt.event.AdjustmentListener l) {
        listenerList.remove(java.awt.event.AdjustmentListener.class, l);
    }

    /** Escucha los eventos de tipo {@code Item} de cualquier componente. */
    public static void addItemListener(java.awt.event.ItemListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.ItemListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeItemListener(java.awt.event.ItemListener l) {
        listenerList.remove(java.awt.event.ItemListener.class, l);
    }

    /** Escucha los eventos de tipo {@code Text} de cualquier componente. */
    public static void addTextListener(java.awt.event.TextListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.awt.event.TextListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeTextListener(java.awt.event.TextListener l) {
        listenerList.remove(java.awt.event.TextListener.class, l);
    }
}
