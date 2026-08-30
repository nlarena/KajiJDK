package java.util;

// A bundle of locale-specific resources, looked up by key. Abstract because the storage is the
// part it does not fix: ListResourceBundle keeps an array, another subclass could keep anything.
//
// The whole design turns on the parent chain. A bundle for "Messages_es_AR" has "Messages_es" as
// its parent and "Messages" beyond that, so a key defined only in the base is still visible
// through the most specific bundle. getObject walks that chain; handleGetObject does not. That
// split is why handleGetObject is the abstract one — a subclass answers only for itself.
//
// A KajiLibrary subset, stated up front:
//
//   - The two getBundle overloads that take a java.lang.Module are absent, because
//     java.lang.Module does not exist here. Every other getBundle overload is present.
//   - Only the "java.class" format loads. "java.properties" is accepted by Control and reported
//     in getFormats, but newBundle returns null for it, because PropertyResourceBundle and the
//     .properties reader are not implemented yet. A bundle written as a class works; a bundle
//     written as a .properties file is simply not found.
//   - The cache is not per-ClassLoader, so clearCache(ClassLoader) clears everything.
public abstract class ResourceBundle {

    // The next bundle to consult for a key this one does not define. Protected because
    // setParent is protected: building the chain is the framework's job, not a caller's.
    protected ResourceBundle parent;

    // The locale this bundle was loaded for. Null until the loader sets it.
    private Locale locale;

    // The base name this bundle was loaded under, e.g. "Messages".
    private String name;

    // This bundle's own keys, computed once from getKeys(). Null until first asked.
    private Set<String> ownKeys;

    // The process-wide bundle cache, keyed by base name and locale.
    private static final HashMap<String, ResourceBundle> cache = new HashMap<String, ResourceBundle>();

    // A bundle with no parent, no locale and no name; the loader fills those in.
    public ResourceBundle() {
    }

    // The base name this bundle was loaded under, or null if it was not loaded by getBundle.
    public String getBaseBundleName() {
        return this.name;
    }

    // The string for `key`, searching this bundle and then its parents.
    public final String getString(String key) {
        return (String) this.getObject(key);
    }

    // The string array for `key`, searching this bundle and then its parents.
    public final String[] getStringArray(String key) {
        return (String[]) this.getObject(key);
    }

    // The object for `key`, searching this bundle and then its parents.
    //
    // Throws rather than returning null when nothing has it: a missing key is a packaging bug,
    // and returning null would push the diagnosis to wherever the value is finally used.
    public final Object getObject(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        Object obj = this.handleGetObject(key);
        if (obj == null && this.parent != null) {
            obj = this.parent.getObject(key);
        }
        if (obj == null) {
            String cls = this.getClass().getName();
            throw new MissingResourceException("Can't find resource for bundle " + cls
                + ", key " + key, cls, key);
        }
        return obj;
    }

    // The locale this bundle was loaded for.
    public Locale getLocale() {
        return this.locale;
    }

    // Sets the parent to consult for keys this bundle does not define.
    protected void setParent(ResourceBundle parent) {
        this.parent = parent;
    }

    // The bundle for `baseName` in the default locale.
    public static final ResourceBundle getBundle(String baseName) {
        return doGetBundle(baseName, Locale.getDefault(), loader(), Control.getControl(Control.FORMAT_DEFAULT));
    }

    // The bundle for `baseName` in the default locale, loaded under `control`.
    public static final ResourceBundle getBundle(String baseName, Control control) {
        return doGetBundle(baseName, Locale.getDefault(), loader(), control);
    }

    // The bundle for `baseName` in `locale`.
    public static final ResourceBundle getBundle(String baseName, Locale locale) {
        return doGetBundle(baseName, locale, loader(), Control.getControl(Control.FORMAT_DEFAULT));
    }

    // The bundle for `baseName` in `locale`, loaded under `control`.
    public static final ResourceBundle getBundle(String baseName, Locale locale, Control control) {
        return doGetBundle(baseName, locale, loader(), control);
    }

    // The bundle for `baseName` in `locale`, loaded through `loader`.
    public static ResourceBundle getBundle(String baseName, Locale locale, ClassLoader loader) {
        return doGetBundle(baseName, locale, loader, Control.getControl(Control.FORMAT_DEFAULT));
    }

    // The bundle for `baseName` in `locale`, loaded through `loader` under `control`.
    public static ResourceBundle getBundle(String baseName, Locale locale, ClassLoader loader,
                                           Control control) {
        return doGetBundle(baseName, locale, loader, control);
    }

    // Empties the bundle cache.
    public static final void clearCache() {
        synchronized (cache) {
            cache.clear();
        }
    }

    // Empties the bundle cache. A KajiLibrary subset: the cache is not partitioned by loader, so
    // this clears everything, exactly as clearCache() does.
    public static final void clearCache(ClassLoader loader) {
        clearCache();
    }

    // The object this bundle itself holds for `key`, or null if it holds none. Must not consult
    // the parent — getObject does that.
    protected abstract Object handleGetObject(String key);

    // Every key visible through this bundle, its parents included.
    public abstract Enumeration<String> getKeys();

    // Whether `key` resolves through this bundle or any of its parents.
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

    // Every key visible through this bundle, its parents included.
    public Set<String> keySet() {
        Set<String> keys = new HashSet<String>();
        ResourceBundle rb = this;
        while (rb != null) {
            // Collection.addAll is not implemented yet, so the union is walked by hand.
            Iterator<String> it = rb.handleKeySet().iterator();
            while (it.hasNext()) {
                keys.add(it.next());
            }
            rb = rb.parent;
        }
        return keys;
    }

    // The keys this bundle defines itself, parents excluded.
    //
    // Derived from getKeys() by keeping the keys handleGetObject actually answers for, which is
    // the definition of "its own". Computed once and kept.
    protected Set<String> handleKeySet() {
        if (this.ownKeys == null) {
            Set<String> keys = new HashSet<String>();
            Enumeration<String> e = this.getKeys();
            while (e.hasMoreElements()) {
                String key = e.nextElement();
                if (this.handleGetObject(key) != null) {
                    keys.add(key);
                }
            }
            this.ownKeys = keys;
        }
        return this.ownKeys;
    }

    // The loader bundles are looked up through when the caller does not name one.
    private static ClassLoader loader() {
        return ClassLoader.getSystemClassLoader();
    }

    // The cache key for a base name and locale.
    private static String cacheKey(String baseName, Locale locale) {
        return baseName + "#" + locale.getLanguage() + "_" + locale.getCountry();
    }

    // Finds the bundle, trying the requested locale, then the fallback locale, then the root.
    private static ResourceBundle doGetBundle(String baseName, Locale locale, ClassLoader loader,
                                              Control control) {
        if (baseName == null || locale == null || loader == null || control == null) {
            throw new NullPointerException();
        }
        String key = cacheKey(baseName, locale);
        synchronized (cache) {
            ResourceBundle hit = cache.get(key);
            if (hit != null) {
                return hit;
            }
        }
        ResourceBundle found = loadChain(baseName, locale, loader, control);
        if (found == null) {
            Locale fallback = control.getFallbackLocale(baseName, locale);
            if (fallback != null) {
                found = loadChain(baseName, fallback, loader, control);
            }
        }
        if (found == null) {
            found = loadChain(baseName, Locale.ROOT, loader, control);
        }
        if (found == null) {
            throw new MissingResourceException("Can't find bundle for base name " + baseName
                + ", locale " + locale, baseName + "_" + locale, "");
        }
        synchronized (cache) {
            cache.put(key, found);
        }
        return found;
    }

    // Loads the bundle for `locale` and chains it to the bundles of its more general candidates,
    // returning the most specific one found, or null if none was.
    private static ResourceBundle loadChain(String baseName, Locale locale, ClassLoader loader,
                                            Control control) {
        List<Locale> candidates = control.getCandidateLocales(baseName, locale);
        List<String> formats = control.getFormats(baseName);
        ResourceBundle first = null;
        ResourceBundle previous = null;
        int i = 0;
        while (i < candidates.size()) {
            Locale candidate = candidates.get(i);
            ResourceBundle bundle = null;
            int f = 0;
            while (f < formats.size() && bundle == null) {
                bundle = tryNewBundle(baseName, candidate, formats.get(f), loader, control);
                f = f + 1;
            }
            if (bundle != null) {
                bundle.name = baseName;
                bundle.locale = candidate;
                if (first == null) {
                    first = bundle;
                } else {
                    previous.setParent(bundle);
                }
                previous = bundle;
            }
            i = i + 1;
        }
        return first;
    }

    // Calls control.newBundle, turning the two checked failures of instantiation into a
    // MissingResourceException.
    //
    // They are not the same as "there is no such bundle": a class that exists but cannot be
    // instantiated — no accessible no-arg constructor, or abstract — is a packaging error, and
    // reporting it as "not found" would send the reader looking for a file that is right there.
    // So it propagates, with the offending class named.
    private static ResourceBundle tryNewBundle(String baseName, Locale locale, String format,
                                               ClassLoader loader, Control control) {
        try {
            return control.newBundle(baseName, locale, format, loader, false);
        } catch (IllegalAccessException e) {
            throw bundleFailed(baseName, locale, control, e);
        } catch (InstantiationException e) {
            throw bundleFailed(baseName, locale, control, e);
        }
    }

    // The exception for a bundle class that exists but could not be instantiated.
    private static MissingResourceException bundleFailed(String baseName, Locale locale,
                                                         Control control, Exception cause) {
        String bundleName = control.toBundleName(baseName, locale);
        return new MissingResourceException("Could not instantiate bundle " + bundleName
            + ": " + cause, bundleName, "");
    }

    // The policy object behind getBundle: which formats to try, which locales to fall back
    // through, how to turn a base name and locale into a bundle name, and how to instantiate it.
    //
    // It is a class rather than an interface so that a subclass can override one decision and
    // inherit the rest — overriding only getFormats to force class bundles, say.
    public static class Control {

        // Try a class bundle, then a properties bundle.
        public static final List<String> FORMAT_DEFAULT = formats("java.class", "java.properties");

        // Try only a class bundle.
        public static final List<String> FORMAT_CLASS = formats("java.class", null);

        // Try only a properties bundle.
        public static final List<String> FORMAT_PROPERTIES = formats("java.properties", null);

        // getTimeToLive: do not cache the bundle at all.
        public static final long TTL_DONT_CACHE = -1;

        // getTimeToLive: cache the bundle with no expiry.
        public static final long TTL_NO_EXPIRATION_CONTROL = -2;

        // The formats this control offers.
        private final List<String> formats;

        // Whether getFallbackLocale falls back to the default locale, or refuses to.
        private final boolean fallback;

        // A control offering the default formats, with fallback enabled.
        protected Control() {
            this.formats = FORMAT_DEFAULT;
            this.fallback = true;
        }

        // The internal constructor the two factories use.
        private Control(List<String> formats, boolean fallback) {
            this.formats = formats;
            this.fallback = fallback;
        }

        // Builds one of the constant format lists.
        private static List<String> formats(String a, String b) {
            Object[] items;
            if (b == null) {
                items = new Object[1];
                items[0] = a;
            } else {
                items = new Object[2];
                items[0] = a;
                items[1] = b;
            }
            return new FixedList<String>(items);
        }

        // A control offering exactly `formats`, with fallback to the default locale.
        public static final Control getControl(List<String> formats) {
            return new Control(checkFormats(formats), true);
        }

        // A control offering exactly `formats`, with no fallback to the default locale.
        public static final Control getNoFallbackControl(List<String> formats) {
            return new Control(checkFormats(formats), false);
        }

        // Rejects a format list that is not one of the three constants, which is the same
        // restriction the JDK's factories apply.
        private static List<String> checkFormats(List<String> formats) {
            if (formats == null) {
                throw new NullPointerException();
            }
            if (formats != FORMAT_DEFAULT && formats != FORMAT_CLASS && formats != FORMAT_PROPERTIES) {
                throw new IllegalArgumentException("Invalid format list");
            }
            return formats;
        }

        // The formats to try for `baseName`.
        public List<String> getFormats(String baseName) {
            if (baseName == null) {
                throw new NullPointerException();
            }
            return this.formats;
        }

        // The locales to try, most specific first, ending at the root.
        //
        // For Locale("es","AR") that is es_AR, then es, then ROOT — which is exactly what makes a
        // key defined once in the base bundle visible from the most specific one.
        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            if (baseName == null || locale == null) {
                throw new NullPointerException();
            }
            String language = locale.getLanguage();
            String country = locale.getCountry();
            ArrayList<Locale> list = new ArrayList<Locale>();
            if (!language.isEmpty() && !country.isEmpty()) {
                list.add(new Locale(language, country));
            }
            if (!language.isEmpty()) {
                list.add(new Locale(language));
            }
            list.add(Locale.ROOT);
            return list;
        }

        // The locale to try after `locale` produced nothing: the default locale, unless that is
        // the one that just failed, or unless this control refuses to fall back.
        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (baseName == null || locale == null) {
                throw new NullPointerException();
            }
            if (!this.fallback) {
                return null;
            }
            Locale def = Locale.getDefault();
            if (def.getLanguage().equals(locale.getLanguage())
                && def.getCountry().equals(locale.getCountry())) {
                return null;
            }
            return def;
        }

        // Instantiates the bundle for `baseName` and `locale` in `format`, or returns null if
        // there is none.
        //
        // A KajiLibrary subset: only "java.class" loads. For "java.properties" this returns null,
        // because PropertyResourceBundle does not exist yet — a .properties bundle is therefore
        // never found rather than found and mis-parsed.
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                        ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException {
            if (baseName == null || locale == null || format == null || loader == null) {
                throw new NullPointerException();
            }
            if (!format.equals("java.class")) {
                return null;
            }
            String bundleName = this.toBundleName(baseName, locale);
            try {
                Class<?> c = Class.forName(bundleName, true, loader);
                Object instance = c.newInstance();
                if (instance instanceof ResourceBundle) {
                    return (ResourceBundle) instance;
                }
                return null;
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        // How long a loaded bundle stays cached. Never expires here.
        public long getTimeToLive(String baseName, Locale locale) {
            if (baseName == null || locale == null) {
                throw new NullPointerException();
            }
            return TTL_NO_EXPIRATION_CONTROL;
        }

        // Whether an expired bundle has actually changed and must be reloaded.
        //
        // A KajiLibrary subset: with no resource timestamps to consult this is always false,
        // which is consistent with getTimeToLive never expiring anything in the first place.
        public boolean needsReload(String baseName, Locale locale, String format,
                                   ClassLoader loader, ResourceBundle bundle, long loadTime) {
            if (baseName == null || locale == null || format == null || loader == null
                || bundle == null) {
                throw new NullPointerException();
            }
            return false;
        }

        // The class name of the bundle for `baseName` and `locale`: "Messages_es_AR", or just
        // "Messages" for the root.
        public String toBundleName(String baseName, Locale locale) {
            if (baseName == null || locale == null) {
                throw new NullPointerException();
            }
            String language = locale.getLanguage();
            String country = locale.getCountry();
            if (language.isEmpty() && country.isEmpty()) {
                return baseName;
            }
            String result = baseName + "_" + language;
            if (!country.isEmpty()) {
                result = result + "_" + country;
            }
            return result;
        }

        // The resource path for a bundle name and suffix: "a.b.Messages" and "properties" give
        // "a/b/Messages.properties".
        public final String toResourceName(String bundleName, String suffix) {
            if (bundleName == null || suffix == null) {
                throw new NullPointerException();
            }
            return bundleName.replace('.', '/') + "." + suffix;
        }
    }
}
