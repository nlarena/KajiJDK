package javax.swing.text;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;

import javax.swing.Action;
import javax.swing.JEditorPane;

/**
 * Everything needed to edit a kind of content: model, views, actions and format.
 *
 * <h2>The interchangeable piece</h2>
 *
 * <p>A text component does not know whether it shows plain text, HTML or RTF: it knows how to
 * ask its editor kit for an empty document, a view factory, the list of actions and how to read
 * and write the format. Changing the kit changes the four things at once and consistently,
 * which is exactly what four loose properties would not achieve.
 *
 * <p>Hence {@link #getContentType} is part of the contract: the kit says which MIME type it
 * serves, and a component can look for the one that corresponds to what it was asked to open.
 */
public abstract class EditorKit implements Cloneable, Serializable {

    public EditorKit() {
    }

    /**
     * A copy.
     *
     * <p>The kits are shared between components, and some keep state --the attributes that will be
     * applied when typing--; that is why they are cloned instead of the same one being used.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /** It was installed on that component; it is where it hooks up whatever it needs. */
    public void install(JEditorPane c) {
    }

    public void deinstall(JEditorPane c) {
    }

    /** The MIME type this kit understands, such as {@code "text/plain"}. */
    public abstract String getContentType();

    public abstract ViewFactory getViewFactory();

    /** The actions this kit offers; an editor puts them in menus and keys. */
    public abstract Action[] getActions();

    public abstract Caret createCaret();

    public abstract Document createDefaultDocument();

    public abstract void read(InputStream in, Document doc, int pos) throws IOException,
            BadLocationException;

    public abstract void write(OutputStream out, Document doc, int pos, int len)
            throws IOException, BadLocationException;

    public abstract void read(Reader in, Document doc, int pos) throws IOException,
            BadLocationException;

    public abstract void write(Writer out, Document doc, int pos, int len) throws IOException,
            BadLocationException;
}
