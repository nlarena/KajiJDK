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
 * Todo lo que hace falta para editar un tipo de contenido: modelo, vistas, acciones y formato.
 *
 * <h2>La pieza intercambiable</h2>
 *
 * <p>Un componente de texto no sabe si muestra texto plano, HTML o RTF: sabe pedirle a su juego de
 * edicion un documento vacio, una fabrica de vistas, la lista de acciones y como leer y escribir
 * el formato. Cambiar el juego cambia las cuatro cosas de una vez y de forma coherente, que es
 * justo lo que no se lograria con cuatro propiedades sueltas.
 *
 * <p>De ahi que {@link #getContentType} sea parte del contrato: el juego dice para que tipo MIME
 * sirve, y un componente puede buscar el que corresponde a lo que le pidieron abrir.
 */
public abstract class EditorKit implements Cloneable, Serializable {

    public EditorKit() {
    }

    /**
     * Una copia.
     *
     * <p>Los juegos se comparten entre componentes, y algunos guardan estado —los atributos que se
     * van a aplicar al escribir—; por eso se clonan en vez de usarse el mismo.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /** Lo instalaron en ese componente; es donde se engancha lo que necesite. */
    public void install(JEditorPane c) {
    }

    public void deinstall(JEditorPane c) {
    }

    /** El tipo MIME que este juego entiende, como {@code "text/plain"}. */
    public abstract String getContentType();

    public abstract ViewFactory getViewFactory();

    /** Las acciones que este juego ofrece; un editor las pone en menus y teclas. */
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
