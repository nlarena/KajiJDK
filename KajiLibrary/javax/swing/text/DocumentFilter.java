package javax.swing.text;

/**
 * Un filtro que ve cada edicion antes de que ocurra y decide que hacer con ella.
 *
 * <p>Es como se hace un campo que solo acepta numeros, uno que pasa todo a mayusculas o uno de
 * largo limitado: el filtro recibe lo que se quiere insertar y escribe otra cosa, o no escribe
 * nada. La version de esta clase deja pasar todo, para que una subclase redefina solo lo que le
 * interesa.
 *
 * <p>El filtro <strong>no</strong> escribe llamando al documento: llama al {@link FilterBypass}
 * que le pasan. Si llamara al documento, la escritura volveria a pasar por el filtro y no
 * terminaria nunca.
 */
public class DocumentFilter {

    public DocumentFilter() {
    }

    /** Deja borrar; una subclase puede no llamar al atajo y asi vetar el borrado. */
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        fb.remove(offset, length);
    }

    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {
        fb.insertString(offset, string, attr);
    }

    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        fb.replace(offset, length, text, attrs);
    }

    /**
     * El atajo para escribir sin volver a pasar por el filtro; ver la nota de la clase.
     *
     * <p>Lo implementa el documento, no el filtro: solo el documento puede saltearse su propio
     * filtro sin romper nada.
     */
    public abstract static class FilterBypass {

        protected FilterBypass() {
        }

        public abstract Document getDocument();

        public abstract void remove(int offset, int length) throws BadLocationException;

        public abstract void insertString(int offset, String string, AttributeSet attr)
                throws BadLocationException;

        public abstract void replace(int offset, int length, String string, AttributeSet attrs)
                throws BadLocationException;
    }
}
