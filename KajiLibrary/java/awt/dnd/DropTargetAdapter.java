package java.awt.dnd;

/**
 * Un {@link DropTargetListener} con los tres métodos opcionales vacíos.
 *
 * <p>La asimetría con los otros adaptadores es deliberada y está bien pensada: {@link #drop} sigue
 * siendo **abstracto**. Un destino de arrastre que no haga nada al soltar no tiene sentido, así que
 * la clase obliga a escribirlo en vez de dejar que se olvide.
 */
public abstract class DropTargetAdapter implements DropTargetListener {

    /** Para las subclases. */
    protected DropTargetAdapter() {
    }

    /** No hace nada. */
    public void dragEnter(DropTargetDragEvent dtde) {
    }

    /** No hace nada. */
    public void dragOver(DropTargetDragEvent dtde) {
    }

    /** No hace nada. */
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    /** No hace nada. */
    public void dragExit(DropTargetEvent dte) {
    }
}
