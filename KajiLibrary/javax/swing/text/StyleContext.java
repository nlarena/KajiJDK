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
 * The bag where the styles and the shared attribute sets live.
 *
 * <h2>Why share</h2>
 *
 * <p>A styled document has an attribute set for each stretch of text, and those sets repeat a
 * great deal: everything in italics has the same one. The context <em>interns</em> them: on
 * asking for "this set plus bold" it returns an immutable and shared object, so a thousand
 * bold stretches are a single instance and comparing two stretches is comparing two references.
 *
 * <p>The small sets are kept as a flat array of name-value pairs
 * ({@link SmallAttributeSet}), which for few attributes is faster and smaller than a table.
 * Past the {@link #getCompressionThreshold} threshold a table is used and sharing stops: a large
 * set almost never repeats, and looking for a twin for it would cost more than it saves.
 *
 * <h2>The styles</h2>
 *
 * <p>A {@link Style} is a named set that reports when it changes, and the styles are chained
 * through their resolving parent. The context always has one, {@link #DEFAULT_STYLE}, which is
 * the last link of that chain.
 *
 * <h2>What is not there</h2>
 *
 * <p>{@link #writeAttributes} and {@link #readAttributes} and their static versions serialize
 * attributes, and they need the static keys to be registered in order to write them by name. The
 * registry is there ({@link #registerStaticAttributeKey}) and {@link StyleConstants}'s keys
 * register themselves; what is not there is the serialization, which in this VM cannot be tested
 * against anything.
 */
public class StyleContext implements Serializable, AbstractDocument.AttributeContext {

    /** The name of the style all the others hang from. */
    public static final String DEFAULT_STYLE = "default";

    /** From how many attributes on sharing stops; see the class note. */
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

    /** The context used by the documents that do not ask for one of their own. */
    public static final StyleContext getDefaultStyleContext() {
        if (defaultContext == null) {
            defaultContext = new StyleContext();
        }
        return defaultContext;
    }

    /** A context with its default style empty. */
    public StyleContext() {
        styles = new NamedStyle(null);
        addStyle(DEFAULT_STYLE, null);
    }

    /**
     * It adds a style with that name and that parent.
     *
     * <p>A {@code null} name creates an anonymous style: it serves just as well to hang from, but
     * it cannot be asked for by name afterwards.
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

    /** It listens to the changes of <em>any</em> style of the context. */
    public void addChangeListener(ChangeListener l) {
        ((NamedStyle) styles).addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l) {
        ((NamedStyle) styles).removeChangeListener(l);
    }

    public ChangeListener[] getChangeListeners() {
        return ((NamedStyle) styles).getChangeListeners();
    }

    /** The font those attributes describe. */
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

        // Superscript and subscript are drawn smaller; it is the only thing that changes the size.
        if (StyleConstants.isSuperscript(attr) || StyleConstants.isSubscript(attr)) {
            size = size - 2;
        }

        return getFont(family, style, size);
    }

    /** Those attributes' foreground colour; see {@link StyleConstants#getForeground}. */
    public Color getForeground(AttributeSet attr) {
        return StyleConstants.getForeground(attr);
    }

    public Color getBackground(AttributeSet attr) {
        return StyleConstants.getBackground(attr);
    }

    /** A shared font: two equal requests return the same object. */
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

    /** That font's metrics, from the toolkit. */
    public FontMetrics getFontMetrics(Font f) {
        return Toolkit.getDefaultToolkit().getFontMetrics(f);
    }

    // -- AttributeContext: the part that shares sets --------------------------------------------

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

    /** The shared empty set. */
    public AttributeSet getEmptySet() {
        return SimpleAttributeSet.EMPTY;
    }

    /**
     * It reports that that set is no longer used.
     *
     * <p>It does nothing: the small sets live in the bag as long as the context exists, and keeping
     * a reference count would cost more than it would save. The JDK does the same when there is no
     * collection involved.
     */
    public void reclaim(AttributeSet a) {
    }

    /** See the class note. */
    protected int getCompressionThreshold() {
        return THRESHOLD;
    }

    protected SmallAttributeSet createSmallAttributeSet(AttributeSet a) {
        return new SmallAttributeSet(a);
    }

    protected MutableAttributeSet createLargeAttributeSet(AttributeSet a) {
        return new SimpleAttributeSet(a);
    }

    /** Nothing to take out: see {@link #reclaim}. */
    synchronized void removeUnusedSets() {
    }

    /** The shared set equal to the one being built; it creates it if it did not exist. */
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

    /** It is not there; see the class note. */
    public void writeAttributes(ObjectOutputStream out, AttributeSet a) throws IOException {
        throw new IOException("this VM does not serialize attributes");
    }

    /** It is not there; see the class note. */
    public void readAttributes(ObjectInputStream in, MutableAttributeSet a)
            throws ClassNotFoundException, IOException {
        throw new IOException("this VM does not serialize attributes");
    }

    /** It is not there; see the class note. */
    public static void writeAttributeSet(ObjectOutputStream out, AttributeSet a)
            throws IOException {
        throw new IOException("this VM does not serialize attributes");
    }

    /** It is not there; see the class note. */
    public static void readAttributeSet(ObjectInputStream in, MutableAttributeSet a)
            throws ClassNotFoundException, IOException {
        throw new IOException("this VM does not serialize attributes");
    }

    /**
     * It registers a static key so that it can be named when serializing.
     *
     * <p>The name is the class's plus the key's: two keys of different classes that are called the
     * same do not clash.
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

    /** The static key that corresponds to that name, or the same object if it is not registered. */
    public static Object getStaticAttribute(Object key) {
        if (thawKeyMap == null || key == null) {
            return null;
        }
        return thawKeyMap.get(key);
    }

    public static Object getStaticAttributeKey(Object key) {
        return key.getClass().getName() + "." + key.toString();
    }

    /** The font table's key: family, style and size. */
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
     * A small and immutable attribute set: an array of pairs.
     *
     * <p>With no table: for fewer than ten attributes, walking an array is faster than computing a
     * hash, and takes up half the room. The resolving parent is kept apart so as not to look it up
     * on every failed query.
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

        /** Its own value, without asking the parent. */
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

        /** It is immutable: the copy is the same object. */
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
     * It walks the names of an array of pairs.
     *
     * <p>Static and not inner: it does not need the context, and our javac does not yet pass the
     * implicit outer instance when an inner class creates a sibling.
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
     * A named style: a mutable set that reports when it changes.
     *
     * <p>It keeps inside an <em>immutable</em> set from the context and replaces it on every
     * change. That way, whoever hangs from this style as their resolving parent goes on seeing the
     * latest without anybody telling them, and those that do want to hear listen.
     */
    public class NamedStyle implements Style, Serializable {

        protected EventListenerList listenerList;
        protected transient ChangeEvent changeEvent;

        private transient AttributeSet attributes;

        public NamedStyle(String name, Style parent) {
            begin(name, parent);
        }

        public NamedStyle(Style parent) {
            begin(null, parent);
        }

        public NamedStyle() {
            begin(null, null);
        }

        /**
         * The common body of the three constructors.
         *
         * <p>A method and not a chained `this(...)`: in an inner class, our javac counts the outer
         * instance as an argument and the chained call ends up calling itself (#511).
         */
        private void begin(String name, Style parent) {
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
