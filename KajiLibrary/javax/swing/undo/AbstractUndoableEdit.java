package javax.swing.undo;

/**
 * La base de casi toda edicion: lleva la cuenta de si esta hecha y si sigue viva.
 *
 * <h2>Dos banderas, cuatro estados</h2>
 *
 * <p>{@code alive} y {@code hasBeenDone} parecen redundantes y no lo son: una edicion muerta
 * contesta {@code false} a {@link #canUndo} y a {@link #canRedo} a la vez, cosa que una sola bandera
 * no puede expresar. Muerta es distinto de deshecha — de lo segundo se vuelve.
 *
 * <p>Toda la clase es esa maquina de estados. Lo que la edicion concreta <em>hace</em> lo pone la
 * subclase sobrescribiendo {@link #undo} y {@link #redo}, siempre llamando a {@code super} primero
 * para que el chequeo corra antes que el trabajo.
 *
 * <h2>Los tres nombres</h2>
 *
 * <p>{@link #getPresentationName} es el de la edicion; los otros dos son el texto del menu, y salen
 * de pegarle "Undo" o "Redo" adelante. Estan separados porque en un menu el nombre completo es lo
 * que se muestra, y una edicion sin nombre igual necesita que el item diga algo.
 */
public class AbstractUndoableEdit implements UndoableEdit, java.io.Serializable {

    private static final long serialVersionUID = 580150227676302096L;

    /** El prefijo del comando de deshacer. */
    protected static final String UndoName = "Undo";

    /** El prefijo del comando de rehacer. */
    protected static final String RedoName = "Redo";

    /** Si la edicion esta aplicada. Arranca en {@code true}: una edicion nace hecha. */
    boolean hasBeenDone;

    /** Si todavia se puede usar. {@link #die} la apaga y no se vuelve a prender. */
    boolean alive;

    /** Una edicion nueva: viva y ya hecha. */
    public AbstractUndoableEdit() {
        super();
        this.hasBeenDone = true;
        this.alive = true;
    }

    /**
     * La saca de juego para siempre.
     *
     * <p>No deshace nada: matar y deshacer son cosas distintas. Quien quiera las dos tiene que
     * pedirlas en orden.
     */
    public void die() {
        this.alive = false;
    }

    /**
     * @throws CannotUndoException si {@link #canUndo} es {@code false}
     */
    public void undo() throws CannotUndoException {
        if (!canUndo()) {
            throw new CannotUndoException();
        }
        this.hasBeenDone = false;
    }

    /** Se puede deshacer si esta viva y hecha. */
    public boolean canUndo() {
        return this.alive && this.hasBeenDone;
    }

    /**
     * @throws CannotRedoException si {@link #canRedo} es {@code false}
     */
    public void redo() throws CannotRedoException {
        if (!canRedo()) {
            throw new CannotRedoException();
        }
        this.hasBeenDone = true;
    }

    /** Se puede rehacer si esta viva y deshecha. */
    public boolean canRedo() {
        return this.alive && !this.hasBeenDone;
    }

    /** No absorbe nada: la fusion la deciden las subclases que sepan como. */
    public boolean addEdit(UndoableEdit anEdit) {
        return false;
    }

    /** No reemplaza nada. */
    public boolean replaceEdit(UndoableEdit anEdit) {
        return false;
    }

    /** Significativa por omision, que es lo seguro: se ve como un paso propio. */
    public boolean isSignificant() {
        return true;
    }

    /** Cadena vacia: una edicion generica no tiene nombre que mostrar. */
    public String getPresentationName() {
        return "";
    }

    /** El prefijo de deshacer seguido del nombre, si lo hay. */
    public String getUndoPresentationName() {
        String nombre = getPresentationName();
        if (nombre.isEmpty()) {
            return UndoName;
        }
        return UndoName + " " + nombre;
    }

    /** El prefijo de rehacer seguido del nombre, si lo hay. */
    public String getRedoPresentationName() {
        String nombre = getPresentationName();
        if (nombre.isEmpty()) {
            return RedoName;
        }
        return RedoName + " " + nombre;
    }

    public String toString() {
        return super.toString()
                + " hasBeenDone: " + String.valueOf(this.hasBeenDone)
                + " alive: " + String.valueOf(this.alive);
    }
}
