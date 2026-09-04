package javax.swing.undo;

/**
 * Algo que se hizo y se puede deshacer.
 *
 * <h2>El ciclo de vida, que es lo que hay que entender</h2>
 *
 * <p>Una edicion nace ya <em>hecha</em>. A partir de ahi alterna entre deshecha y rehecha, y en
 * cualquier momento puede <strong>morir</strong> con {@link #die}: eso la saca de juego para siempre
 * y libera lo que estuviera reteniendo. El estado muerto no es lo mismo que deshecho — de uno se
 * vuelve, del otro no.
 *
 * <p>De ahi que haya cuatro metodos donde parecerian alcanzar dos: {@link #canUndo} y
 * {@link #canRedo} no son la negacion uno del otro, porque una edicion muerta contesta {@code false}
 * a los dos.
 *
 * <h2>Fusion: {@link #addEdit} y {@link #replaceEdit}</h2>
 *
 * <p>Sin fusion, tipear una palabra dejaria una edicion por letra y deshacer seria letra por letra.
 * Los dos metodos son las dos direcciones de absorber al vecino: {@code addEdit} pregunta "¿te
 * podes tragar a este que viene?", {@code replaceEdit} pregunta "¿te podes tragar al que ya
 * estaba?". Contestar {@code false} a los dos es siempre valido y da el comportamiento sin fusion.
 */
public interface UndoableEdit {

    /** Revierte lo que esta edicion hizo. */
    void undo() throws CannotUndoException;

    /** Si se puede deshacer ahora. */
    boolean canUndo();

    /** Vuelve a aplicar lo que esta edicion hizo. */
    void redo() throws CannotRedoException;

    /** Si se puede rehacer ahora. */
    boolean canRedo();

    /** La saca de juego para siempre y libera lo que retenia. */
    void die();

    /** Intenta absorber a {@code anEdit}, que viene despues de esta. */
    boolean addEdit(UndoableEdit anEdit);

    /** Intenta absorber a {@code anEdit}, que estaba antes que esta. */
    boolean replaceEdit(UndoableEdit anEdit);

    /**
     * Si vale la pena mostrarla como un paso propio.
     *
     * <p>Una edicion insignificante se deshace junto con la siguiente significativa en vez de
     * consumir un paso del usuario. Es como se evita que mover el cursor cuente como una accion.
     */
    boolean isSignificant();

    /** El nombre para mostrarle a una persona. */
    String getPresentationName();

    /** El nombre para el comando de deshacer, tipicamente "Deshacer" mas el anterior. */
    String getUndoPresentationName();

    /** El nombre para el comando de rehacer. */
    String getRedoPresentationName();
}
