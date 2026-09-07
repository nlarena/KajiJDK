package javax.swing;

import java.awt.Component;

import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;

/**
 * Un area de texto con estilos, imagenes y componentes adentro.
 *
 * <h2>Que la separa de {@link JEditorPane}</h2>
 *
 * <p>La de arriba muestra cualquier formato -- texto plano, HTML, RTF -- eligiendo un
 * {@link EditorKit} segun el tipo de contenido. Esta fija el formato: siempre texto con estilos, y a
 * cambio expone la API para manipularlos sin pasar por el documento. De ahi que
 * {@link #setEditorKit} sea final: cambiarlo dejaria a todos los demas metodos sin sentido.
 *
 * <h2>Tres niveles de atributos</h2>
 *
 * <p>El <em>estilo logico</em> es el de fondo, el que se aplica al parrafo entero por pertenecer a
 * una categoria -- "titulo", "cita" --. Los <em>atributos de parrafo</em> lo pisan para ese parrafo.
 * Los <em>atributos de caracter</em> pisan a los dos para un tramo de texto. Se resuelven en ese
 * orden, y es lo que permite cambiar la letra de todos los titulos a la vez sin tocar las negritas
 * que alguien puso a mano.
 *
 * <h2>Los atributos de entrada</h2>
 *
 * <p>{@link #getInputAttributes} es lo que se le va a aplicar a lo <em>proximo</em> que se escriba.
 * Es lo que hace que apretar el boton de negrita sin nada seleccionado ponga en negrita lo que se
 * escriba a continuacion, que es lo que uno espera de un editor.
 */
public class JTextPane extends JEditorPane {

    /** Un area vacia con un documento con estilos. */
    public JTextPane() {
        super();
        EditorKit editorKit = createDefaultEditorKit();
        String contentType = editorKit.getContentType();
        if (contentType != null && getEditorKitClassNameForContentType(contentType)
                == getEditorKitClassNameForContentType("text/plain")) {
            setEditorKitForContentType(contentType, editorKit);
        }
        setEditorKit(editorKit);
    }

    /**
     * Sobre ese documento.
     *
     * @throws NullPointerException si el documento es nulo
     */
    public JTextPane(StyledDocument doc) {
        this();
        setStyledDocument(doc);
    }

    public String getUIClassID() {
        return "TextPaneUI";
    }

    /**
     * Cambia el documento.
     *
     * @throws IllegalArgumentException si no es un {@link StyledDocument}
     */
    public void setDocument(Document doc) {
        if (doc instanceof StyledDocument) {
            super.setDocument(doc);
        } else {
            throw new IllegalArgumentException("Model must be StyledDocument");
        }
    }

    /** Lo mismo, con el tipo justo. */
    public void setStyledDocument(StyledDocument doc) {
        super.setDocument(doc);
    }

    public StyledDocument getStyledDocument() {
        return (StyledDocument) getDocument();
    }

    /**
     * Reemplaza lo seleccionado por ese texto.
     *
     * <p>El texto nuevo se lleva los atributos de entrada; ver la nota de la clase.
     */
    public void replaceSelection(String content) {
        replaceSelection(content, true);
    }

    private void replaceSelection(String content, boolean checkEditable) {
        if (checkEditable && !isEditable()) {
            javax.swing.UIManager.getLookAndFeel();
            return;
        }
        Document doc = getStyledDocument();
        if (doc != null) {
            try {
                java.awt.Rectangle r = null;
                int p0 = Math.min(getCaret().getDot(), getCaret().getMark());
                int p1 = Math.max(getCaret().getDot(), getCaret().getMark());
                if (p0 != p1) {
                    doc.remove(p0, p1 - p0);
                }
                if (content != null && content.length() > 0) {
                    doc.insertString(p0, content, getInputAttributes().copyAttributes());
                }
            } catch (javax.swing.text.BadLocationException e) {
                // La posicion sale del cursor, que el documento acaba de validar: no puede pasar.
                throw new IllegalStateException(e.getMessage());
            }
        }
    }

    /**
     * Mete un componente adentro del texto, como si fuera una letra.
     *
     * <p>Ocupa una sola posicion en el documento y la vista lo dibuja tal cual. Es como se pone un
     * boton adentro de un parrafo.
     */
    public void insertComponent(Component c) {
        MutableAttributeSet inputAttributes = getInputAttributes();
        inputAttributes.removeAttributes(inputAttributes);
        javax.swing.text.StyleConstants.setComponent(inputAttributes, c);
        replaceSelection(" ", false);
        inputAttributes.removeAttributes(inputAttributes);
    }

    /** Mete un icono adentro del texto; ver {@link #insertComponent}. */
    public void insertIcon(Icon g) {
        MutableAttributeSet inputAttributes = getInputAttributes();
        inputAttributes.removeAttributes(inputAttributes);
        javax.swing.text.StyleConstants.setIcon(inputAttributes, g);
        replaceSelection(" ", false);
        inputAttributes.removeAttributes(inputAttributes);
    }

    /**
     * Agrega un estilo con nombre.
     *
     * <p>El padre es de donde hereda lo que no defina: es lo que permite tener un "titulo chico" que
     * es el "titulo" con otro tamano y nada mas.
     */
    public Style addStyle(String nm, Style parent) {
        StyledDocument doc = getStyledDocument();
        return doc.addStyle(nm, parent);
    }

    public void removeStyle(String nm) {
        StyledDocument doc = getStyledDocument();
        doc.removeStyle(nm);
    }

    public Style getStyle(String nm) {
        StyledDocument doc = getStyledDocument();
        return doc.getStyle(nm);
    }

    /** El estilo de fondo del parrafo donde esta el cursor; ver la nota de la clase. */
    public void setLogicalStyle(Style s) {
        StyledDocument doc = getStyledDocument();
        doc.setLogicalStyle(getCaretPosition(), s);
    }

    public Style getLogicalStyle() {
        StyledDocument doc = getStyledDocument();
        return doc.getLogicalStyle(getCaretPosition());
    }

    /** Los atributos del caracter donde esta el cursor. */
    public AttributeSet getCharacterAttributes() {
        StyledDocument doc = getStyledDocument();
        javax.swing.text.Element run = doc.getCharacterElement(getCaretPosition());
        if (run != null) {
            return run.getAttributes();
        }
        return null;
    }

    /**
     * Aplica esos atributos a lo seleccionado.
     *
     * <p>Sin seleccion, quedan como atributos de entrada -- ver la nota de la clase --. Con
     * {@code replace} en cierto se descarta lo que hubiera; en falso se mezcla.
     */
    public void setCharacterAttributes(AttributeSet attr, boolean replace) {
        int p0 = getSelectionStart();
        int p1 = getSelectionEnd();
        if (p0 != p1) {
            StyledDocument doc = getStyledDocument();
            doc.setCharacterAttributes(p0, p1 - p0, attr, replace);
        } else {
            MutableAttributeSet inputAttributes = getInputAttributes();
            if (replace) {
                inputAttributes.removeAttributes(inputAttributes);
            }
            inputAttributes.addAttributes(attr);
        }
    }

    /** Los atributos del parrafo donde esta el cursor. */
    public AttributeSet getParagraphAttributes() {
        StyledDocument doc = getStyledDocument();
        javax.swing.text.Element paragraph = doc.getParagraphElement(getCaretPosition());
        if (paragraph != null) {
            return paragraph.getAttributes();
        }
        return null;
    }

    /**
     * Aplica esos atributos a los parrafos tocados por la seleccion.
     *
     * <p>Un parrafo se aplica entero aunque la seleccion lo toque en una letra: la alineacion o la
     * sangria no pueden valer para media linea.
     */
    public void setParagraphAttributes(AttributeSet attr, boolean replace) {
        int p0 = getSelectionStart();
        int p1 = getSelectionEnd();
        StyledDocument doc = getStyledDocument();
        doc.setParagraphAttributes(p0, p1 - p0, attr, replace);
    }

    /** Lo que se le va a aplicar a lo proximo que se escriba; ver la nota de la clase. */
    public MutableAttributeSet getInputAttributes() {
        return getStyledEditorKit().getInputAttributes();
    }

    protected final StyledEditorKit getStyledEditorKit() {
        return (StyledEditorKit) getEditorKit();
    }

    protected EditorKit createDefaultEditorKit() {
        return new StyledEditorKit();
    }

    /**
     * Cambia el motor de edicion.
     *
     * @throws IllegalArgumentException si no es un {@link StyledEditorKit}; ver la nota de la clase
     */
    public final void setEditorKit(EditorKit kit) {
        if (kit instanceof StyledEditorKit) {
            super.setEditorKit(kit);
        } else {
            throw new IllegalArgumentException("Must be StyledEditorKit");
        }
    }

    protected String paramString() {
        return super.paramString();
    }
}
