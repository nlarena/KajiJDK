package javax.swing.undo;

import java.util.Vector;

import javax.swing.event.UndoableEditListener;
import javax.swing.event.UndoableEditEvent;

/**
 * El lado emisor: junta oyentes y les reparte las ediciones.
 *
 * <h2>Que agrega sobre una lista de oyentes</h2>
 *
 * <p>El <strong>agrupamiento</strong>. Entre {@link #beginUpdate} y {@link #endUpdate} las ediciones
 * no se reparten: se acumulan en un {@link CompoundEdit} y al cerrar sale <em>una sola</em>. Es como
 * una operacion compuesta —un reemplazo que borra e inserta— se deshace de un golpe en vez de en
 * dos pasos que el usuario nunca penso como separados.
 *
 * <p>{@link #getUpdateLevel} cuenta anidamiento, asi que las agrupaciones se pueden encajar: solo el
 * {@code endUpdate} mas externo dispara el envio.
 *
 * <h2>Por que existe {@link #realSource}</h2>
 *
 * <p>Un objeto que quiere emitir estos eventos rara vez hereda de esta clase: la tiene adentro como
 * un campo. Sin {@code realSource}, el evento diria que la edicion vino del ayudante y no del
 * documento, que es lo que al oyente le importa.
 */
public class UndoableEditSupport {

    /** Cuantos {@link #beginUpdate} hay abiertos. Cero significa que se reparte al toque. */
    protected int updateLevel;

    /** Donde se acumulan las ediciones mientras hay una agrupacion abierta. */
    protected CompoundEdit compoundEdit;

    /** Los oyentes. */
    protected Vector<UndoableEditListener> listeners;

    /** Quien figura como origen de los eventos; ver la nota de la clase. */
    protected Object realSource;

    /** Con este mismo objeto como origen. */
    public UndoableEditSupport() {
        this(null);
    }

    /** Con {@code r} como origen de los eventos. */
    public UndoableEditSupport(Object r) {
        this.realSource = r == null ? this : r;
        this.updateLevel = 0;
        this.compoundEdit = null;
        this.listeners = new Vector<UndoableEditListener>();
    }

    /** Agrega un oyente. */
    public synchronized void addUndoableEditListener(UndoableEditListener l) {
        this.listeners.addElement(l);
    }

    /** Saca un oyente. */
    public synchronized void removeUndoableEditListener(UndoableEditListener l) {
        this.listeners.removeElement(l);
    }

    /** Los oyentes, en un arreglo nuevo. */
    public synchronized UndoableEditListener[] getUndoableEditListeners() {
        int n = this.listeners.size();
        UndoableEditListener[] copia = new UndoableEditListener[n];
        for (int i = 0; i < n; i++) {
            copia[i] = this.listeners.elementAt(i);
        }
        return copia;
    }

    /**
     * Reparte {@code e} a todos los oyentes, ya.
     *
     * <p>Sin sincronizar y separada de {@link #postEdit} a proposito: el reparto llama a codigo
     * ajeno, y hacerlo con el candado tomado es una receta de interbloqueo.
     */
    protected void _postEdit(UndoableEdit e) {
        UndoableEditEvent ev = new UndoableEditEvent(this.realSource, e);
        UndoableEditListener[] copia = getUndoableEditListeners();
        for (int i = 0; i < copia.length; i++) {
            copia[i].undoableEditHappened(ev);
        }
    }

    /**
     * Publica una edicion: la acumula si hay una agrupacion abierta, o la reparte si no.
     */
    public synchronized void postEdit(UndoableEdit e) {
        if (this.updateLevel == 0) {
            _postEdit(e);
        } else {
            this.compoundEdit.addEdit(e);
        }
    }

    /** Cuantas agrupaciones hay abiertas. */
    public int getUpdateLevel() {
        return this.updateLevel;
    }

    /** Abre una agrupacion; las ediciones se acumulan hasta el {@link #endUpdate} que la cierre. */
    public synchronized void beginUpdate() {
        if (this.updateLevel == 0) {
            this.compoundEdit = createCompoundEdit();
        }
        this.updateLevel = this.updateLevel + 1;
    }

    /**
     * El grupo que usa la agrupacion.
     *
     * <p>Existe para que una subclase pueda devolver un {@link CompoundEdit} propio —uno con nombre,
     * por ejemplo— sin reescribir el resto del mecanismo.
     */
    protected CompoundEdit createCompoundEdit() {
        return new CompoundEdit();
    }

    /** Cierra una agrupacion; la mas externa reparte el grupo entero como una sola edicion. */
    public synchronized void endUpdate() {
        this.updateLevel = this.updateLevel - 1;
        if (this.updateLevel == 0) {
            this.compoundEdit.end();
            CompoundEdit terminado = this.compoundEdit;
            this.compoundEdit = null;
            _postEdit(terminado);
        }
    }

    public String toString() {
        return super.toString()
                + " updateLevel: " + String.valueOf(this.updateLevel)
                + " listeners: " + String.valueOf(this.listeners)
                + " compoundEdit: " + String.valueOf(this.compoundEdit);
    }
}
