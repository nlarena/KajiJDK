package com.sun.java.accessibility.util;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.accessibility.Accessible;

/**
 * El observador global de la cola de eventos de AWT: sabe que ventanas hay y donde esta el mouse.
 *
 * <h2>Como puede saber eso sin que nadie lo registre</h2>
 *
 * <p>Con {@code Toolkit.addAWTEventListener}, que es un enganche a la cola de eventos
 * <strong>de todo el proceso</strong>. Cada evento de AWT pasa por aca antes de llegar a su
 * componente.
 *
 * <p>Es un privilegio grande y por eso esta protegido: en el JDK requiere permiso. Lo que compra es
 * lo unico que hace posible una tecnologia de asistencia — enterarse de una aplicacion que no fue
 * escrita para colaborar con ella.
 *
 * <h2>Todo estatico, y por que</h2>
 *
 * <p>Porque hay una sola cola de eventos por proceso. Dos monitores serian dos enganches al mismo
 * flujo, duplicando cada notificacion. La clase se puede instanciar —el JDK deja el constructor
 * publico— pero el estado es uno solo.
 *
 * <h2>En esta VM</h2>
 *
 * <p>El enganche se instala igual, y las consultas contestan lo que el estado tenga. Como esta VM no
 * corre una interfaz grafica real, ese estado queda vacio: {@link #getTopLevelWindows} devuelve un
 * arreglo sin elementos y {@link #isGUIInitialized} contesta {@code false}. No es un stub — es el
 * mecanismo funcionando sobre un escritorio que no existe.
 */
public class EventQueueMonitor implements AWTEventListener {

    private static final List<Window> ventanas = new ArrayList<Window>();
    private static final List<GUIInitializedListener> oyentesGui =
            new ArrayList<GUIInitializedListener>();
    private static final List<TopLevelWindowListener> oyentesVentana =
            new ArrayList<TopLevelWindowListener>();

    private static Window conFoco;
    private static Point posicionMouse;
    private static boolean guiInicializada = false;
    private static boolean enganchado = false;

    public EventQueueMonitor() {
    }

    /**
     * Instala el enganche a la cola de eventos, si no estaba.
     *
     * <p>Lo llaman todos los metodos que necesitan estado, en vez de hacerlo en un inicializador
     * estatico: cargar esta clase no deberia enganchar nada por su cuenta.
     */
    public static void maybeInitialize() {
        synchronized (EventQueueMonitor.class) {
            if (enganchado) {
                return;
            }
            enganchado = true;
        }
        try {
            java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(
                    new EventQueueMonitor(), AWTEvent.WINDOW_EVENT_MASK
                    | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
                    | AWTEvent.COMPONENT_EVENT_MASK);
        } catch (RuntimeException e) {
            // Sin escritorio no hay a que engancharse, y eso no es un error: las consultas
            // simplemente van a contestar vacio. Dejar que la excepcion suba haria que cargar esta
            // clase reventara en un entorno sin interfaz grafica.
            return;
        }
    }

    /** Recibe cada evento de AWT del proceso. */
    public void eventDispatched(AWTEvent theEvent) {
        if (theEvent instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) theEvent;
            synchronized (EventQueueMonitor.class) {
                posicionMouse = new Point(me.getX(), me.getY());
            }
        }
        if (theEvent.getSource() instanceof Window) {
            Window w = (Window) theEvent.getSource();
            int id = theEvent.getID();
            if (id == java.awt.event.WindowEvent.WINDOW_OPENED) {
                agregarVentana(w);
            } else if (id == java.awt.event.WindowEvent.WINDOW_CLOSED) {
                sacarVentana(w);
            } else if (id == java.awt.event.WindowEvent.WINDOW_ACTIVATED) {
                synchronized (EventQueueMonitor.class) {
                    conFoco = w;
                }
            }
        }
    }

    private static void agregarVentana(Window w) {
        List<TopLevelWindowListener> avisar;
        boolean primera = false;
        synchronized (EventQueueMonitor.class) {
            if (ventanas.contains(w)) {
                return;
            }
            ventanas.add(w);
            if (!guiInicializada) {
                guiInicializada = true;
                primera = true;
            }
            avisar = new ArrayList<TopLevelWindowListener>(oyentesVentana);
        }
        // Los avisos van FUERA del bloque sincronizado: un oyente que vuelva a llamar a esta clase
        // desde su propio hilo se trabaria con el candado tomado.
        if (primera) {
            avisarGuiInicializada();
        }
        for (int i = 0; i < avisar.size(); i++) {
            avisar.get(i).topLevelWindowCreated(w);
        }
    }

    private static void sacarVentana(Window w) {
        List<TopLevelWindowListener> avisar;
        synchronized (EventQueueMonitor.class) {
            if (!ventanas.remove(w)) {
                return;
            }
            if (conFoco == w) {
                conFoco = null;
            }
            avisar = new ArrayList<TopLevelWindowListener>(oyentesVentana);
        }
        for (int i = 0; i < avisar.size(); i++) {
            avisar.get(i).topLevelWindowDestroyed(w);
        }
    }

    private static void avisarGuiInicializada() {
        List<GUIInitializedListener> avisar;
        synchronized (EventQueueMonitor.class) {
            avisar = new ArrayList<GUIInitializedListener>(oyentesGui);
            oyentesGui.clear();
        }
        for (int i = 0; i < avisar.size(); i++) {
            avisar.get(i).guiInitialized();
        }
    }

    /**
     * El objeto accesible que esta en ese punto de la pantalla, o {@code null}.
     *
     * <p>Es la consulta central de una tecnologia de asistencia: "que hay debajo del cursor".
     */
    public static Accessible getAccessibleAt(Point p) {
        maybeInitialize();
        Window[] ws = getTopLevelWindows();
        for (int i = 0; i < ws.length; i++) {
            Component c = componenteEn(ws[i], p);
            if (c instanceof Accessible) {
                return (Accessible) c;
            }
        }
        return null;
    }

    /** El componente visible mas profundo que contiene ese punto. */
    private static Component componenteEn(Container c, Point p) {
        if (c == null || !c.isShowing() || !c.getBounds().contains(p)) {
            return null;
        }
        Component[] hijos = c.getComponents();
        // De adelante hacia atras: el primero que contiene el punto es el que se ve.
        for (int i = 0; i < hijos.length; i++) {
            if (hijos[i] instanceof Container) {
                Component hallado = componenteEn((Container) hijos[i], p);
                if (hallado != null) {
                    return hallado;
                }
            } else if (hijos[i].isShowing() && hijos[i].getBounds().contains(p)) {
                return hijos[i];
            }
        }
        return c;
    }

    /** Si ya aparecio alguna ventana. */
    public static boolean isGUIInitialized() {
        maybeInitialize();
        synchronized (EventQueueMonitor.class) {
            return guiInicializada;
        }
    }

    /**
     * Avisa cuando aparezca la interfaz grafica.
     *
     * <p>Si ya aparecio, el aviso llega <strong>enseguida</strong> y no se guarda al oyente: es un
     * evento que pasa una sola vez, y guardarlo para un aviso que no va a repetirse seria una fuga.
     */
    public static void addGUIInitializedListener(GUIInitializedListener l) {
        maybeInitialize();
        boolean ya;
        synchronized (EventQueueMonitor.class) {
            ya = guiInicializada;
            if (!ya) {
                oyentesGui.add(l);
            }
        }
        if (ya) {
            l.guiInitialized();
        }
    }

    /** Saca un oyente de inicializacion. */
    public static void removeGUIInitializedListener(GUIInitializedListener l) {
        synchronized (EventQueueMonitor.class) {
            oyentesGui.remove(l);
        }
    }

    /** Avisa cuando aparezca o desaparezca una ventana de primer nivel. */
    public static void addTopLevelWindowListener(TopLevelWindowListener l) {
        maybeInitialize();
        synchronized (EventQueueMonitor.class) {
            oyentesVentana.add(l);
        }
    }

    /** Saca un oyente de ventanas. */
    public static void removeTopLevelWindowListener(TopLevelWindowListener l) {
        synchronized (EventQueueMonitor.class) {
            oyentesVentana.remove(l);
        }
    }

    /** Donde estaba el mouse la ultima vez que se lo vio, o {@code null}. */
    public static Point getCurrentMousePosition() {
        maybeInitialize();
        synchronized (EventQueueMonitor.class) {
            return posicionMouse == null ? null : new Point(posicionMouse);
        }
    }

    /** Las ventanas de primer nivel que hay ahora. */
    public static Window[] getTopLevelWindows() {
        maybeInitialize();
        synchronized (EventQueueMonitor.class) {
            return ventanas.toArray(new Window[ventanas.size()]);
        }
    }

    /** La que tiene el foco, o {@code null}. */
    public static Window getTopLevelWindowWithFocus() {
        maybeInitialize();
        synchronized (EventQueueMonitor.class) {
            return conFoco;
        }
    }
}
