package javax.swing.text;

/**
 * Un filtro que ve cada movimiento del cursor antes de que ocurra.
 *
 * <p>Es el hermano de {@link DocumentFilter} para la navegacion: sirve para que el cursor no pueda
 * entrar en un tramo —una plantilla con partes fijas— o para que salte de a palabras. La version
 * de esta clase deja pasar todo.
 *
 * <p>Como alla, el filtro no mueve el cursor llamando al cursor: llama al {@link FilterBypass},
 * que se saltea el filtro y no vuelve a entrar.
 */
public class NavigationFilter {

    public NavigationFilter() {
    }

    /** Mover el cursor deshaciendo la seleccion. */
    public void setDot(FilterBypass fb, int dot, Position.Bias bias) {
        fb.setDot(dot, bias);
    }

    /** Mover el cursor extendiendo la seleccion. */
    public void moveDot(FilterBypass fb, int dot, Position.Bias bias) {
        fb.moveDot(dot, bias);
    }

    /**
     * A donde va el cursor desde esa posicion en esa direccion.
     *
     * <p>Se la pasa al aspecto del componente, que es quien conoce el arbol de vistas; un filtro
     * que quiera saltar de otra forma la redefine.
     */
    public int getNextVisualPositionFrom(JTextComponent text, int pos, Position.Bias bias,
            int direction, Position.Bias[] biasRet) throws BadLocationException {
        return text.getUI().getNextVisualPositionFrom(text, pos, bias, direction, biasRet);
    }

    /** El atajo para mover el cursor sin volver a pasar por el filtro. */
    public abstract static class FilterBypass {

        protected FilterBypass() {
        }

        public abstract Caret getCaret();

        public abstract void setDot(int dot, Position.Bias bias);

        public abstract void moveDot(int dot, Position.Bias bias);
    }
}
