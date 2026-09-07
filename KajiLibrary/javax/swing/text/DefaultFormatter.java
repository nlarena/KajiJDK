package javax.swing.text;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.text.ParseException;

import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField$AbstractFormatter;

/**
 * El formateador de siempre: convierte con {@code toString} y con un constructor de una cadena.
 *
 * <h2>Como convierte de vuelta</h2>
 *
 * <p>Para ir de valor a texto alcanza con {@code toString}. Para volver, busca en la clase del
 * valor un constructor que reciba una {@code String} y lo llama. Por eso funciona sin configurar
 * nada con {@code Integer}, {@code Double} o cualquier clase que tenga ese constructor, y no
 * funciona con las que no lo tienen.
 *
 * <h2>Edicion valida a cada tecla</h2>
 *
 * <p>Con {@link #setAllowsInvalid} en falso, cada tecla se prueba antes de aceptarla: se arma como
 * quedaria el texto, se intenta convertir, y si no se puede la tecla se rechaza. Eso es lo que
 * permite un campo donde literalmente no se puede escribir algo invalido.
 *
 * <p>El precio es que hay estados intermedios validos que se vuelven inalcanzables. Escribir
 * <code>-5</code> requiere pasar por <code>-</code>, que no es un numero. Por eso el valor por
 * omision es dejar escribir cualquier cosa y validar despues.
 */
public class DefaultFormatter extends JFormattedTextField$AbstractFormatter
        implements Cloneable, Serializable {

    private boolean commitOnEdit;
    private boolean overwriteMode;
    private boolean allowsInvalid;
    private Class<?> valueClass;

    private transient DocumentFilter filtroDoc;
    private transient NavigationFilter filtroNav;

    /** Un formateador que deja escribir cualquier cosa y sobrescribe al escribir. */
    public DefaultFormatter() {
        overwriteMode = true;
        allowsInvalid = true;
    }

    /** Se engancha al campo y deja el cursor al principio. */
    public void install(JFormattedTextField ftf) {
        super.install(ftf);
        positionCursorAtInitialLocation();
    }

    /** Si cada edicion valida pasa enseguida al valor del campo. */
    public void setCommitsOnValidEdit(boolean commit) {
        commitOnEdit = commit;
    }

    public boolean getCommitsOnValidEdit() {
        return commitOnEdit;
    }

    /** Si escribir reemplaza en lugar de insertar. */
    public void setOverwriteMode(boolean overwriteMode) {
        this.overwriteMode = overwriteMode;
    }

    public boolean getOverwriteMode() {
        return overwriteMode;
    }

    /** Si se puede dejar el campo en un estado que no se convierte; ver la nota de la clase. */
    public void setAllowsInvalid(boolean allowsInvalid) {
        this.allowsInvalid = allowsInvalid;
    }

    public boolean getAllowsInvalid() {
        return allowsInvalid;
    }

    /** La clase a la que se convierte el texto. */
    public void setValueClass(Class<?> valueClass) {
        this.valueClass = valueClass;
    }

    public Class<?> getValueClass() {
        return valueClass;
    }

    /**
     * Convierte el texto al valor con el constructor de una cadena.
     *
     * @throws ParseException si la clase no tiene ese constructor o si lo rechaza.
     */
    public Object stringToValue(String string) throws ParseException {
        Class<?> vc = getValueClass();
        JFormattedTextField ftf = getFormattedTextField();

        if (vc == null && ftf != null) {
            Object value = ftf.getValue();
            if (value != null) {
                vc = value.getClass();
            }
        }
        if (vc != null) {
            Constructor<?> cons;
            try {
                cons = vc.getConstructor(new Class<?>[] {String.class});
            } catch (NoSuchMethodException nsme) {
                cons = null;
            }
            if (cons != null) {
                try {
                    return cons.newInstance(new Object[] {string});
                } catch (Throwable ex) {
                    throw new ParseException("Error creating instance", 0);
                }
            } else {
                throw new ParseException("Unable to create instance for " + vc, 0);
            }
        }
        return string;
    }

    /** El texto del valor: su {@code toString}, o vacio si es nulo. */
    public String valueToString(Object value) throws ParseException {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    /** El filtro que valida cada tecla; ver la nota de la clase. */
    protected DocumentFilter getDocumentFilter() {
        if (filtroDoc == null) {
            filtroDoc = new DefaultDocumentFilter(this);
        }
        return filtroDoc;
    }

    /** El filtro que decide donde se puede parar el cursor. */
    protected NavigationFilter getNavigationFilter() {
        if (filtroNav == null) {
            filtroNav = new DefaultNavigationFilter(this);
        }
        return filtroNav;
    }

    public Object clone() throws CloneNotSupportedException {
        DefaultFormatter formatter = (DefaultFormatter) super.clone();
        formatter.filtroDoc = null;
        formatter.filtroNav = null;
        return formatter;
    }

    /** Donde empieza el cursor al instalarse. */
    void positionCursorAtInitialLocation() {
        JFormattedTextField ftf = getFormattedTextField();
        if (ftf != null) {
            ftf.setCaretPosition(getInitialVisualPosition());
        }
    }

    int getInitialVisualPosition() {
        return 0;
    }

    /**
     * Reenvia a {@link #invalidEdit()} desde las clases anidadas.
     *
     * <p>El filtro es una clase anidada y nuestro compilador todavia no le deja tocar un
     * {@code protected} heredado de otro paquete (hallazgo #512 en
     * <code>COMPILER_FINDINGS.md</code>).
     */
    void avisarInvalido() {
        invalidEdit();
    }

    /** Si el texto que quedaria es aceptable. */
    boolean isValidEdit(String text) {
        if (!getAllowsInvalid()) {
            try {
                stringToValue(text);
            } catch (ParseException pe) {
                return false;
            }
        }
        return true;
    }

    /** Pasa el texto al valor, si asi se pidio. */
    void commitEdit() {
        JFormattedTextField ftf = getFormattedTextField();
        if (ftf != null) {
            try {
                ftf.commitEdit();
                setEditValid(true);
            } catch (ParseException pe) {
                setEditValid(false);
            }
        }
    }

    /** Marca si lo escrito hasta ahora se convierte. */
    void updateValue(String text) {
        try {
            stringToValue(text);
            setEditValid(true);
            if (getCommitsOnValidEdit()) {
                commitEdit();
            }
        } catch (ParseException pe) {
            setEditValid(false);
        }
    }

    /**
     * Filtra la escritura: valida antes de dejar pasar y sobrescribe si corresponde.
     *
     * <p>En el JDK es una clase interna; aca es estatica con el formateador como primer parametro.
     * No es publica, asi que la firma no se ve desde afuera.
     */
    static class DefaultDocumentFilter extends DocumentFilter implements Serializable {

        private final DefaultFormatter fmt;

        DefaultDocumentFilter(DefaultFormatter fmt) {
            this.fmt = fmt;
        }

        public void remove(DocumentFilter.FilterBypass fb, int offset, int length)
                throws BadLocationException {
            String actual = fb.getDocument().getText(0, fb.getDocument().getLength());
            String queda = actual.substring(0, offset) + actual.substring(offset + length);
            if (fmt.isValidEdit(queda)) {
                fb.remove(offset, length);
                fmt.updateValue(queda);
            } else {
                fmt.avisarInvalido();
            }
        }

        public void insertString(DocumentFilter.FilterBypass fb, int offset, String string,
                AttributeSet attr) throws BadLocationException {
            replace(fb, offset, 0, string, attr);
        }

        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text,
                AttributeSet attr) throws BadLocationException {
            if (text == null) {
                text = "";
            }
            Document doc = fb.getDocument();
            String actual = doc.getText(0, doc.getLength());
            int fin = offset + length;
            if (fmt.getOverwriteMode() && length == 0) {
                // Sobrescribir: lo que se escribe se come lo que hay adelante.
                fin = Math.min(offset + text.length(), actual.length());
            }
            String queda = actual.substring(0, offset) + text + actual.substring(fin);
            if (fmt.isValidEdit(queda)) {
                fb.replace(offset, fin - offset, text, attr);
                fmt.updateValue(queda);
            } else {
                fmt.avisarInvalido();
            }
        }
    }

    /** Filtra el movimiento del cursor; el de siempre no lo limita. */
    static class DefaultNavigationFilter extends NavigationFilter implements Serializable {

        private final DefaultFormatter fmt;

        DefaultNavigationFilter(DefaultFormatter fmt) {
            this.fmt = fmt;
        }

        public void setDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias) {
            fb.setDot(dot, bias);
        }

        public void moveDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias) {
            fb.moveDot(dot, bias);
        }
    }
}
