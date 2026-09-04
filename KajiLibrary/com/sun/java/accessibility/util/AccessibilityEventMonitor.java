package com.sun.java.accessibility.util;

import java.beans.PropertyChangeListener;

/**
 * Escucha los cambios de propiedad de <strong>los objetos accesibles</strong>, no de los
 * componentes.
 *
 * <h2>En que se diferencia de los otros dos monitores</h2>
 *
 * <p>{@link AWTEventMonitor} y {@link SwingEventMonitor} escuchan eventos de la interfaz: un clic,
 * una tecla, un cambio de foco. Este escucha el <strong>arbol de accesibilidad</strong>, que es otra
 * cosa: que cambio el nombre accesible de algo, que un elemento paso a estar deshabilitado, que
 * cambio la seleccion.
 *
 * <p>La distincion importa porque no hay correspondencia uno a uno. Un solo evento de interfaz puede
 * cambiar varias propiedades accesibles, y una propiedad puede cambiar sin ningun evento de interfaz
 * — cuando el programa la modifica directamente.
 *
 * <p>Un lector de pantalla necesita los dos: aquellos para saber que hizo el usuario, este para
 * saber que cambio en lo que hay para leer.
 */
public class AccessibilityEventMonitor {

    /** La lista compartida, con el mismo formato que la de los otros monitores. */
    protected static final AccessibilityListenerList listenerList =
            new AccessibilityListenerList();

    public AccessibilityEventMonitor() {
    }

    /** Escucha los cambios de propiedad de cualquier objeto accesible. */
    public static void addPropertyChangeListener(PropertyChangeListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(PropertyChangeListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removePropertyChangeListener(PropertyChangeListener l) {
        listenerList.remove(PropertyChangeListener.class, l);
    }
}
