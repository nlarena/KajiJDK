package javax.swing.undo;

import java.util.Vector;

/**
 * Varias ediciones que se deshacen y rehacen como una sola.
 *
 * <h2>Los dos momentos: en curso y cerrada</h2>
 *
 * <p>Es el estado que gobierna todo lo demas. Mientras esta <em>en curso</em> acepta ediciones y no
 * se puede deshacer; despues de {@link #end} deja de aceptar y recien ahi se comporta como una
 * edicion normal. Esa asimetria evita el caso sin sentido de deshacer un grupo que todavia se esta
 * armando.
 *
 * <h2>El orden importa</h2>
 *
 * <p>Deshacer recorre la lista <strong>al reves</strong> y rehacer hacia adelante. Es obvio dicho y
 * facil de escribir mal: deshacer en orden de aplicacion revertiria los efectos en la secuencia
 * equivocada cada vez que dos ediciones tocan lo mismo.
 *
 * <p>Un grupo es significativo si <em>alguna</em> de sus partes lo es, y su nombre es el de la
 * ultima, que es la que el usuario acaba de hacer.
 */
public class CompoundEdit extends AbstractUndoableEdit {

    private static final long serialVersionUID = -6512679417930021399L;

    /** Si todavia acepta ediciones. */
    boolean inProgress;

    /** Las ediciones que agrupa, en orden de aplicacion. */
    protected Vector<UndoableEdit> edits;

    /** Un grupo nuevo, en curso y vacio. */
    public CompoundEdit() {
        super();
        this.inProgress = true;
        this.edits = new Vector<UndoableEdit>();
    }

    /** Deshace todas, de la ultima a la primera. */
    public void undo() throws CannotUndoException {
        super.undo();
        int i = this.edits.size();
        while (i > 0) {
            i = i - 1;
            UndoableEdit e = this.edits.elementAt(i);
            e.undo();
        }
    }

    /** Rehace todas, de la primera a la ultima. */
    public void redo() throws CannotRedoException {
        super.redo();
        for (int i = 0; i < this.edits.size(); i++) {
            this.edits.elementAt(i).redo();
        }
    }

    /** La ultima edicion agregada, o {@code null} si no hay ninguna. */
    protected UndoableEdit lastEdit() {
        int n = this.edits.size();
        if (n > 0) {
            return this.edits.elementAt(n - 1);
        }
        return null;
    }

    /** Mata a todas, de la ultima a la primera, y despues a si misma. */
    public void die() {
        int i = this.edits.size();
        while (i > 0) {
            i = i - 1;
            this.edits.elementAt(i).die();
        }
        super.die();
    }

    /**
     * Agrega una edicion al grupo, si todavia esta en curso.
     *
     * <p>Antes de encolarla intenta <strong>fusionarla</strong> con la ultima: primero le pregunta a
     * la ultima si puede absorber a la nueva, y si no, le pregunta a la nueva si puede absorber a la
     * ultima. Solo si las dos dicen que no, la lista crece.
     */
    public boolean addEdit(UndoableEdit anEdit) {
        if (!this.inProgress) {
            return false;
        }
        UndoableEdit ultima = lastEdit();
        if (ultima == null) {
            this.edits.addElement(anEdit);
            return true;
        }
        if (!ultima.addEdit(anEdit)) {
            if (anEdit.replaceEdit(ultima)) {
                this.edits.removeElementAt(this.edits.size() - 1);
            }
            this.edits.addElement(anEdit);
        }
        return true;
    }

    /** Cierra el grupo: deja de aceptar ediciones y empieza a poder deshacerse. */
    public void end() {
        this.inProgress = false;
    }

    /** Se puede deshacer si esta cerrada y viva. */
    public boolean canUndo() {
        return !isInProgress() && super.canUndo();
    }

    /** Se puede rehacer si esta cerrada y deshecha. */
    public boolean canRedo() {
        return !isInProgress() && super.canRedo();
    }

    /** Si todavia acepta ediciones. */
    public boolean isInProgress() {
        return this.inProgress;
    }

    /** Significativa si alguna de sus partes lo es. */
    public boolean isSignificant() {
        for (int i = 0; i < this.edits.size(); i++) {
            if (this.edits.elementAt(i).isSignificant()) {
                return true;
            }
        }
        return false;
    }

    /** El nombre de la ultima edicion: la que el usuario acaba de hacer. */
    public String getPresentationName() {
        UndoableEdit ultima = lastEdit();
        if (ultima != null) {
            return ultima.getPresentationName();
        }
        return super.getPresentationName();
    }

    public String getUndoPresentationName() {
        UndoableEdit ultima = lastEdit();
        if (ultima != null) {
            return ultima.getUndoPresentationName();
        }
        return super.getUndoPresentationName();
    }

    public String getRedoPresentationName() {
        UndoableEdit ultima = lastEdit();
        if (ultima != null) {
            return ultima.getRedoPresentationName();
        }
        return super.getRedoPresentationName();
    }

    public String toString() {
        return super.toString()
                + " inProgress: " + String.valueOf(this.inProgress)
                + " edits: " + String.valueOf(this.edits);
    }
}
