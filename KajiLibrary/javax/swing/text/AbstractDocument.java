package javax.swing.text;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent$ElementChange;
import javax.swing.event.DocumentEvent$EventType;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.tree.TreeNode;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

/**
 * La base de todos los documentos: el texto por un lado, la estructura por otro, y un candado
 * entre los dos.
 *
 * <h2>Tres piezas</h2>
 *
 * <ul>
 * <li>El <strong>contenido</strong> ({@link Content}) guarda los caracteres y sabe crear
 * {@link Position}, marcas que se mueven solas cuando se inserta o se borra antes de ellas. Es la
 * pieza que hace que nada tenga que recorrer el texto para acomodar indices.
 * <li>La <strong>estructura</strong> es un arbol de {@link Element}. Cada elemento marca un tramo
 * del contenido con {@code Position}, asi que la estructura sobrevive a las ediciones sin
 * recalcularse. Que forma tiene el arbol lo decide la subclase: {@link PlainDocument} hace una
 * lista de lineas, {@link DefaultStyledDocument} hace secciones, parrafos y tramos con estilo.
 * <li>El <strong>contexto de atributos</strong> ({@link AttributeContext}) comparte los conjuntos
 * de atributos repetidos, que en un documento con estilo son casi todos.
 * </ul>
 *
 * <h2>El candado, y por que es de una sola escritura</h2>
 *
 * <p>Un documento admite muchos lectores a la vez y un solo escritor, y mientras hay un escritor no
 * lee nadie. La razon no es la velocidad: es que una edicion cambia el contenido y la estructura en
 * dos pasos, y entre esos dos pasos el documento no es consistente. {@link #render} es la forma
 * correcta de leer sin pisarse; los metodos de escritura toman el candado ellos mismos.
 *
 * <p>El escritor puede volver a entrar —un {@code insertUpdate} puede llamar a otra escritura—,
 * pero un lector que ya tiene el candado de lectura y pide escribir es un error de programa, no una
 * espera: seria un abrazo mortal consigo mismo, y por eso lanza {@link IllegalStateException}.
 *
 * <h2>Las clases anidadas son estaticas, y llevan el documento por parametro</h2>
 *
 * <p>En el JDK {@link AbstractElement} y las suyas son clases <em>internas</em>: cada elemento
 * tiene una referencia implicita a su documento y el constructor se escribe
 * {@code new BranchElement(padre, atributos)}. Aca son estaticas y el documento va primero:
 * {@code new BranchElement(doc, padre, atributos)}. La firma binaria que queda es la misma —una
 * clase interna compila su externa como primer parametro—, pero en el codigo fuente se ve.
 *
 * <p>El motivo es del compilador de este proyecto, no del diseno: una clase interna todavia no
 * puede llamar al constructor de otra interna hermana ni al {@code super(...)} de una, y no hay
 * forma de escribir esa instancia a mano (hallazgos #507 y #508). Cuando eso se arregle, esto
 * vuelve a la forma del JDK.
 *
 * <h2>Lo que no hay</h2>
 *
 * <p>No hay analisis bidireccional. {@link #getBidiRootElement} devuelve una estructura de un solo
 * tramo de izquierda a derecha, que es exactamente lo que el JDK arma para un texto que no mezcla
 * escrituras, y {@link #isLeftToRight} contesta siempre {@code true}. Un texto en arabe o hebreo se
 * guarda bien y se muestra en el orden en que esta, sin reordenar.
 */
public abstract class AbstractDocument implements Document, Serializable {

    /** El mensaje de las excepciones de posicion. */
    protected static final String BAD_LOCATION = "document location failure";

    public static final String ParagraphElementName = "paragraph";

    public static final String ContentElementName = "content";

    /** El nombre del elemento raiz de un documento con estilo. */
    public static final String SectionElementName = "section";

    /** El nombre de los elementos del arbol bidireccional. */
    public static final String BidiElementName = "bidi level";

    /** La clave del atributo que guarda el nombre de un elemento. */
    public static final String ElementNameAttribute = "$ename";

    /** La propiedad que marca un documento con texto internacional. */
    static final String I18NProperty = "i18n";

    /** La propiedad que marca contenido de varios bytes. */
    static final Object MultiByteProperty = "multiByte";

    /** La propiedad con la prioridad de carga asincronica. */
    static final String AsyncLoadPriority = "load priority";

    protected EventListenerList listenerList = new EventListenerList();

    private transient Content data;
    private transient AttributeContext context;
    private transient BranchElement bidiRoot;
    private transient Dictionary<Object, Object> documentProperties;
    private transient Thread currentWriter = null;
    private transient int numWriters = 0;
    private transient int numReaders = 0;
    private transient boolean notifyingListeners = false;
    private transient DocumentFilter documentFilter;
    private transient DocumentFilterFilterBypass filterBypass;

    /** Un documento sobre ese contenido, con el contexto de estilos compartido. */
    protected AbstractDocument(Content data) {
        this(data, StyleContext.getDefaultStyleContext());
    }

    protected AbstractDocument(Content data, AttributeContext context) {
        this.data = data;
        this.context = context;

        // La raiz y el tramo bidi son un BranchElement y un LeafElement con su nombre puesto por
        // atributo, y no dos subclases: una clase interna no puede llamar al `super(...)` de otra
        // interna hermana con nuestro javac (#508), y el nombre por atributo da lo mismo.
        // Con el candado tomado: crear un elemento con atributos es escribir en el documento, y
        // el propio elemento lo comprueba.
        writeLock();
        try {
            bidiRoot = new BranchElement(this, null, nombreBidi("bidi root"));
            Element[] p = new Element[1];
            p[0] = new LeafElement(this, bidiRoot, nivelBidi(0), 0, 0);
            bidiRoot.replace(0, 0, p);
        } finally {
            writeUnlock();
        }
    }

    /** Las propiedades del documento —titulo, juego de caracteres—; se crean al primer uso. */
    public Dictionary<Object, Object> getDocumentProperties() {
        if (documentProperties == null) {
            documentProperties = new Hashtable<Object, Object>(2);
        }
        return documentProperties;
    }

    public void setDocumentProperties(Dictionary<Object, Object> x) {
        documentProperties = x;
    }

    /** Avisa una insercion; se llama con el candado de escritura tomado. */
    protected void fireInsertUpdate(DocumentEvent e) {
        notifyingListeners = true;
        try {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i = i - 2) {
                if (listeners[i] == DocumentListener.class) {
                    ((DocumentListener) listeners[i + 1]).insertUpdate(e);
                }
            }
        } finally {
            notifyingListeners = false;
        }
    }

    protected void fireChangedUpdate(DocumentEvent e) {
        notifyingListeners = true;
        try {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i = i - 2) {
                if (listeners[i] == DocumentListener.class) {
                    ((DocumentListener) listeners[i + 1]).changedUpdate(e);
                }
            }
        } finally {
            notifyingListeners = false;
        }
    }

    protected void fireRemoveUpdate(DocumentEvent e) {
        notifyingListeners = true;
        try {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i = i - 2) {
                if (listeners[i] == DocumentListener.class) {
                    ((DocumentListener) listeners[i + 1]).removeUpdate(e);
                }
            }
        } finally {
            notifyingListeners = false;
        }
    }

    protected void fireUndoableEditUpdate(UndoableEditEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == UndoableEditListener.class) {
                ((UndoableEditListener) listeners[i + 1]).undoableEditHappened(e);
            }
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /** La prioridad de carga asincronica; negativa significa cargar en el hilo que pide. */
    public int getAsynchronousLoadPriority() {
        Integer loadPriority = (Integer) getProperty(AbstractDocument.AsyncLoadPriority);
        if (loadPriority != null) {
            return loadPriority.intValue();
        }
        return -1;
    }

    public void setAsynchronousLoadPriority(int p) {
        Integer loadPriority = (p < 0) ? null : Integer.valueOf(p);
        putProperty(AbstractDocument.AsyncLoadPriority, loadPriority);
    }

    /** El filtro que puede vetar o cambiar cada edicion; ver {@link DocumentFilter}. */
    public void setDocumentFilter(DocumentFilter filter) {
        documentFilter = filter;
    }

    public DocumentFilter getDocumentFilter() {
        return documentFilter;
    }

    /** Corre eso con el candado de lectura tomado; ver la nota de la clase. */
    public void render(Runnable r) {
        readLock();
        try {
            r.run();
        } finally {
            readUnlock();
        }
    }

    public int getLength() {
        return data.length() - 1;
    }

    public void addDocumentListener(DocumentListener listener) {
        listenerList.add(DocumentListener.class, listener);
    }

    public void removeDocumentListener(DocumentListener listener) {
        listenerList.remove(DocumentListener.class, listener);
    }

    public DocumentListener[] getDocumentListeners() {
        return listenerList.getListeners(DocumentListener.class);
    }

    public void addUndoableEditListener(UndoableEditListener listener) {
        listenerList.add(UndoableEditListener.class, listener);
    }

    public void removeUndoableEditListener(UndoableEditListener listener) {
        listenerList.remove(UndoableEditListener.class, listener);
    }

    public UndoableEditListener[] getUndoableEditListeners() {
        return listenerList.getListeners(UndoableEditListener.class);
    }

    public final Object getProperty(Object key) {
        return getDocumentProperties().get(key);
    }

    public final void putProperty(Object key, Object value) {
        if (value != null) {
            getDocumentProperties().put(key, value);
        } else {
            getDocumentProperties().remove(key);
        }
    }

    /**
     * Borra un tramo; pasa por el filtro si hay.
     *
     * <p>Borrar cero caracteres no hace nada, ni siquiera avisa: es lo que hace que un
     * {@code replace} sin borrado no produzca un evento de mas.
     */
    public void remove(int offs, int len) throws BadLocationException {
        DocumentFilter filter = getDocumentFilter();

        writeLock();
        try {
            if (filter != null) {
                filter.remove(getFilterBypass(), offs, len);
            } else {
                handleRemove(offs, len);
            }
        } finally {
            writeUnlock();
        }
    }

    /** El borrado de verdad, ya pasado el filtro. */
    void handleRemove(int offs, int len) throws BadLocationException {
        if (len > 0) {
            if (offs < 0 || (offs + len) > getLength()) {
                throw new BadLocationException("Invalid remove", getLength() + 1);
            }
            DefaultDocumentEvent chng = new DefaultDocumentEvent(this, offs, len,
                    DocumentEvent$EventType.REMOVE);

            boolean isComposedTextElement = false;

            removeUpdate(chng);
            UndoableEdit u = data.remove(offs, len);
            if (u != null) {
                chng.addEdit(u);
            }
            postRemoveUpdate(chng);
            updateBidi(chng);
            chng.end();
            fireRemoveUpdate(chng);
            if ((u != null) && !isComposedTextElement) {
                fireUndoableEditUpdate(new UndoableEditEvent(this, chng));
            }
        }
    }

    /**
     * Borra e inserta como una sola operacion.
     *
     * <p>Que sea una sola importa para deshacer: sin esto, deshacer un reemplazo dejaria el texto
     * borrado y sin reponer. Sin filtro, son un borrado y una insercion, y por eso salen dos
     * eventos; con filtro, el filtro decide.
     */
    public void replace(int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        if (length == 0 && (text == null || text.length() == 0)) {
            return;
        }
        DocumentFilter filter = getDocumentFilter();

        writeLock();
        try {
            if (filter != null) {
                filter.replace(getFilterBypass(), offset, length, text, attrs);
            } else {
                if (length > 0) {
                    remove(offset, length);
                }
                if (text != null && text.length() > 0) {
                    insertString(offset, text, attrs);
                }
            }
        } finally {
            writeUnlock();
        }
    }

    /** Inserta texto; pasa por el filtro si hay. */
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        if ((str == null) || (str.length() == 0)) {
            return;
        }
        DocumentFilter filter = getDocumentFilter();

        writeLock();
        try {
            if (filter != null) {
                filter.insertString(getFilterBypass(), offs, str, a);
            } else {
                handleInsertString(offs, str, a);
            }
        } finally {
            writeUnlock();
        }
    }

    /** La insercion de verdad, ya pasada por el filtro. */
    void handleInsertString(int offs, String str, AttributeSet a) throws BadLocationException {
        if ((str == null) || (str.length() == 0)) {
            return;
        }
        UndoableEdit u = data.insertString(offs, str);
        DefaultDocumentEvent e = new DefaultDocumentEvent(this, offs, str.length(),
                DocumentEvent$EventType.INSERT);
        if (u != null) {
            e.addEdit(u);
        }

        insertUpdate(e, a);
        updateBidi(e);

        e.end();
        fireInsertUpdate(e);
        if (u != null) {
            fireUndoableEditUpdate(new UndoableEditEvent(this, e));
        }
    }

    public String getText(int offset, int length) throws BadLocationException {
        if (length < 0) {
            throw new BadLocationException("Length must be positive", length);
        }
        String str = data.getString(offset, length);
        return str;
    }

    /**
     * El texto sin copiarlo: el segmento apunta al arreglo del contenido cuando se puede.
     *
     * <p>Es la forma que usa el dibujado, que recorre el texto muchas veces por segundo y no
     * puede permitirse una copia por cuadro.
     */
    public void getText(int offset, int length, Segment txt) throws BadLocationException {
        if (length < 0) {
            throw new BadLocationException("Length must be positive", length);
        }
        data.getChars(offset, length, txt);
    }

    /** Una marca que se mueve con el texto; ver la nota de la clase. */
    public synchronized Position createPosition(int offs) throws BadLocationException {
        return data.createPosition(offs);
    }

    /** El principio del documento; nunca se mueve. */
    public final Position getStartPosition() {
        Position p;
        try {
            p = createPosition(0);
        } catch (BadLocationException bl) {
            p = null;
        }
        return p;
    }

    /** El final del documento; se corre con cada insercion. */
    public final Position getEndPosition() {
        Position p;
        try {
            p = createPosition(data.length());
        } catch (BadLocationException bl) {
            p = null;
        }
        return p;
    }

    /** Las dos raices: la de la estructura y la bidireccional. */
    public Element[] getRootElements() {
        Element[] elems = new Element[2];
        elems[0] = getDefaultRootElement();
        elems[1] = getBidiRootElement();
        return elems;
    }

    public abstract Element getDefaultRootElement();

    /** Ver la nota de la clase sobre lo que no hay. */
    public Element getBidiRootElement() {
        return bidiRoot;
    }

    /** Siempre {@code true}; ver la nota de la clase. */
    static boolean isLeftToRight(Document doc, int p0, int p1) {
        return true;
    }

    public abstract Element getParagraphElement(int pos);

    protected final AttributeContext getAttributeContext() {
        return context;
    }

    /**
     * La estructura, despues de una insercion en el contenido.
     *
     * <p>La subclase la redefine para acomodar su arbol. La version de aca no hace nada: un
     * documento sin estructura propia no tiene que acomodar nada.
     */
    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr) {
    }

    /** La estructura, antes de sacar el texto del contenido. */
    protected void removeUpdate(DefaultDocumentEvent chng) {
    }

    /** La estructura, despues de sacar el texto; aca ya no se puede leer lo borrado. */
    protected void postRemoveUpdate(DefaultDocumentEvent chng) {
    }

    /** Los atributos de un elemento bidi con ese nombre. */
    private AttributeSet nombreBidi(String nombre) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(ElementNameAttribute, nombre);
        return a;
    }

    /** Los atributos de un tramo bidi de ese nivel. */
    private AttributeSet nivelBidi(int nivel) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(ElementNameAttribute, BidiElementName);
        a.addAttribute(StyleConstants.BidiLevel, Integer.valueOf(nivel));
        return a;
    }

    /**
     * Rehace el tramo bidireccional para que cubra el documento entero.
     *
     * <p>Un solo tramo, de izquierda a derecha; ver la nota de la clase. Llega hasta
     * {@code getLength() + 1}, o sea incluye el fin de linea implicito del final: el tramo
     * bidireccional cubre el contenido, no el texto que el usuario ve. Se rehace en vez de moverse
     * porque ese final no es una {@link Position} que se corra sola.
     */
    void updateBidi(DefaultDocumentEvent chng) {
        Element[] p = new Element[1];
        p[0] = new LeafElement(this, bidiRoot, nivelBidi(0), 0, getLength() + 1);
        int n = bidiRoot.getElementCount();
        bidiRoot.replace(0, n, p);
    }

    /** Imprime el arbol; es la herramienta de depuracion de un documento. */
    public void dump(PrintStream out) {
        Element root = getDefaultRootElement();
        if (root instanceof AbstractElement) {
            ((AbstractElement) root).dump(out, 0);
        }
        ((AbstractElement) getBidiRootElement()).dump(out, 0);
    }

    protected final Content getContent() {
        return data;
    }

    /** Un elemento hoja: un tramo de texto con atributos. */
    protected Element createLeafElement(Element parent, AttributeSet a, int p0, int p1) {
        return new LeafElement(this, parent, a, p0, p1);
    }

    /** Un elemento rama: un elemento con hijos, cuyo rango es el de ellos. */
    protected Element createBranchElement(Element parent, AttributeSet a) {
        return new BranchElement(this, parent, a);
    }

    protected final synchronized Thread getCurrentWriter() {
        return currentWriter;
    }

    /**
     * Toma el candado de escritura; espera a que no quede ningun lector.
     *
     * <p>Un escritor que ya lo tiene vuelve a entrar sin esperar. Un lector que pide escribir es un
     * error de programa; ver la nota de la clase.
     */
    protected final synchronized void writeLock() {
        try {
            while ((numReaders > 0) || (currentWriter != null)) {
                if (Thread.currentThread() == currentWriter) {
                    if (notifyingListeners) {
                        // Un escucha no puede modificar el documento: los demas escuchas
                        // recibirian un evento que ya no describe lo que hay.
                        throw new IllegalStateException(
                                "Attempt to mutate in notification");
                    }
                    numWriters = numWriters + 1;
                    return;
                }
                wait();
            }
            currentWriter = Thread.currentThread();
            numWriters = 1;
        } catch (InterruptedException e) {
            throw new Error("Interrupted attempt to acquire write lock");
        }
    }

    protected final synchronized void writeUnlock() {
        numWriters = numWriters - 1;
        if (numWriters <= 0) {
            numWriters = 0;
            currentWriter = null;
            notifyAll();
        }
    }

    public final synchronized void readLock() {
        try {
            while (currentWriter != null) {
                if (currentWriter == Thread.currentThread()) {
                    // El escritor puede leer lo que el mismo esta escribiendo.
                    return;
                }
                wait();
            }
            numReaders = numReaders + 1;
        } catch (InterruptedException e) {
            throw new Error("Interrupted attempt to acquire read lock");
        }
    }

    public final synchronized void readUnlock() {
        if (currentWriter == Thread.currentThread()) {
            return;
        }
        if (numReaders <= 0) {
            throw new StateInvariantError("Unbalanced readLock/readUnlock");
        }
        numReaders = numReaders - 1;
        notify();
    }

    private DocumentFilter.FilterBypass getFilterBypass() {
        if (filterBypass == null) {
            filterBypass = new DocumentFilterFilterBypass();
        }
        return filterBypass;
    }

    /** El atajo que el filtro usa para escribir sin volver a pasar por si mismo. */
    private class DocumentFilterFilterBypass extends DocumentFilter.FilterBypass {

        public Document getDocument() {
            return AbstractDocument.this;
        }

        public void remove(int offset, int length) throws BadLocationException {
            handleRemove(offset, length);
        }

        public void insertString(int offset, String string, AttributeSet attr)
                throws BadLocationException {
            handleInsertString(offset, string, attr);
        }

        public void replace(int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (length > 0) {
                handleRemove(offset, length);
            }
            if (text != null && text.length() > 0) {
                handleInsertString(offset, text, attrs);
            }
        }
    }

    /**
     * Lo que guarda los caracteres.
     *
     * <p>Separado del documento porque hay mas de una forma razonable de guardarlos: un hueco
     * movil que hace baratas las ediciones seguidas en un mismo lugar ({@link GapContent}), o una
     * cadena simple ({@link StringContent}). Las {@link Position} son responsabilidad de esta
     * pieza porque solo ella sabe cuando se corren los caracteres.
     *
     * <p>El contenido siempre tiene al menos un caracter, un fin de linea implicito, y por eso
     * {@link AbstractDocument#getLength} resta uno.
     */
    public interface Content {

        Position createPosition(int offset) throws BadLocationException;

        int length();

        UndoableEdit insertString(int where, String str) throws BadLocationException;

        UndoableEdit remove(int where, int nitems) throws BadLocationException;

        String getString(int where, int len) throws BadLocationException;

        /** El texto sin copiarlo cuando se puede; ver {@link AbstractDocument#getText}. */
        void getChars(int where, int len, Segment txt) throws BadLocationException;
    }

    /**
     * Quien comparte los conjuntos de atributos.
     *
     * <p>Los conjuntos son inmutables: cambiar un atributo devuelve otro conjunto. Suena caro y es
     * al reves: un documento con mil parrafos en cursiva guarda un solo conjunto "cursiva", y
     * comparar dos tramos es comparar dos referencias.
     */
    public interface AttributeContext {

        AttributeSet addAttribute(AttributeSet old, Object name, Object value);

        AttributeSet addAttributes(AttributeSet old, AttributeSet attr);

        AttributeSet removeAttribute(AttributeSet old, Object name);

        AttributeSet removeAttributes(AttributeSet old, Enumeration<?> names);

        AttributeSet removeAttributes(AttributeSet old, AttributeSet attrs);

        AttributeSet getEmptySet();

        /** Avisa que ese conjunto ya no se usa; el contexto decide si lo tira. */
        void reclaim(AttributeSet a);
    }

    /**
     * El cambio que sufrio un elemento: que hijos se fueron y cuales llegaron.
     *
     * <p>Es tambien una edicion deshacible, y por eso guarda las dos listas: deshacer es poner de
     * vuelta los que se fueron.
     */
    public static class ElementEdit extends AbstractUndoableEdit implements
            DocumentEvent$ElementChange {

        private Element e;
        private int index;
        private Element[] removed;
        private Element[] added;

        public ElementEdit(Element e, int index, Element[] removed, Element[] added) {
            super();
            this.e = e;
            this.index = index;
            this.removed = removed;
            this.added = added;
        }

        public Element getElement() {
            return e;
        }

        /** Donde empezo el cambio, contando hijos. */
        public int getIndex() {
            return index;
        }

        public Element[] getChildrenRemoved() {
            return removed;
        }

        public Element[] getChildrenAdded() {
            return added;
        }

        public void redo() throws CannotRedoException {
            super.redo();
            Element[] tmp = removed;
            removed = added;
            added = tmp;
            ((AbstractDocument.BranchElement) e).replace(index, removed.length, added);
        }

        public void undo() throws CannotUndoException {
            super.undo();
            ((AbstractDocument.BranchElement) e).replace(index, added.length, removed);
            Element[] tmp = removed;
            removed = added;
            added = tmp;
        }
    }

    /**
     * Un elemento del arbol, que ademas es su propio conjunto de atributos.
     *
     * <p>Esa union no es pereza: los atributos de un elemento se consultan tantas veces como el
     * elemento, y tenerlos en el mismo objeto ahorra un salto por consulta. Los atributos de
     * verdad viven en el contexto compartido; este objeto guarda una referencia al conjunto
     * inmutable y la reemplaza cada vez que cambia.
     */
    public abstract static class AbstractElement implements Element, MutableAttributeSet,
            Serializable, TreeNode {

        /** El documento al que pertenece; ver la nota sobre las clases anidadas. */
        protected final AbstractDocument documento;

        private Element parent;
        private transient AttributeSet attributes;

        public AbstractElement(AbstractDocument documento, Element parent, AttributeSet a) {
            this.documento = documento;
            this.parent = parent;
            attributes = documento.getAttributeContext().getEmptySet();
            if (a != null) {
                addAttributes(a);
            }
        }

        /** Imprime este elemento y sus hijos, sangrados; ver {@link AbstractDocument#dump}. */
        public void dump(PrintStream psOut, int indentAmount) {
            String indentation = "";
            for (int i = 0; i < indentAmount; i++) {
                indentation = indentation + "  ";
            }
            psOut.print(indentation + "<" + getName());
            Enumeration<?> names = getAttributeNames();
            while (names.hasMoreElements()) {
                Object name = names.nextElement();
                psOut.print(" " + name + "=" + getAttribute(name));
            }
            psOut.println(">");

            if (getElementCount() == 0) {
                int start = getStartOffset();
                int end = getEndOffset();
                psOut.print(indentation + "  [" + start + "," + end + "]");
                try {
                    String text = getDocument().getText(start, end - start);
                    psOut.print("[" + text + "]");
                } catch (BadLocationException e) {
                    // Un elemento puede estar fuera de rango mientras se edita; no es un error.
                }
                psOut.println("");
            } else {
                for (int i = 0; i < getElementCount(); i++) {
                    ((AbstractElement) getElement(i)).dump(psOut, indentAmount + 1);
                }
            }
        }

        public int getAttributeCount() {
            return attributes.getAttributeCount();
        }

        public boolean isDefined(Object attrName) {
            return attributes.isDefined(attrName);
        }

        public boolean isEqual(AttributeSet attr) {
            return attributes.isEqual(attr);
        }

        public AttributeSet copyAttributes() {
            return attributes.copyAttributes();
        }

        public Object getAttribute(Object attrName) {
            Object value = attributes.getAttribute(attrName);
            if (value == null) {
                // El padre del arbol hace de padre de resolucion: un parrafo hereda del documento.
                AttributeSet a = attributes.getResolveParent();
                if (a == null && parent != null) {
                    a = parent.getAttributes();
                }
                if (a != null) {
                    value = a.getAttribute(attrName);
                }
            }
            return value;
        }

        public Enumeration<?> getAttributeNames() {
            return attributes.getAttributeNames();
        }

        public boolean containsAttribute(Object name, Object value) {
            return attributes.containsAttribute(name, value);
        }

        public boolean containsAttributes(AttributeSet attrs) {
            return attributes.containsAttributes(attrs);
        }

        public AttributeSet getResolveParent() {
            AttributeSet a = attributes.getResolveParent();
            if ((a == null) && (parent != null)) {
                a = parent.getAttributes();
            }
            return a;
        }

        public void addAttribute(Object name, Object value) {
            checkForIllegalCast();
            AttributeContext context = documento.getAttributeContext();
            attributes = context.addAttribute(attributes, name, value);
        }

        public void addAttributes(AttributeSet attr) {
            checkForIllegalCast();
            AttributeContext context = documento.getAttributeContext();
            attributes = context.addAttributes(attributes, attr);
        }

        public void removeAttribute(Object name) {
            checkForIllegalCast();
            AttributeContext context = documento.getAttributeContext();
            attributes = context.removeAttribute(attributes, name);
        }

        public void removeAttributes(Enumeration<?> names) {
            checkForIllegalCast();
            AttributeContext context = documento.getAttributeContext();
            attributes = context.removeAttributes(attributes, names);
        }

        public void removeAttributes(AttributeSet attrs) {
            checkForIllegalCast();
            AttributeContext context = documento.getAttributeContext();
            if (attrs == this) {
                attributes = context.getEmptySet();
            } else {
                attributes = context.removeAttributes(attributes, attrs);
            }
        }

        public void setResolveParent(AttributeSet parent) {
            checkForIllegalCast();
            AttributeContext context = documento.getAttributeContext();
            if (parent != null) {
                attributes = context.addAttribute(attributes, StyleConstants.ResolveAttribute,
                        parent);
            } else {
                attributes = context.removeAttribute(attributes,
                        StyleConstants.ResolveAttribute);
            }
        }

        /** Cambiar atributos es escribir en el documento: hace falta el candado. */
        private void checkForIllegalCast() {
            Thread t = documento.getCurrentWriter();
            if ((t == null) || (t != Thread.currentThread())) {
                throw new StateInvariantError("Illegal cast to MutableAttributeSet");
            }
        }

        public Document getDocument() {
            return documento;
        }

        public Element getParentElement() {
            return parent;
        }

        public AttributeSet getAttributes() {
            return this;
        }

        /** El nombre del elemento, del atributo que lo guarda. */
        public String getName() {
            if (attributes.isDefined(ElementNameAttribute)) {
                return (String) attributes.getAttribute(ElementNameAttribute);
            }
            return null;
        }

        public abstract int getStartOffset();

        public abstract int getEndOffset();

        public abstract Element getElement(int index);

        public abstract int getElementCount();

        public abstract int getElementIndex(int offset);

        public abstract boolean isLeaf();

        // -- TreeNode: la misma estructura, vista como arbol ------------------------------------

        public TreeNode getChildAt(int childIndex) {
            return (AbstractElement) getElement(childIndex);
        }

        public int getChildCount() {
            return getElementCount();
        }

        public TreeNode getParent() {
            return (AbstractElement) getParentElement();
        }

        public int getIndex(TreeNode node) {
            for (int counter = getChildCount() - 1; counter >= 0; counter--) {
                if (getChildAt(counter) == node) {
                    return counter;
                }
            }
            return -1;
        }

        public abstract boolean getAllowsChildren();

        public abstract Enumeration<TreeNode> children();
    }

    /** Un elemento con hijos; su rango es el de ellos. */
    public static class BranchElement extends AbstractElement {

        private AbstractElement[] children;
        private int nchildren;
        private int lastIndex;

        public BranchElement(AbstractDocument documento, Element parent, AttributeSet a) {
            super(documento, parent, a);
            children = new AbstractElement[1];
            nchildren = 0;
            lastIndex = -1;
        }

        /** El hijo que contiene esa posicion, o {@code null} si esta fuera. */
        public Element positionToElement(int pos) {
            int index = getElementIndex(pos);
            Element child = children[index];
            int p0 = child.getStartOffset();
            int p1 = child.getEndOffset();
            if ((pos >= p0) && (pos < p1)) {
                return child;
            }
            return null;
        }

        /** Cambia un tramo de hijos por otro; es la operacion basica de reestructurar. */
        public void replace(int offset, int length, Element[] elems) {
            int delta = elems.length - length;
            int src = offset + length;
            int nmove = nchildren - src;
            int dest = src + delta;
            if ((nchildren + delta) >= children.length) {
                // El arreglo crece de a duplicaciones: reestructurar es frecuente.
                int newLength = Math.max(2 * children.length, nchildren + delta);
                AbstractElement[] newChildren = new AbstractElement[newLength];
                System.arraycopy(children, 0, newChildren, 0, offset);
                System.arraycopy(elems, 0, newChildren, offset, elems.length);
                System.arraycopy(children, src, newChildren, dest, nmove);
                children = newChildren;
            } else {
                System.arraycopy(children, src, children, dest, nmove);
                System.arraycopy(elems, 0, children, offset, elems.length);
            }
            nchildren = nchildren + delta;
        }

        public String toString() {
            return "BranchElement(" + getName() + ") " + getStartOffset() + ","
                    + getEndOffset() + "\n";
        }

        public String getName() {
            String nm = super.getName();
            if (nm == null) {
                nm = ParagraphElementName;
            }
            return nm;
        }

        public int getStartOffset() {
            return children[0].getStartOffset();
        }

        public int getEndOffset() {
            Element child = (nchildren > 0) ? children[nchildren - 1] : children[0];
            return child.getEndOffset();
        }

        public Element getElement(int index) {
            if (index < nchildren) {
                return children[index];
            }
            return null;
        }

        public int getElementCount() {
            return nchildren;
        }

        /**
         * El indice del hijo que contiene esa posicion, por busqueda binaria.
         *
         * <p>Guarda el ultimo resultado y lo prueba primero: las consultas vienen casi siempre en
         * orden, recorriendo el texto, y asi la mayoria no hace ninguna comparacion de mas.
         */
        public int getElementIndex(int offset) {
            int index;
            int lower = 0;
            int upper = nchildren - 1;
            int mid = 0;
            int p0 = getStartOffset();
            int p1;

            if (nchildren == 0) {
                return 0;
            }
            if (offset >= getEndOffset()) {
                return nchildren - 1;
            }

            if (lastIndex >= lower && lastIndex <= upper) {
                Element lastHit = children[lastIndex];
                p0 = lastHit.getStartOffset();
                p1 = lastHit.getEndOffset();
                if ((offset >= p0) && (offset < p1)) {
                    return lastIndex;
                }
                if (offset < p0) {
                    upper = lastIndex;
                } else {
                    lower = lastIndex;
                }
            }

            while (lower <= upper) {
                mid = lower + ((upper - lower) / 2);
                Element elem = children[mid];
                p0 = elem.getStartOffset();
                p1 = elem.getEndOffset();
                if ((offset >= p0) && (offset < p1)) {
                    lastIndex = mid;
                    return mid;
                } else if (offset < p0) {
                    upper = mid - 1;
                } else {
                    lower = mid + 1;
                }
            }

            // Sin coincidencia exacta: el mas cercano, que es lo que quiere quien busca un hueco.
            if (offset < p0) {
                index = mid;
            } else {
                index = mid + 1;
            }
            lastIndex = index;
            return index;
        }

        public boolean isLeaf() {
            return false;
        }

        public boolean getAllowsChildren() {
            return true;
        }

        public Enumeration<TreeNode> children() {
            if (nchildren == 0) {
                return null;
            }
            Vector<TreeNode> tempVector = new Vector<TreeNode>(nchildren);
            for (int counter = 0; counter < nchildren; counter++) {
                tempVector.addElement(children[counter]);
            }
            return tempVector.elements();
        }
    }

    /** Un elemento sin hijos: un tramo de texto marcado con dos {@link Position}. */
    public static class LeafElement extends AbstractElement {

        private transient Position p0;
        private transient Position p1;

        public LeafElement(AbstractDocument documento, Element parent, AttributeSet a,
                int offs0, int offs1) {
            super(documento, parent, a);
            try {
                p0 = documento.createPosition(offs0);
                p1 = documento.createPosition(offs1);
            } catch (BadLocationException e) {
                p0 = null;
                p1 = null;
                throw new StateInvariantError("Can't create Position references");
            }
        }

        public String toString() {
            return "LeafElement(" + getName() + ") " + p0 + "," + p1 + "\n";
        }

        public int getStartOffset() {
            return p0.getOffset();
        }

        public int getEndOffset() {
            return p1.getOffset();
        }

        public String getName() {
            String nm = super.getName();
            if (nm == null) {
                nm = ContentElementName;
            }
            return nm;
        }

        /** Cero: un elemento hoja no tiene hijos entre los que buscar. */
        public int getElementIndex(int pos) {
            return -1;
        }

        public Element getElement(int index) {
            return null;
        }

        public int getElementCount() {
            return 0;
        }

        public boolean isLeaf() {
            return true;
        }

        public boolean getAllowsChildren() {
            return false;
        }

        public Enumeration<TreeNode> children() {
            return null;
        }
    }

    /**
     * El cambio que sufrio el documento, y a la vez la edicion que lo deshace.
     *
     * <p>Que sean el mismo objeto es lo que hace que deshacer sea barato: el evento ya tiene
     * adentro cada pedacito de la edicion —el texto y los cambios de estructura—, en orden.
     */
    public static class DefaultDocumentEvent extends CompoundEdit implements DocumentEvent {

        /** El documento al que pertenece; ver la nota sobre las clases anidadas. */
        protected final AbstractDocument documento;

        private int offset;
        private int length;
        private Hashtable<Element, DocumentEvent$ElementChange> changeLookup;
        private DocumentEvent$EventType type;

        public DefaultDocumentEvent(AbstractDocument documento, int offs, int len,
                DocumentEvent$EventType type) {
            super();
            this.documento = documento;
            offset = offs;
            length = len;
            this.type = type;
        }

        public String toString() {
            return edits.toString();
        }

        /** Un cambio de estructura se indexa por elemento, para que {@link #getChange} sea barato. */
        public boolean addEdit(UndoableEdit anEdit) {
            if ((changeLookup == null) && (anEdit instanceof DocumentEvent$ElementChange)) {
                changeLookup = new Hashtable<Element, DocumentEvent$ElementChange>();
            }
            if (changeLookup != null && (anEdit instanceof DocumentEvent$ElementChange)) {
                DocumentEvent$ElementChange ec = (DocumentEvent$ElementChange) anEdit;
                changeLookup.put(ec.getElement(), ec);
            }
            return super.addEdit(anEdit);
        }

        public void redo() throws CannotRedoException {
            documento.writeLock();
            try {
                super.redo();
                if (type == DocumentEvent$EventType.INSERT) {
                    documento.fireInsertUpdate(this);
                } else if (type == DocumentEvent$EventType.REMOVE) {
                    documento.fireRemoveUpdate(this);
                } else {
                    documento.fireChangedUpdate(this);
                }
            } finally {
                documento.writeUnlock();
            }
        }

        /** Deshacer un alta es una baja: el evento que se avisa es el contrario. */
        public void undo() throws CannotUndoException {
            documento.writeLock();
            try {
                super.undo();
                if (type == DocumentEvent$EventType.REMOVE) {
                    documento.fireInsertUpdate(this);
                } else if (type == DocumentEvent$EventType.INSERT) {
                    documento.fireRemoveUpdate(this);
                } else {
                    documento.fireChangedUpdate(this);
                }
            } finally {
                documento.writeUnlock();
            }
        }

        public boolean isSignificant() {
            return true;
        }

        public String getPresentationName() {
            DocumentEvent$EventType type = getType();
            if (type == DocumentEvent$EventType.INSERT) {
                return "addition";
            }
            if (type == DocumentEvent$EventType.REMOVE) {
                return "deletion";
            }
            return "style change";
        }

        public String getUndoPresentationName() {
            return "Undo " + getPresentationName();
        }

        public String getRedoPresentationName() {
            return "Redo " + getPresentationName();
        }

        public DocumentEvent$EventType getType() {
            return type;
        }

        public int getOffset() {
            return offset;
        }

        public int getLength() {
            return length;
        }

        public Document getDocument() {
            return documento;
        }

        public DocumentEvent$ElementChange getChange(Element elem) {
            if (changeLookup != null) {
                return changeLookup.get(elem);
            }
            return null;
        }
    }
}
