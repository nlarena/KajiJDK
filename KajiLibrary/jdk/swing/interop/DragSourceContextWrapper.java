package jdk.swing.interop;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.event.InputEvent;
import java.util.Map;

/**
 * El lado del origen de un arrastre, visto desde otro juego de herramientas graficas.
 *
 * <h2>Que hace</h2>
 *
 * <p>Arrastrar algo fuera de la ventana es una conversacion con el sistema operativo, no con AWT.
 * Esta clase es la mitad de esa conversacion que corresponde al origen: le avisa al sistema que
 * empezo un arrastre, le cambia el cursor mientras dura, y espera a que termine.
 *
 * <h2>Por que un bucle de eventos propio</h2>
 *
 * <p>{@link #startSecondaryEventLoop} y {@link #quitSecondaryEventLoop} existen porque en algunos
 * sistemas el arrastre es una llamada que no vuelve hasta que el usuario suelta. Durante todo ese
 * rato la interfaz tiene que seguir respondiendo, asi que hay que atender eventos desde adentro de
 * la llamada. Eso es un bucle secundario.
 *
 * <h2>{@link #convertModifiersToDropAction}</h2>
 *
 * <p>Es la unica parte que no depende del sistema: la regla de que Control copia, Mayusculas mueve y
 * los dos juntos enlazan, y que sin ninguna de las dos se elige la primera accion que el origen
 * permita, en el orden mover, copiar, enlazar. El resultado siempre se recorta a lo que el origen
 * permita, asi que pedir copiar donde solo se puede mover no da copiar sino nada.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>La regla de las teclas funciona de verdad y es lo unico de este paquete que se puede comprobar
 * contra el JDK sin una pantalla. Lo demas necesita el par nativo, que aca no existe: ver
 * {@link #getDragSourceContext}.
 *
 * @since 9
 */
public abstract class DragSourceContextWrapper {

    private final DragGestureEvent trigger;

    /**
     * Uno para el arrastre que arranco ese gesto.
     *
     * @param dge el gesto que lo arranco
     */
    public DragSourceContextWrapper(DragGestureEvent dge) {
        this.trigger = dge;
    }

    /**
     * Que accion corresponde a esas teclas, recortada a lo que el origen permita.
     *
     * <p>Control copia, Mayusculas mueve, los dos juntos enlazan. Sin ninguna de las dos se elige la
     * primera que el origen permita, en el orden mover, copiar, enlazar --mover primero porque es lo
     * que el usuario espera al arrastrar dentro de la misma aplicacion--. Cualquier otra tecla no
     * cambia nada.
     *
     * @param modifiers las teclas apretadas, como las da {@link InputEvent#getModifiersEx}
     * @param supportedActions las acciones que el origen permite
     * @return la accion, o {@link DnDConstants#ACTION_NONE} si ninguna sirve
     */
    public static int convertModifiersToDropAction(int modifiers, int supportedActions) {
        final int teclas = modifiers & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
        int accion;
        if (teclas == (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) {
            accion = DnDConstants.ACTION_LINK;
        } else if (teclas == InputEvent.CTRL_DOWN_MASK) {
            accion = DnDConstants.ACTION_COPY;
        } else if (teclas == InputEvent.SHIFT_DOWN_MASK) {
            accion = DnDConstants.ACTION_MOVE;
        } else if ((supportedActions & DnDConstants.ACTION_MOVE) != 0) {
            accion = DnDConstants.ACTION_MOVE;
        } else if ((supportedActions & DnDConstants.ACTION_COPY) != 0) {
            accion = DnDConstants.ACTION_COPY;
        } else if ((supportedActions & DnDConstants.ACTION_LINK) != 0) {
            accion = DnDConstants.ACTION_LINK;
        } else {
            accion = DnDConstants.ACTION_NONE;
        }
        return accion & supportedActions;
    }

    /**
     * Pone el cursor que corresponde a lo que va a pasar si el usuario suelta ahi.
     *
     * @param c el cursor
     * @param cursorType que cursor es, en los terminos del sistema
     */
    protected abstract void setNativeCursor(Cursor c, int cursorType);

    /**
     * Le pide al sistema que arranque el arrastre.
     *
     * <p>Los formatos van por separado y no dentro de los datos porque el sistema los publica antes
     * de que nadie pida nada: quien esta del otro lado tiene que poder decidir si acepta el arrastre
     * sin transferir todavia.
     *
     * @param t los datos que se arrastran
     * @param formats los formatos, en los terminos del sistema
     * @param formatMap de cada formato del sistema al formato de AWT que le corresponde
     */
    protected abstract void startDrag(Transferable t, long[] formats,
            Map<Long, DataFlavor> formatMap);

    /** Atiende eventos hasta que el arrastre termine. */
    public abstract void startSecondaryEventLoop();

    /** Corta el bucle que abrio {@link #startSecondaryEventLoop}. */
    public abstract void quitSecondaryEventLoop();

    /**
     * Avisa que el arrastre termino y espera a que el aviso llegue.
     *
     * <p>El aviso va por la cola de eventos y no directo porque quien lo escucha es codigo de la
     * aplicacion, y ese corre en el hilo de eventos. Despues de encolarlo se entra en el bucle
     * secundario, que es lo que le da al hilo de eventos la chance de atenderlo antes de que esta
     * llamada vuelva.
     *
     * @param success si el destino se quedo con los datos
     * @param operations que se hizo con ellos
     * @param x donde se solto, en pantalla
     * @param y donde se solto, en pantalla
     * @throws IllegalArgumentException si el arrastre nunca arranco, y por lo tanto no hay contexto
     *     al que avisarle
     */
    public void dragDropFinished(boolean success, int operations, int x, int y) {
        final DragSourceDropEvent ev = new DragSourceDropEvent(getDragSourceContext(),
                operations & accionesDelOrigen(), success, x, y);
        EventQueue.invokeLater(new Aviso(ev));
        startSecondaryEventLoop();
    }

    /**
     * El contexto del arrastre en curso.
     *
     * <p>Devuelve {@code null} mientras no haya un arrastre andando, que en esta biblioteca es
     * siempre: el contexto lo llena {@code DragSource.startDrag} al llegar al par nativo, y par
     * nativo no hay. El JDK devuelve lo mismo para un envoltorio recien creado.
     *
     * @return el contexto, o {@code null}
     */
    public DragSourceContext getDragSourceContext() {
        return null;
    }

    /** Que acciones permite el origen del gesto, para recortar lo que se informa al soltar. */
    private int accionesDelOrigen() {
        if (this.trigger == null) {
            return DnDConstants.ACTION_NONE;
        }
        final DragGestureRecognizer r = this.trigger.getSourceAsDragGestureRecognizer();
        return r == null ? DnDConstants.ACTION_NONE : r.getSourceActions();
    }

    /** El aviso de fin de arrastre, para correrlo en el hilo de eventos. */
    private static final class Aviso implements Runnable {

        private final DragSourceDropEvent ev;

        Aviso(DragSourceDropEvent ev) {
            this.ev = ev;
        }

        public void run() {
            this.ev.getDragSourceContext().dragDropEnd(this.ev);
        }
    }
}
