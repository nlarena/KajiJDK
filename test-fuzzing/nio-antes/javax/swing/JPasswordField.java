package javax.swing;

import javax.accessibility.AccessibleContext;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * A field that shows an echo character instead of what is typed.
 *
 * <h2>What it protects and what it does not</h2>
 *
 * <p>It protects against whoever looks at the screen, not against the program: the text is whole
 * in the document. What this class does do is <strong>remove the comfortable paths</strong> to
 * it: {@link #cut} and {@link #copy} copy nothing, and {@link #getText} is discouraged in favour
 * of {@link #getPassword}, which returns an array of characters.
 *
 * <p>The difference between the two is not cosmetic. A {@code String} lives in memory until the
 * collector picks it up and cannot be erased; an array can be filled with zeros as soon as it is
 * used. It is the whole reason {@code getPassword} exists.
 */
public class JPasswordField extends JTextField {

    private static final String uiClassID = "PasswordFieldUI";

    private char echoChar;
    private boolean echoCharSet = false;

    /** An empty field with the default echo. */
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

    /**
     * It changes the echo character; zero switches it off and the field looks like an ordinary one.
     */
    public void setEchoChar(char c) {
        echoChar = c;
        echoCharSet = true;
        repaint();
        revalidate();
    }

    public boolean echoCharIsSet() {
        return echoChar != 0;
    }

    /** It does nothing: see the class note. */
    public void cut() {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    /** It does nothing: see the class note. */
    public void copy() {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    /** @deprecated it is {@link #getPassword}; see the class note. */
    @Deprecated
    public String getText() {
        return super.getText();
    }

    /** @deprecated it is {@link #getPassword}. */
    @Deprecated
    public String getText(int offs, int len) throws BadLocationException {
        return super.getText(offs, len);
    }

    public void setText(String t) {
        super.setText(t);
    }

    /** The password in an array the caller may erase; see the class note. */
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

    /** The echo is not taken from a look and feel's table if the user set it. */
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

    /** With no accessibility context: there is no assistive technology on this VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }
}
