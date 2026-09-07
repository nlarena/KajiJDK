package javax.swing.text;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Color;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * La bolsa donde viven los estilos y los conjuntos de atributos compartidos.
 *
 * <h2>Por que compartir</h2>
 *
 * <p>Un documento con estilo tiene un conjunto de atributos por cada tramo de texto, y esos
 * conjuntos se repiten muchisimo: todo lo que esta en cursiva tiene el mismo. El contexto los
 * <em>internaliza</em>: al pedir "este conjunto mas negrita" devuelve un objeto inmutable y
 * compartido, asi que mil tramos en negrita son una sola instancia y comparar dos tramos es
 * comparar dos referencias.
 *
 * <p>Los conjuntos chicos se guardan como un arreglo plano de pares nombre-valor
 * ({@link SmallAttributeSet}), que para pocos atributos es mas rapido y mas chico que una tabla.
 * Pasado el umbral de {@link #getCompressionThreshold} se usa una tabla y se deja de compartir:
 * un conjunto grande casi nunca se repite, y buscarle un gemelo costaria mas de lo que ahorra.
 *
 * <h2>Los estilos</h2>
 *
 * <p>Un {@link Style} es un conjunto con nombre que avisa cuando cambia, y los estilos se
 * encadenan por su padre de resolucion. El contexto siempre tiene uno, {@link #DEFAULT_STYLE},
 * que es el ultimo eslabon de esa cadena.
 *
 * <h2>Lo que no esta</h2>
 *
 * <p>{@link #writeAttributes} y {@link #readAttributes} y sus versiones estaticas serializan
 * atributos, y necesitan que las claves estaticas esten registradas para poder escribirlas por
 * nombre. El registro esta ({@link #registerStaticAttributeKey}) y las claves de
 * {@link StyleConstants} se registran solas; lo que no esta es la serializacion, que en esta VM no
 * se puede probar contra nada.
 */
public class StyleContext implements Serializable, AbstractDocument.AttributeContext {

    /** El nombre del estilo del que cuelgan todos los demas. */
    public static final String DEFAULT_STYLE = "default";

    /** A partir de cuantos atributos se deja de compartir; ver la nota de la clase. */
    static final int THRESHOLD = 9;

    private static StyleContext defaultContext;

    private static Hashtable<Object, String> freezeKeyMap;
    private static Hashtable<String, Object> thawKeyMap;

    private Style styles;
    private transient FontKey fontSearch = new FontKey(null, 0, 0);
    private transient Hashtable<FontKey, Font> fontTable = new Hashtable<FontKey, Font>();
    private transient Hashtable<SmallAttributeSet, SmallAttributeSet> attributesPool =
            new Hashtable<SmallAttributeSet, SmallAttributeSet>();
    private transient MutableAttributeSet search = new SimpleAttributeSet();

    /** El contexto que usan los documentos que no piden uno propio. */
    public static final StyleContext getDefaultStyleContext() {
        if (defaultContext == null) {
            defaultContext = new StyleContext();
        }
        return defaultContext;
    }

    /** Un contexto con su estilo por omision vacio. */
    public StyleContext() {
        styles = new NamedStyle(null);
        addStyle(DEFAULT_STYLE, null);
    }

    /**
     * Agrega un estilo con ese nombre y ese padre.
     *
     * <p>Un nombre {@code null} crea un estilo anonimo: sirve igual para colgarse de el, pero no
     * se puede pedir por nombre despues.
     */
    public Style addStyle(String nm, Style parent) {
        Style style = new NamedStyle(nm, parent);
        if (nm != null) {
            styles.addAttribute(nm, style);
        }
        return style;
    }

    public void removeStyle(String nm) {
        styles.removeAttribute(nm);
    }

    public Style getStyle(String nm) {
        return (Style) styles.getAttribute(nm);
    }

    public Enumeration<?> getStyleNames() {
        return styles.getAttributeNames();
    }

    /** Escucha los cambios de <em>cualquier</em> estilo del contexto. */
    public void addChangeListener(ChangeListener l) {
        ((NamedStyle) styles).addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l) {
        ((NamedStyle) styles).removeChangeListener(l);
    }

    public ChangeListener[] getChangeListeners() {
        return ((NamedStyle) styles).getChangeListeners();
    }

    /** La fuente que describen esos atributos. */
    public Font getFont(AttributeSet attr) {
        int style = Font.PLAIN;
        if (StyleConstants.isBold(attr)) {
            style = style | Font.BOLD;
        }
        if (StyleConstants.isItalic(attr)) {
            style = style | Font.ITALIC;
        }
        String family = StyleConstants.getFontFamily(attr);
        int size = StyleConstants.getFontSize(attr);

        // El superindice y el subindice se dibujan mas chicos; es lo unico que cambia el cuerpo.
        if (StyleConstants.isSuperscript(attr) || StyleConstants.isSubscript(attr)) {
            size = size - 2;
        }

        return getFont(family, style, size);
    }

    /** El color de frente de esos atributos; ver {@link StyleConstants#getForeground}. */
    public Color getForeground(AttributeSet attr) {
        return StyleConstants.getForeground(attr);
    }

    public Color getBackground(AttributeSet attr) {
        return StyleConstants.getBackground(attr);
    }

    /** Una fuente compartida: dos pedidos iguales devuelven el mismo objeto. */
    public Font getFont(String family, int style, int size) {
        fontSearch.setValue(family, style, size);
        Font f = fontTable.get(fontSearch);
        if (f == null) {
            f = new Font(family, style, size);
            FontKey key = new FontKey(family, style, size);
            fontTable.put(key, f);
        }
        return f;
    }

    /** Las metricas de esa fuente, del toolkit. */
    public FontMetrics getFontMetrics(Font f) {
        return Toolkit.getDefaultToolkit().getFontMetrics(f);
    }

    // -- AttributeContext: la parte que comparte conjuntos ---------------------------------------

    public synchronized AttributeSet addAttribute(AttributeSet old, Object name, Object value) {
        if ((old.getAttributeCount() + 1) <= getCompressionThreshold()) {
            search.removeAttributes(search);
            search.addAttributes(old);
            search.addAttribute(name, value);
            reclaim(old);
            return getImmutableUniqueSet();
        }
        MutableAttributeSet ma = getMutableAttributeSet(old);
        ma.addAttribute(name, value);
        return ma;
    }

    public synchronized AttributeSet addAttributes(AttributeSet old, AttributeSet attr) {
        if ((old.getAttributeCount() + attr.getAttributeCount()) <= getCompressionThreshold()) {
            search.removeAttributes(search);
            search.addAttributes(old);
            search.addAttributes(attr);
            reclaim(old);
            return getImmutableUniqueSet();
        }
        MutableAttributeSet ma = getMutableAttributeSet(old);
        ma.addAttributes(attr);
        return ma;
    }

    public synchronized AttributeSet removeAttribute(AttributeSet old, Object name) {
        if ((old.getAttributeCount() - 1) <= getCompressionThreshold()) {
            search.removeAttributes(search);
            search.addAttributes(old);
            search.removeAttribute(name);
            reclaim(old);
            return getImmutableUniqueSet();
        }
        MutableAttributeSet ma = getMutableAttributeSet(old);
        ma.removeAttribute(name);
        return ma;
    }

    public synchronized AttributeSet removeAttributes(AttributeSet old, Enumeration<?> names) {
        if (old.getAttributeCount() <= getCompressionThreshold()) {
            search.removeAttributes(search);
            search.addAttributes(old);
            search.removeAttributes(names);
            reclaim(old);
            return getImmutableUniqueSet();
        }
        MutableAttributeSet ma = getMutableAttributeSet(old);
        ma.removeAttributes(names);
        return ma;
    }

    public synchronized AttributeSet removeAttributes(AttributeSet old, AttributeSet attrs) {
        if (old.getAttributeCount() <= getCompressionThreshold()) {
            search.removeAttributes(search);
            search.addAttributes(old);
            search.removeAttributes(attrs);
            reclaim(old);
            return getImmutableUniqueSet();
        }
        MutableAttributeSet ma = getMutableAttributeSet(old);
        ma.removeAttributes(attrs);
        return ma;
    }

    /** El conjunto vacio compartido. */
    public AttributeSet getEmptySet() {
        return SimpleAttributeSet.EMPTY;
    }

    /**
     * Avisa que ese conjunto ya no se usa.
     *
     * <p>No hace nada: los conjuntos chicos viven en la bolsa mientras el contexto exista, y
     * llevar una cuenta de referencias costaria mas de lo que ahorraria. El JDK hace lo mismo
     * cuando no hay recoleccion de por medio.
     */
    public void reclaim(AttributeSet a) {
    }

    /** Ver la nota de la clase. */
    protected int getCompressionThreshold() {
        return THRESHOLD;
    }

    protected SmallAttributeSet createSmallAttributeSet(AttributeSet a) {
        return new SmallAttributeSet(a);
    }

    protected MutableAttributeSet createLargeAttributeSet(AttributeSet a) {
        return new SimpleAttributeSet(a);
    }

    /** Nada que sacar: ver {@link #reclaim}. */
    synchronized void removeUnusedSets() {
    }

    /** El conjunto compartido igual al que se esta armando; lo crea si no existia. */
    AttributeSet getImmutableUniqueSet() {
        SmallAttributeSet key = createSmallAttributeSet(search);
        SmallAttributeSet a = attributesPool.get(key);
        if (a == null) {
            a = key;
            attributesPool.put(key, a);
        }
        return a;
    }

    MutableAttributeSet getMutableAttributeSet(AttributeSet a) {
        if (a instanceof MutableAttributeSet && a != SimpleAttributeSet.EMPTY) {
            return (MutableAttributeSet) a;
        }
        return createLargeAttributeSet(a);
    }

    public String toString() {
        removeUnusedSets();
        String s = "";
        Enumeration<SmallAttributeSet> keys = attributesPool.keys();
        while (keys.hasMoreElements()) {
            SmallAttributeSet set = keys.nextElement();
            s = s + set + "\n";
        }
        return s;
    }

    /** No esta; ver la nota de la clase. */
    public void writeAttributes(ObjectOutputStream out, AttributeSet a) throws IOException {
        throw new IOException("esta VM no serializa atributos");
    }

    /** No esta; ver la nota de la clase. */
    public void readAttributes(ObjectInputStream in, MutableAttributeSet a)
            throws ClassNotFoundException, IOException {
        throw new IOException("esta VM no serializa atributos");
    }

    /** No esta; ver la nota de la clase. */
    public static void writeAttributeSet(ObjectOutputStream out, AttributeSet a)
            throws IOException {
        throw new IOException("esta VM no serializa atributos");
    }

    /** No esta; ver la nota de la clase. */
    public static void readAttributeSet(ObjectInputStream in, MutableAttributeSet a)
            throws ClassNotFoundException, IOException {
        throw new IOException("esta VM no serializa atributos");
    }

    /**
     * Registra una clave estatica para poder nombrarla al serializar.
     *
     * <p>El nombre es el de la clase mas el de la clave: dos claves de clases distintas que se
     * llamen igual no chocan.
     */
    public static void registerStaticAttributeKey(Object key) {
        String ioFmt = key.getClass().getName() + "." + key.toString();
        if (freezeKeyMap == null) {
            freezeKeyMap = new Hashtable<Object, String>();
            thawKeyMap = new Hashtable<String, Object>();
        }
        freezeKeyMap.put(key, ioFmt);
        thawKeyMap.put(ioFmt, key);
    }

    /** La clave estatica que corresponde a ese nombre, o el mismo objeto si no esta registrada. */
    public static Object getStaticAttribute(Object key) {
        if (thawKeyMap == null || key == null) {
            return null;
        }
        return thawKeyMap.get(key);
    }

    public static Object getStaticAttributeKey(Object key) {
        return key.getClass().getName() + "." + key.toString();
    }

    /** La clave de la tabla de fuentes: familia, estilo y cuerpo. */
    static class FontKey {

        private String family;
        private int style;
        private int size;

        public FontKey(String family, int style, int size) {
            setValue(family, style, size);
        }

        public void setValue(String family, int style, int size) {
            this.family = (family != null) ? family.intern() : null;
            this.style = style;
            this.size = size;
        }

        public int hashCode() {
            int fhash = (family != null) ? family.hashCode() : 0;
            return fhash ^ style ^ size;
        }

        public boolean equals(Object obj) {
            if (obj instanceof FontKey) {
                FontKey font = (FontKey) obj;
                return (size == font.size) && (style == font.style) && (family == font.family);
            }
            return false;
        }
    }

    /**
     * Un conjunto de atributos chico e inmutable: un arreglo de pares.
     *
     * <p>Sin tabla: para menos de diez atributos, recorrer un arreglo es mas rapido que calcular
     * un hash, y ocupa la mitad. El padre de resolucion se guarda aparte para no buscarlo en cada
     * consulta fallida.
     */
    public class SmallAttributeSet implements AttributeSet {

        Object[] attributes;
        AttributeSet resolveParent;

        public SmallAttributeSet(Object[] attributes) {
            this.attributes = attributes;
            updateResolveParent();
        }

        public SmallAttributeSet(AttributeSet attrs) {
            int n = attrs.getAttributeCount();
            Object[] tbl = new Object[2 * n];
            Enumeration<?> names = attrs.getAttributeNames();
            int i = 0;
            while (names.hasMoreElements()) {
                tbl[i] = names.nextElement();
                tbl[i + 1] = attrs.getAttribute(tbl[i]);
                i = i + 2;
            }
            attributes = tbl;
            updateResolveParent();
        }

        private void updateResolveParent() {
            resolveParent = null;
            Object[] tbl = attributes;
            for (int i = 0; i < tbl.length; i = i + 2) {
                if (tbl[i] == StyleConstants.ResolveAttribute) {
                    resolveParent = (AttributeSet) tbl[i + 1];
                    break;
                }
            }
        }

        /** El valor propio, sin preguntarle al padre. */
        Object getLocalAttribute(Object nm) {
            Object[] tbl = attributes;
            for (int i = 0; i < tbl.length; i = i + 2) {
                if (nm.equals(tbl[i])) {
                    return tbl[i + 1];
                }
            }
            return null;
        }

        public String toString() {
            String s = "{";
            Object[] tbl = attributes;
            for (int i = 0; i < tbl.length; i = i + 2) {
                if (tbl[i + 1] instanceof AttributeSet) {
                    s = s + tbl[i] + "=" + "AttributeSet" + ",";
                } else {
                    s = s + tbl[i] + "=" + tbl[i + 1] + ",";
                }
            }
            s = s + "}";
            return s;
        }

        public int hashCode() {
            int code = 0;
            Object[] tbl = attributes;
            for (int i = 1; i < tbl.length; i = i + 2) {
                code = code ^ tbl[i].hashCode();
            }
            return code;
        }

        public boolean equals(Object obj) {
            if (obj instanceof AttributeSet) {
                AttributeSet attrs = (AttributeSet) obj;
                return ((getAttributeCount() == attrs.getAttributeCount())
                        && containsAttributes(attrs));
            }
            return false;
        }

        /** Es inmutable: la copia es el mismo objeto. */
        public Object clone() {
            return this;
        }

        public int getAttributeCount() {
            return attributes.length / 2;
        }

        public boolean isDefined(Object key) {
            Object[] a = attributes;
            int n = a.length;
            for (int i = 0; i < n; i = i + 2) {
                if (key.equals(a[i])) {
                    return true;
                }
            }
            return false;
        }

        public boolean isEqual(AttributeSet attr) {
            if (attr instanceof SmallAttributeSet) {
                return attr == this;
            }
            return ((getAttributeCount() == attr.getAttributeCount())
                    && containsAttributes(attr));
        }

        public AttributeSet copyAttributes() {
            return this;
        }

        public Object getAttribute(Object key) {
            Object value = getLocalAttribute(key);
            if (value == null) {
                AttributeSet parent = getResolveParent();
                if (parent != null) {
                    value = parent.getAttribute(key);
                }
            }
            return value;
        }

        public Enumeration<?> getAttributeNames() {
            return new KeyEnumeration(attributes);
        }

        public boolean containsAttribute(Object name, Object value) {
            return value.equals(getAttribute(name));
        }

        public boolean containsAttributes(AttributeSet attrs) {
            boolean result = true;
            Enumeration<?> names = attrs.getAttributeNames();
            while (result && names.hasMoreElements()) {
                Object name = names.nextElement();
                result = attrs.getAttribute(name).equals(getAttribute(name));
            }
            return result;
        }

        public AttributeSet getResolveParent() {
            return resolveParent;
        }
    }

    /**
     * Recorre los nombres de un arreglo de pares.
     *
     * <p>Estatica y no interna: no necesita el contexto, y nuestro javac todavia no pasa la
     * instancia externa implicita cuando una clase interna crea a una hermana.
     */
    static class KeyEnumeration implements Enumeration<Object> {

        KeyEnumeration(Object[] attr) {
            this.attr = attr;
            i = 0;
        }

        public boolean hasMoreElements() {
            return i < attr.length;
        }

        public Object nextElement() {
            if (i < attr.length) {
                Object o = attr[i];
                i = i + 2;
                return o;
            }
            throw new java.util.NoSuchElementException();
        }

        Object[] attr;
        int i;
    }

    /**
     * Un estilo con nombre: un conjunto mutable que avisa cuando cambia.
     *
     * <p>Guarda adentro un conjunto <em>inmutable</em> del contexto y lo reemplaza en cada cambio.
     * Asi, quien se cuelgue de este estilo como padre de resolucion sigue viendo lo ultimo sin que
     * nadie le avise, y los que si quieren enterarse escuchan.
     */
    public class NamedStyle implements Style, Serializable {

        protected EventListenerList listenerList;
        protected transient ChangeEvent changeEvent;

        private transient AttributeSet attributes;

        public NamedStyle(String name, Style parent) {
            iniciar(name, parent);
        }

        public NamedStyle(Style parent) {
            iniciar(null, parent);
        }

        public NamedStyle() {
            iniciar(null, null);
        }

        /**
         * El cuerpo comun de los tres constructores.
         *
         * <p>Un metodo y no un `this(...)` encadenado: en una clase interna, nuestro javac cuenta
         * la instancia externa como argumento y el encadenado termina llamandose a si mismo
         * (#511).
         */
        private void iniciar(String name, Style parent) {
            attributes = getEmptySet();
            listenerList = new EventListenerList();
            if (name != null) {
                setName(name);
            }
            if (parent != null) {
                setResolveParent(parent);
            }
        }

        public String toString() {
            return "NamedStyle:" + getName() + " " + attributes;
        }

        public String getName() {
            if (isDefined(StyleConstants.NameAttribute)) {
                return getAttribute(StyleConstants.NameAttribute).toString();
            }
            return null;
        }

        public void setName(String name) {
            if (name != null) {
                this.addAttribute(StyleConstants.NameAttribute, name);
            }
        }

        public void addChangeListener(ChangeListener l) {
            listenerList.add(ChangeListener.class, l);
        }

        public void removeChangeListener(ChangeListener l) {
            listenerList.remove(ChangeListener.class, l);
        }

        public ChangeListener[] getChangeListeners() {
            return listenerList.getListeners(ChangeListener.class);
        }

        protected void fireStateChanged() {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i = i - 2) {
                if (listeners[i] == ChangeListener.class) {
                    if (changeEvent == null) {
                        changeEvent = new ChangeEvent(this);
                    }
                    ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
                }
            }
        }

        public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
            return listenerList.getListeners(listenerType);
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
            NamedStyle a = new NamedStyle();
            a.attributes = attributes.copyAttributes();
            return a;
        }

        public Object getAttribute(Object attrName) {
            return attributes.getAttribute(attrName);
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

        public void addAttribute(Object name, Object value) {
            StyleContext context = StyleContext.this;
            attributes = context.addAttribute(attributes, name, value);
            fireStateChanged();
        }

        public void addAttributes(AttributeSet attr) {
            StyleContext context = StyleContext.this;
            attributes = context.addAttributes(attributes, attr);
            fireStateChanged();
        }

        public void removeAttribute(Object name) {
            StyleContext context = StyleContext.this;
            attributes = context.removeAttribute(attributes, name);
            fireStateChanged();
        }

        public void removeAttributes(Enumeration<?> names) {
            StyleContext context = StyleContext.this;
            attributes = context.removeAttributes(attributes, names);
            fireStateChanged();
        }

        public void removeAttributes(AttributeSet attrs) {
            StyleContext context = StyleContext.this;
            if (attrs == this) {
                attributes = context.getEmptySet();
            } else {
                attributes = context.removeAttributes(attributes, attrs);
            }
            fireStateChanged();
        }

        public AttributeSet getResolveParent() {
            return attributes.getResolveParent();
        }

        public void setResolveParent(AttributeSet parent) {
            if (parent != null) {
                addAttribute(StyleConstants.ResolveAttribute, parent);
            } else {
                removeAttribute(StyleConstants.ResolveAttribute);
            }
        }
    }
}
