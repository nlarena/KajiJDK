package javax.swing.undo;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

/**
 * La pila de deshacer y rehacer.
 *
 * <h2>Un {@link CompoundEdit} que se lee distinto</h2>
 *
 * <p>Hereda la lista de ediciones pero no su semantica: un {@code CompoundEdit} deshace
 * <em>todas</em> sus partes de un golpe, y este deshace <strong>una por vez</strong>. Lo que lo
 * consigue es un solo campo, {@link #indexOfNextAdd}: el cursor que parte la lista en lo hecho y lo
 * deshecho. Todo lo demas de la clase es mover ese cursor.
 *
 * <p>Y por eso {@code UndoManager} sigue estando "en curso" para siempre: nunca se cierra, porque
 * siempre puede llegar otra edicion.
 *
 * <h2>Agregar tira lo rehacible, y tiene que hacerlo</h2>
 *
 * <p>Si el usuario deshizo tres pasos y despues hace algo nuevo, esos tres dejan de tener sentido:
 * rehacerlos aplicaria cambios sobre un estado que ya no es el que tenian delante. {@link #addEdit}
 * los mata explicitamente en vez de dejarlos colgando.
 *
 * <h2>El limite</h2>
 *
 * <p>{@link #setLimit} acota cuantas ediciones se recuerdan, porque una pila sin limite crece con la
 * sesion entera. Al podar se sacan <strong>las de los extremos</strong>, conservando las vecinas al
 * cursor: son las que el usuario tiene mas cerca de deshacer o rehacer.
 */
public class UndoManager extends CompoundEdit implements UndoableEditListener {

    private static final long serialVersionUID = -2077529998244066750L;

    /** El cursor: cuantas ediciones de la lista estan hechas. */
    int indexOfNextAdd;

    /** Cuantas ediciones se recuerdan como maximo. */
    int limit;

    /** Una pila nueva, con capacidad para cien ediciones. */
    public UndoManager() {
        super();
        this.indexOfNextAdd = 0;
        this.limit = 100;
    }

    /** El limite actual. */
    public synchronized int getLimit() {
        return this.limit;
    }

    /** Tira todo lo que recordaba y vuelve al estado inicial. */
    public synchronized void discardAllEdits() {
        for (int i = 0; i < this.edits.size(); i++) {
            this.edits.elementAt(i).die();
        }
        this.edits.removeAllElements();
        this.indexOfNextAdd = 0;
    }

    /** Poda hasta cumplir el limite, sacando de los extremos. */
    protected void trimForLimit() {
        if (this.limit < 0) {
            return;
        }
        int tamano = this.edits.size();
        if (tamano <= this.limit) {
            return;
        }
        // El cursor es el centro de interes: se conservan las `limit` ediciones mas cercanas a el,
        // repartidas a los dos lados. Podar solo por un extremo dejaria al usuario sin rehacer
        // justo despues de deshacer mucho.
        int mitad = this.limit / 2;
        int desde = this.indexOfNextAdd - mitad;
        int hasta = this.indexOfNextAdd + (this.limit - mitad) - 1;
        if (desde < 0) {
            hasta = hasta - desde;
            desde = 0;
        }
        if (hasta >= tamano) {
            desde = desde - (hasta - tamano + 1);
            hasta = tamano - 1;
            if (desde < 0) {
                desde = 0;
            }
        }
        trimEdits(hasta + 1, tamano - 1);
        trimEdits(0, desde - 1);
    }

    /**
     * Mata y saca las ediciones del rango, inclusive.
     *
     * <p>De atras para adelante, porque sacar corre los indices: hacerlo al reves saltearia
     * elementos, que es el error clasico de borrar mientras se recorre.
     */
    protected void trimEdits(int from, int to) {
        if (from > to) {
            return;
        }
        for (int i = to; i >= from; i--) {
            this.edits.elementAt(i).die();
            this.edits.removeElementAt(i);
        }
        if (this.indexOfNextAdd > to) {
            this.indexOfNextAdd = this.indexOfNextAdd - (to - from + 1);
        } else if (this.indexOfNextAdd >= from) {
            this.indexOfNextAdd = from;
        }
    }

    /** Cambia el limite y poda enseguida si hace falta. */
    public synchronized void setLimit(int l) {
        if (!isInProgress()) {
            throw new RuntimeException("La pila ya se cerro");
        }
        this.limit = l;
        trimForLimit();
    }

    /** La proxima edicion que se deshara, o {@code null}. */
    protected UndoableEdit editToBeUndone() {
        int i = this.indexOfNextAdd;
        while (i > 0) {
            i = i - 1;
            UndoableEdit e = this.edits.elementAt(i);
            if (e.isSignificant()) {
                return e;
            }
        }
        return null;
    }

    /** La proxima edicion que se rehara, o {@code null}. */
    protected UndoableEdit editToBeRedone() {
        int contador = this.edits.size();
        int i = this.indexOfNextAdd;
        while (i < contador) {
            UndoableEdit e = this.edits.elementAt(i);
            if (e.isSignificant()) {
                return e;
            }
            i = i + 1;
        }
        return null;
    }

    /** Deshace hacia atras hasta pasar {@code edit}, inclusive. */
    protected void undoTo(UndoableEdit edit) throws CannotUndoException {
        boolean seguir = true;
        while (seguir) {
            this.indexOfNextAdd = this.indexOfNextAdd - 1;
            UndoableEdit siguiente = this.edits.elementAt(this.indexOfNextAdd);
            siguiente.undo();
            seguir = siguiente != edit;
        }
    }

    /** Rehace hacia adelante hasta pasar {@code edit}, inclusive. */
    protected void redoTo(UndoableEdit edit) throws CannotRedoException {
        boolean seguir = true;
        while (seguir) {
            UndoableEdit siguiente = this.edits.elementAt(this.indexOfNextAdd);
            this.indexOfNextAdd = this.indexOfNextAdd + 1;
            siguiente.redo();
            seguir = siguiente != edit;
        }
    }

    /**
     * Deshace o rehace, lo que corresponda.
     *
     * <p>Para un boton unico. Cual de las dos depende de donde este el cursor, no de un estado
     * aparte.
     */
    public void undoOrRedo() throws CannotRedoException, CannotUndoException {
        if (this.indexOfNextAdd == this.edits.size()) {
            undo();
        } else {
            redo();
        }
    }

    /** Si alguna de las dos se puede. */
    public synchronized boolean canUndoOrRedo() {
        if (this.indexOfNextAdd == this.edits.size()) {
            return canUndo();
        }
        return canRedo();
    }

    /** Deshace la ultima edicion significativa. */
    public synchronized void undo() throws CannotUndoException {
        if (isInProgress()) {
            UndoableEdit e = editToBeUndone();
            if (e == null) {
                throw new CannotUndoException();
            }
            undoTo(e);
            return;
        }
        super.undo();
    }

    /** Si hay algo que deshacer. */
    public synchronized boolean canUndo() {
        if (isInProgress()) {
            UndoableEdit e = editToBeUndone();
            return e != null && e.canUndo();
        }
        return super.canUndo();
    }

    /** Rehace la proxima edicion significativa. */
    public synchronized void redo() throws CannotRedoException {
        if (isInProgress()) {
            UndoableEdit e = editToBeRedone();
            if (e == null) {
                throw new CannotRedoException();
            }
            redoTo(e);
            return;
        }
        super.redo();
    }

    /** Si hay algo que rehacer. */
    public synchronized boolean canRedo() {
        if (isInProgress()) {
            UndoableEdit e = editToBeRedone();
            return e != null && e.canRedo();
        }
        return super.canRedo();
    }

    /**
     * Agrega una edicion, tirando lo que quedaba por rehacer.
     *
     * <p>Ver la nota de la clase: rehacer despues de una edicion nueva aplicaria cambios sobre un
     * estado que ya no es el que tenian delante.
     */
    public synchronized boolean addEdit(UndoableEdit anEdit) {
        boolean retVal;
        trimEdits(this.indexOfNextAdd, this.edits.size() - 1);
        retVal = super.addEdit(anEdit);
        if (isInProgress()) {
            retVal = true;
        }
        this.indexOfNextAdd = this.edits.size();
        trimForLimit();
        return retVal;
    }

    /** Cierra la pila: a partir de ahi se comporta como un {@link CompoundEdit} comun. */
    public synchronized void end() {
        super.end();
        trimEdits(this.indexOfNextAdd, this.edits.size() - 1);
    }

    /** El nombre del comando para un boton unico de deshacer o rehacer. */
    public synchronized String getUndoOrRedoPresentationName() {
        if (this.indexOfNextAdd == this.edits.size()) {
            return getUndoPresentationName();
        }
        return getRedoPresentationName();
    }

    /** El nombre del comando de deshacer. */
    public synchronized String getUndoPresentationName() {
        if (!isInProgress()) {
            return super.getUndoPresentationName();
        }
        if (canUndo()) {
            return editToBeUndone().getUndoPresentationName();
        }
        return UndoName;
    }

    /** El nombre del comando de rehacer. */
    public synchronized String getRedoPresentationName() {
        if (!isInProgress()) {
            return super.getRedoPresentationName();
        }
        if (canRedo()) {
            return editToBeRedone().getRedoPresentationName();
        }
        return RedoName;
    }

    /**
     * Recibe una edicion de quien la produjo y la agrega.
     *
     * <p>Implementar {@link UndoableEditListener} es lo que permite conectar la pila a un documento
     * con una linea, sin que el documento sepa que existe una pila.
     */
    public void undoableEditHappened(UndoableEditEvent e) {
        addEdit(e.getEdit());
    }

    public String toString() {
        return super.toString()
                + " limit: " + String.valueOf(this.limit)
                + " indexOfNextAdd: " + String.valueOf(this.indexOfNextAdd);
    }
}
