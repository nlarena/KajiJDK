package javax.swing.undo;

import java.util.Hashtable;

/**
 * Un objeto que sabe guardar y restaurar su propio estado en una tabla.
 *
 * <h2>La otra forma de deshacer</h2>
 *
 * <p>Un {@link UndoableEdit} corriente sabe <em>revertir una accion</em>: conoce la operacion y su
 * inversa. Esto es el enfoque opuesto — no se guarda la operacion sino <strong>una foto del estado
 * antes y otra despues</strong>, y deshacer es volver a poner la primera. Ver {@link StateEdit}, que
 * es quien las toma.
 *
 * <p>Sirve cuando la inversa de una operacion es dificil o imposible de escribir, y cuesta memoria:
 * dos copias del estado por cada paso.
 *
 * <p>Las dos firmas no son simetricas y eso es deliberado: guardar recibe una tabla que se puede
 * escribir, restaurar recibe una que solo se lee.
 */
public interface StateEditable {

    /** Identificador de version del JDK; se conserva por fidelidad de la superficie. */
    public static final String RCSID = "$Id: StateEditable.java,v 1.2 1997/09/08 19:39:08 marklin Exp $";

    /** Guarda en {@code state} lo que haga falta para poder volver a este estado. */
    void storeState(Hashtable<Object, Object> state);

    /** Vuelve al estado que describe {@code state}. */
    void restoreState(Hashtable<?, ?> state);
}
