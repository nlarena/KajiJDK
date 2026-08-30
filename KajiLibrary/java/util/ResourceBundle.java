package java.util;

// KajiLibrary's java.util.ResourceBundle -- the abstract base of localized resource containers. A
// concrete bundle answers keys through handleGetObject / getKeys; this class adds the key lookup
// with parent fallback (getObject / getString / getStringArray), the key-set views, and the static
// getBundle factories.
//
// A KajiLibrary honesty note: KajiJDK ships no bundle files and models no module system, so the
// getBundle factories cannot find anything and raise MissingResourceException. The getBundle
// overloads taking a ResourceBundle.Control or a java.lang.Module are OMITTED (neither type is
// modelled here), which is invisible to bundles a program constructs itself and passes around --
// e.g. to System.getLogger(String, ResourceBundle) or Logger.log(..., ResourceBundle, ...).
public abstract class ResourceBundle {

    /** The parent bundle, consulted when this one lacks a key. */
    protected ResourceBundle parent;

    // The bundle's locale (its own; ROOT for a bundle built by the no-arg constructor).
    private Locale locale = Locale.ROOT;

    // The base name this bundle was loaded under, or null when it was constructed directly.
    private String name;

    public ResourceBundle() {
    }

    /** The base name of this bundle, or null if unknown. */
    public String getBaseBundleName() {
        return this.name;
    }

    /** The string for {@code key}. */
    public final String getString(String key) {
        return (String) this.getObject(key);
    }

    /** The string array for {@code key}. */
    public final String[] getStringArray(String key) {
        return (String[]) this.getObject(key);
    }

    /** The object for {@code key}, searching this bundle then its parents. */
    public final Object getObject(String key) {
        Object obj = this.handleGetObject(key);
        if (obj == null) {
            if (this.parent != null) {
                obj = this.parent.getObject(key);
            }
            if (obj == null) {
                throw new MissingResourceException(
                        "Can't find resource for bundle "
                                + this.getClass().getName() + ", key " + key,
                        this.getClass().getName(), key);
            }
        }
        return obj;
    }

    /** This bundle's locale. */
    public Locale getLocale() {
        return this.locale;
    }

    /** Sets the parent bundle used for key fallback. */
    protected void setParent(ResourceBundle parent) {
        this.parent = parent;
    }

    /** Whether {@code key} is in this bundle or any of its parents. */
    public boolean containsKey(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        ResourceBundle rb = this;
        while (rb != null) {
            if (rb.handleKeySet().contains(key)) {
                return true;
            }
            rb = rb.parent;
        }
        return false;
    }

    /** Every key in this bundle and its parents. */
    public Set<String> keySet() {
        Set<String> keys = new HashSet<String>();
        ResourceBundle rb = this;
        while (rb != null) {
            for (String k : rb.handleKeySet()) {
                keys.add(k);
            }
            rb = rb.parent;
        }
        return keys;
    }

    /** The keys this bundle answers directly (not its parents'). Built from {@link #getKeys()}. */
    protected Set<String> handleKeySet() {
        Set<String> keys = new HashSet<String>();
        Enumeration<String> e = this.getKeys();
        while (e.hasMoreElements()) {
            String k = e.nextElement();
            if (this.handleGetObject(k) != null) {
                keys.add(k);
            }
        }
        return keys;
    }

    /** The object for {@code key} in THIS bundle, or null. Defined by concrete subclasses. */
    protected abstract Object handleGetObject(String key);

    /** An enumeration of this bundle's keys (including its parents'). Defined by subclasses. */
    public abstract Enumeration<String> getKeys();

    // ---- factories ----
    //
    // KajiJDK has no bundle files to load, so every factory misses. The signatures taking a
    // ResourceBundle.Control or a java.lang.Module are omitted (those types are not modelled).

    public static final ResourceBundle getBundle(String baseName) {
        return getBundle(baseName, Locale.getDefault());
    }

    public static final ResourceBundle getBundle(String baseName, Locale locale) {
        throw notFound(baseName);
    }

    public static ResourceBundle getBundle(String baseName, Locale locale, ClassLoader loader) {
        throw notFound(baseName);
    }

    private static MissingResourceException notFound(String baseName) {
        return new MissingResourceException(
                "Can't find bundle for base name " + baseName + " (KajiJDK ships no bundle files)",
                baseName, "");
    }

    /** Clears the (empty) bundle cache. A no-op: KajiJDK caches no bundles. */
    public static final void clearCache() {
    }

    /** Clears the (empty) bundle cache for a loader. A no-op: KajiJDK caches no bundles. */
    public static final void clearCache(ClassLoader loader) {
    }
}
