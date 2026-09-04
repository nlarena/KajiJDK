package javax.swing.text;

import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;

/**
 * El modelo de un texto editable: contenido, estructura y avisos de cambio.
 *
 * <h2>Las tres cosas que junta</h2>
 *
 * <p>Un {@code Document} es a la vez la <strong>secuencia de caracteres</strong>, el
 * <strong>arbol de elementos</strong> que la estructura, y el <strong>emisor</strong> que avisa
 * cuando algo cambia. Juntarlas no es pereza: las tres tienen que moverse a la vez o el arbol
 * quedaria describiendo un texto que ya no es.
 *
 * <h2>{@link #render}, que es donde vive la concurrencia</h2>
 *
 * <p>Un editor lee el documento desde el hilo que pinta y lo escribe desde el que atiende el
 * teclado. {@code render} corre codigo con la garantia de que nadie modifica mientras tanto — es la
 * unica forma segura de recorrer el texto para dibujarlo. Sin el, un repintado podria leer el
 * documento a mitad de una insercion.
 *
 * <p>De ahi tambien que las posiciones se pidan como {@link Position} y no como enteros: un numero
 * guardado entre dos ediciones apunta a otro lado.
 */
public interface Document {

    /** La clave de la propiedad que describe de donde salio el texto. */
    public static final String StreamDescriptionProperty = "stream";

    /** La clave de la propiedad del titulo. */
    public static final String TitleProperty = "title";

    /** Cuantos caracteres tiene. */
    int getLength();

    /** Agrega un oyente de cambios de contenido. */
    void addDocumentListener(DocumentListener listener);

    /** Saca un oyente de cambios de contenido. */
    void removeDocumentListener(DocumentListener listener);

    /** Agrega un oyente de ediciones deshacibles. */
    void addUndoableEditListener(UndoableEditListener listener);

    /** Saca un oyente de ediciones deshacibles. */
    void removeUndoableEditListener(UndoableEditListener listener);

    /** El valor de una propiedad del documento. */
    Object getProperty(Object key);

    /** Fija una propiedad del documento. */
    void putProperty(Object key, Object value);

    /** Borra {@code length} caracteres desde {@code offs}. */
    void remove(int offs, int len) throws BadLocationException;

    /** Inserta {@code str} en {@code offset}, con esos atributos. */
    void insertString(int offset, String str, AttributeSet a) throws BadLocationException;

    /** El texto de un tramo, como {@link String}. */
    String getText(int offset, int length) throws BadLocationException;

    /**
     * El texto de un tramo, sin copiar: ver {@link Segment}.
     *
     * <p>La version que hay que usar en un camino caliente. La otra aloca.
     */
    void getText(int offset, int length, Segment txt) throws BadLocationException;

    /** Una marca en el principio, que se queda ahi. */
    Position getStartPosition();

    /** Una marca en el final, que sigue al final. */
    Position getEndPosition();

    /** Una marca en {@code offs}, que se movera con las ediciones. */
    Position createPosition(int offs) throws BadLocationException;

    /** Las raices de los arboles de estructura; ver {@link Element}. */
    Element[] getRootElements();

    /** La raiz del arbol principal. */
    Element getDefaultRootElement();

    /** Corre {@code r} con la garantia de que nadie modifica mientras tanto. */
    void render(Runnable r);
}
