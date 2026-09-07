package javax.swing;

import javax.accessibility.AccessibleContext;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * Un campo que muestra un caracter de eco en vez de lo que se escribe.
 *
 * <h2>Que protege y que no</h2>
 *
 * <p>Protege contra quien mira la pantalla, no contra el programa: el texto esta entero en el
 * documento. Lo que si hace esta clase es <strong>quitar los caminos comodos</strong> hacia el:
 * {@link #cut} y {@link #copy} no copian nada, y {@link #getText} esta desaconsejado en favor de
 * {@link #getPassword}, que devuelve un arreglo de caracteres.
 *
 * <p>La diferencia entre los dos no es cosmetica. Un {@code String} vive en memoria hasta que el
 * recolector lo levante y no se puede borrar; un arreglo se puede llenar de ceros apenas se usa.
 * Es toda la razon de que {@code getPassword} exista.
 */
public class JPasswordField extends JTextField {

    private static final String uiClassID = "PasswordFieldUI";

    private char echoChar;
    private boolean echoCharSet = false;

    /** Un campo vacio con el eco por omision. */
    public JPasswordField() {
        this(null, null, 0);
    }

    public JPasswordField(String text) {
        this(null, text, 0);
    }

    public JPasswordField(int columns) {
        this(null, null, columns);
    }

    public JPasswordField(String text, int columns) {
        this(null, text, columns);
    }

    public JPasswordField(Document doc, String txt, int columns) {
        super(doc, txt, columns);
        echoChar = '*';
    }

    public String getUIClassID() {
        return uiClassID;
    }

    public void updateUI() {
        if (!echoCharSet) {
            echoChar = '*';
        }
        super.updateUI();
    }

    public char getEchoChar() {
        return echoChar;
    }

    /** Cambia el caracter de eco; cero lo apaga y el campo se ve como uno comun. */
    public void setEchoChar(char c) {
        echoChar = c;
        echoCharSet = true;
        repaint();
        revalidate();
    }

    public boolean echoCharIsSet() {
        return echoChar != 0;
    }

    /** No hace nada: ver la nota de la clase. */
    public void cut() {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    /** No hace nada: ver la nota de la clase. */
    public void copy() {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    /** @deprecated es {@link #getPassword}; ver la nota de la clase. */
    @Deprecated
    public String getText() {
        return super.getText();
    }

    /** @deprecated es {@link #getPassword}. */
    @Deprecated
    public String getText(int offs, int len) throws BadLocationException {
        return super.getText(offs, len);
    }

    public void setText(String t) {
        super.setText(t);
    }

    /** La contrasena en un arreglo que el llamador puede borrar; ver la nota de la clase. */
    public char[] getPassword() {
        Document doc = getDocument();
        javax.swing.text.Segment txt = new javax.swing.text.Segment();
        try {
            doc.getText(0, doc.getLength(), txt);
        } catch (BadLocationException e) {
            return null;
        }
        char[] retValue = new char[txt.count];
        System.arraycopy(txt.array, txt.offset, retValue, 0, txt.count);
        return retValue;
    }

    protected String paramString() {
        return super.paramString() + ",echoChar=" + echoChar;
    }

    /** El eco no se toma de la tabla de un aspecto si el usuario lo puso. */
    boolean customSetUIProperty(String propertyName, Object value) {
        if ("echoChar".equals(propertyName)) {
            if (!echoCharSet) {
                setEchoChar((Character) value);
                echoCharSet = false;
            }
            return true;
        }
        return false;
    }

    /** Sin contexto de accesibilidad: no hay tecnologia asistiva en esta VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }
}
