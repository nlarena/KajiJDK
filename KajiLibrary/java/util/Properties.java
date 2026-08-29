package java.util;

// KajiLibrary's java.util.Properties (finding #267).
//
// It exists because `jakarta.persistence.spi.PersistenceUnitInfo` returns one from
// `getProperties()`, and without the class the file does not compile.
//
// The JDK's shape is kept where it is load-bearing: it EXTENDS Hashtable<Object,Object> -- which
// is why `put` can take any object and `getProperty` returns null for a non-String value rather
// than throwing -- and it chains to a `defaults` table.
//
// What it deliberately does NOT have: `load(InputStream)`, `load(Reader)`, `store`, `storeToXML`,
// `loadFromXML` and `list(PrintStream)`. All six are about a stream format, and the two that read
// have a syntax of their own (escapes, continuation lines, `=` vs `:` vs whitespace). Writing them
// against an IO layer we do not model would be inventing a parser nobody can test here. The
// in-memory half is the half a persistence unit actually uses.
//
// A missing member is a legal subset; a member that lies is not.
public class Properties extends Hashtable<Object, Object> {

    /** The table consulted when a key is not in this one. Null if there is none. */
    protected Properties defaults;

    public Properties() {
        this.defaults = null;
    }

    public Properties(Properties defaults) {
        this.defaults = defaults;
    }

    /**
     * The value of {@code key}, or the one the defaults chain gives, or null.
     *
     * <p>Returns null -- not the stored object -- when the value is present but is not a String.
     * That is the JDK's behaviour and the reason this class can extend a table of Objects without
     * its String-typed accessors ever lying about what they return.
     */
    public String getProperty(String key) {
        Object value = this.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        if (this.defaults != null) {
            return this.defaults.getProperty(key);
        }
        return null;
    }

    public String getProperty(String key, String defaultValue) {
        String value = this.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Stores a String value. Returns whatever was there before, which need not be a String --
     * again the JDK's signature, and the honest one for a table of Objects.
     */
    public Object setProperty(String key, String value) {
        return this.put(key, value);
    }

    /** The keys of this table and of its defaults chain. */
    public Enumeration<Object> propertyNames() {
        return this.collectNames().keys();
    }

    /** The keys whose key AND value are both Strings, defaults included. */
    public Set<String> stringPropertyNames() {
        Hashtable<Object, Object> all = this.collectNames();
        Set<String> names = new HashSet<String>();
        Enumeration<Object> keys = all.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof String && all.get(key) instanceof String) {
                names.add((String) key);
            }
        }
        return names;
    }

    // The defaults FIRST, so this table's own entries overwrite them -- which is the whole point
    // of a defaults chain.
    private Hashtable<Object, Object> collectNames() {
        Hashtable<Object, Object> all = new Hashtable<Object, Object>();
        if (this.defaults != null) {
            Hashtable<Object, Object> inherited = this.defaults.collectNames();
            Enumeration<Object> keys = inherited.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                all.put(key, inherited.get(key));
            }
        }
        Enumeration<Object> mine = this.keys();
        while (mine.hasMoreElements()) {
            Object key = mine.nextElement();
            all.put(key, this.get(key));
        }
        return all;
    }
}
