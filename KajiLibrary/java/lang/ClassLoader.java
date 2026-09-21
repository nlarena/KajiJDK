package java.lang;

// Through an import and a simple name: qualifying the type at the use site does not resolve from
// java.lang
// (finding #210).
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * KajiLibrary's java.lang.ClassLoader.
 *
 * <p><strong>KajiJDK has one loader.</strong> Everything is found on one classpath and defined by
 * one authority, so the delegation hierarchy that shapes the real class this models is here as a
 * shape and not as a mechanism: {@link #getParent()} answers null, and {@link #getSystemClassLoader()}
 * and {@link #getPlatformClassLoader()} answer the same object. That is not a stub -- it is what a
 * VM with one loader honestly reports, the same way {@code Class.getClassLoader()} answers null
 * for a class the bootstrap loader defined.
 *
 * <p>What IS a real mechanism is {@link #defineClass(String, byte[], int, int)}: bytes in, a
 * loaded type out, with no file anywhere. That is the only way a type enters the VM without
 * coming off the classpath, and it is what a bytecode generator needs.
 *
 * <p><strong>Resources, packages, module.</strong> The resource methods ({@code getResource},
 * {@code getResourceAsStream}, {@code getResources}, {@code resources} and their static twins)
 * degrade honestly: KajiJDK serves classes, not co-located resource files, so every lookup misses
 * -- a stream is null, an enumeration or stream of URLs is empty -- exactly as the real loader
 * answers for a resource that is not present. The package methods ({@code getDefinedPackage},
 * {@code getDefinedPackages}) find nothing because the single loader keeps no package registry;
 * {@link Class#getPackage()} still mints a {@link Package} on demand. {@link #getUnnamedModule()}
 * is real -- it returns the one {@link Module} every class this loader defines belongs to.
 */
public class ClassLoader {

    private final ClassLoader parent;

    private final String name;

    // The only loader there is. `getSystemClassLoader` and `getPlatformClassLoader` return this very
    // one, and that is exact: there are not two.
    private static final ClassLoader THE_LOADER = new ClassLoader(null, "app");

    // The assertion state, which is the only thing a KajiJDK loader really keeps. It lives here and
    // not in the VM because `assert` desugars to a read of `desiredAssertionStatus` in each class's
    // `<clinit>`: changing it after a class has been initialised does not affect that class, and that
    // is exactly what the specification says.
    private boolean defaultAssertionStatus;

    private String[] assertionClasses = new String[0];

    private boolean[] assertionValues = new boolean[0];

    /** A loader with no parent and no name. */
    protected ClassLoader() {
        this.parent = null;
        this.name = null;
    }

    /**
     * A loader that delegates to {@code parent}.
     *
     * @param parent the loader to ask first
     */
    protected ClassLoader(ClassLoader parent) {
        this.parent = parent;
        this.name = null;
    }

    /**
     * A named loader that delegates to {@code parent}.
     *
     * <p>The name is for diagnostics only -- two loaders with the same name are still two
     * loaders, and a type defined by one is not the type defined by the other.
     *
     * @param name the name, or null
     * @param parent the loader to ask first
     */
    protected ClassLoader(String name, ClassLoader parent) {
        this.parent = parent;
        this.name = name;
    }

    private ClassLoader(ClassLoader parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    /** The loader this one asks first, or {@code null} for the bootstrap loader. */
    public final ClassLoader getParent() {
        return this.parent;
    }

    /** This loader's name, or {@code null}. */
    public String getName() {
        return this.name;
    }

    /** The loader that found the application's own classes. */
    public static ClassLoader getSystemClassLoader() {
        return ClassLoader.THE_LOADER;
    }

    /**
     * The loader for the platform's classes.
     *
     * <p>The same object as {@link #getSystemClassLoader()}, because there is one loader. In the
     * JDK they differ so that application code cannot see JDK-internal types through the wrong
     * one; KajiJDK has no such separation to enforce.
     */
    public static ClassLoader getPlatformClassLoader() {
        return ClassLoader.THE_LOADER;
    }

    // ---- module ----

    // This loader's unnamed module, minted once. Every class this loader defines belongs to it,
    // which is what {@link Class#getModule()} reports for a class with no named module.
    private Module unnamedModule;

    /** The unnamed {@link Module} of this loader — the module every class it defines belongs to. */
    public final Module getUnnamedModule() {
        if (this.unnamedModule == null) {
            this.unnamedModule = new Module(this);
        }
        return this.unnamedModule;
    }

    // ---- packages ----
    //
    // The loader **does** keep a register of the packages it defined. It used to not keep one, and
    // that left the pair cut in half: both queries were there (`getDefinedPackage`,
    // `getDefinedPackages`) and the only thing that could populate them was not (`definePackage`), so
    // both answered "none" for ever.
    //
    // What does **not** change is where they come from: nobody reads manifests here, so the register
    // only holds whatever somebody defined by hand. `Class.getPackage()` still mints a `Package` on
    // the fly from the class's name, without going through here -- they are two different
    // questions.

    private final java.util.HashMap<String, Package> packages =
            new java.util.HashMap<String, Package>();

    /**
     * It defines a package in this loader, with the attributes a manifest would bring.
     *
     * <p>It is `protected` because it is an operation of the loader upon itself: whoever defines
     * classes is who knows which JAR they came from and, therefore, who can state their version and
     * their vendor.
     *
     * @param name the package's name
     * @param sealBase the URL the package is sealed against, or `null` not to seal it
     * @throws IllegalArgumentException if the package was already defined in this loader
     * @throws NullPointerException if `name` is `null`
     */
    protected Package definePackage(String name, String specTitle, String specVersion,
            String specVendor, String implTitle, String implVersion, String implVendor,
            URL sealBase) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        synchronized (this.packages) {
            if (this.packages.containsKey(name)) {
                throw new IllegalArgumentException("package " + name + " is already defined");
            }
            Package p = new Package(name, specTitle, specVersion, specVendor,
                    implTitle, implVersion, implVendor, sealBase);
            this.packages.put(name, p);
            return p;
        }
    }

    /**
     * The {@link Package} of the given name defined by this loader, or {@code null}.
     *
     * @param name the package name; the empty string for the default package
     * @throws NullPointerException if {@code name} is null
     */
    public final Package getDefinedPackage(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        synchronized (this.packages) {
            return this.packages.get(name);
        }
    }

    /** The packages defined by this loader. */
    public final Package[] getDefinedPackages() {
        synchronized (this.packages) {
            return this.packages.values().toArray(new Package[0]);
        }
    }

    /**
     * The package with that name, searching **the parents as well**.
     *
     * <p>That is the difference from `getDefinedPackage`, which looks only at this loader. Here there
     * is one loader and therefore no parent, so both answers coincide -- but the delegation is
     * written, because it is the part of the contract that matters the day there is a hierarchy.
     *
     * @deprecated in the JDK, because it does not tell apart packages of the same name in different
     *     loaders.
     *     Use {@link #getDefinedPackage(String)}.
     */
    protected Package getPackage(String name) {
        Package own = this.getDefinedPackage(name);
        if (own != null) {
            return own;
        }
        if (this.parent != null) {
            return this.parent.getPackage(name);
        }
        return null;
    }

    /**
     * Every package visible from this loader: its own and its parents'.
     *
     * <p>The parent's go **first** and its own after, which is the order they are found in when
     * delegating.
     */
    protected Package[] getPackages() {
        Package[] own = this.getDefinedPackages();
        if (this.parent == null) {
            return own;
        }
        Package[] fromParent = this.parent.getPackages();
        Package[] all = new Package[fromParent.length + own.length];
        System.arraycopy(fromParent, 0, all, 0, fromParent.length);
        System.arraycopy(own, 0, all, fromParent.length, own.length);
        return all;
    }

    // ---- resources ----
    //
    // KajiJDK serves classes off the class path, not co-located resource files, so every resource
    // lookup honestly misses: a stream is {@code null}, an enumeration or stream of URLs is empty.
    // This is the same answer the real loader gives for a resource that is simply not present.

    // The three `find*` below are **the extension points**, and the three public forms call them.
    // That wiring is the part that was missing and it was not cosmetic: `getResource` used to return
    // `null` outright, so a subclass overriding `findResource` --which is the only documented way of
    // serving resources of one's own-- **was ignored**. The public method did not follow the protocol
    // its javadoc promises.

    /**
     * The resource of this name, or {@code null}.
     *
     * <p>It delegates to the parent first and then to {@link #findResource(String)}, which is the
     * JDK's order: a parent's resource wins over one's own of the same name.
     */
    public URL getResource(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        URL url = null;
        if (this.parent != null) {
            url = this.parent.getResource(name);
        }
        if (url == null) {
            url = this.findResource(name);
        }
        return url;
    }

    /** A stream for the resource of this name, or {@code null} if there is none. */
    public InputStream getResourceAsStream(String name) {
        URL url = this.getResource(name);
        if (url == null) {
            return null;
        }
        try {
            return url.openStream();
        } catch (IOException e) {
            return null;
        }
    }

    /** Every resource of this name: the parent's, and then its own. */
    public Enumeration<URL> getResources(String name) throws IOException {
        if (name == null) {
            throw new NullPointerException("name");
        }
        java.util.ArrayList<URL> all = new java.util.ArrayList<URL>();
        if (this.parent != null) {
            Enumeration<URL> fromParent = this.parent.getResources(name);
            while (fromParent.hasMoreElements()) {
                all.add(fromParent.nextElement());
            }
        }
        Enumeration<URL> own = this.findResources(name);
        while (own.hasMoreElements()) {
            all.add(own.nextElement());
        }
        return java.util.Collections.enumeration(all);
    }

    /** Every resource of this name as a stream. */
    public Stream<URL> resources(String name) {
        try {
            Enumeration<URL> e = this.getResources(name);
            java.util.ArrayList<URL> all = new java.util.ArrayList<URL>();
            while (e.hasMoreElements()) {
                all.add(e.nextElement());
            }
            return all.stream();
        } catch (IOException ex) {
            return Stream.empty();
        }
    }

    // ---- the extension points ----------------------------------------------------------------------
    //
    // All three return "nothing": KajiJDK serves classes off the classpath, not resource files
    // alongside. It is the same answer the real loader gives for a resource that is not there, and it
    // is where a subclass wanting to serve resources of its own has to step in.

    /** The resource of that name **this** loader serves, or `null`. */
    protected URL findResource(String name) {
        return null;
    }

    /** The resource of that name in that module, or `null`. */
    protected URL findResource(String moduleName, String name) throws IOException {
        return null;
    }

    /** Every resource of that name **this** loader serves. */
    protected Enumeration<URL> findResources(String name) throws IOException {
        return new EmptyEnumeration();
    }

    /** A system resource of this name, or {@code null} — none are served. */
    public static URL getSystemResource(String name) {
        return THE_LOADER.getResource(name);
    }

    /** A stream for a system resource of this name, or {@code null} — none are served. */
    public static InputStream getSystemResourceAsStream(String name) {
        return THE_LOADER.getResourceAsStream(name);
    }

    /** Every system resource of this name — an empty enumeration, as none are served. */
    public static Enumeration<URL> getSystemResources(String name) throws IOException {
        return THE_LOADER.getResources(name);
    }

    // ---- finding a type ----

    /**
     * The type named {@code name}, loading it if necessary.
     *
     * @param name the binary name, with dots
     * @throws ClassNotFoundException if there is no such type
     */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return this.loadClass(name, false);
    }

    /**
     * The type named {@code name}, optionally linked.
     *
     * <p>The order is the one §5.3.2 fixes: already loaded, then the parent (or the bootstrap
     * loader when there is none), then this loader's own {@link #findClass}.
     *
     * @param name the binary name, with dots
     * @param resolve whether to link the type as well
     * @throws ClassNotFoundException if there is no such type
     */
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (this.getClassLoadingLock(name)) {
            Class<?> found = this.findLoadedClass(name);
            if (found == null) {
                // Delegation to the parent first (§5.3.2): the one higher up wins. It is what stops
                // a `java.lang.String` dropped on some classpath replacing the platform's.
                try {
                    found = this.parent != null
                            ? this.parent.loadClass(name, false)
                            : Class.forName(name);
                } catch (ClassNotFoundException notUpThere) {
                    found = null;
                }
            }
            if (found == null) {
                // And only then `findClass`: the hook the subclass writes.
                //
                // This step WAS MISSING, and its absence was invisible because this class's
                // `findClass` always throws: with no subclasses writing it, delegating and then not
                // asking gave the same result. With `java.net.URLClassLoader` --the first real
                // subclass in the tree-- it showed up at once: it found the file with `findResource`
                // and then `loadClass` threw `ClassNotFoundException` without having looked
                // at it.
                found = this.findClass(name);
            }
            if (resolve) {
                this.resolveClass(found);
            }
            return found;
        }
    }

    /**
     * The type named {@code name}, found by this loader alone.
     *
     * <p>The hook a subclass overrides. The base implementation finds nothing, which is what
     * makes {@link #loadClass(String, boolean)} fall through to the classpath.
     *
     * @param name the binary name, with dots
     * @throws ClassNotFoundException always, here
     */
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }

    /**
     * The type named {@code name} in the module {@code moduleName}, or {@code null}.
     *
     * @param moduleName the module to look in
     * @param name the binary name, with dots
     */
    protected Class<?> findClass(String moduleName, String name) {
        return null;
    }

    /**
     * The type named {@code name} if it is ALREADY loaded, without loading it.
     *
     * <p>The difference from {@link #loadClass(String)} is the effect, not the answer, and that
     * is the whole point: this one cannot trigger a load, so it is safe to call from inside one.
     *
     * @param name the binary name, with dots
     */
    protected final Class<?> findLoadedClass(String name) {
        int i = 0;
        while (i < this.definedNames.length) {
            if (this.definedNames[i].equals(name)) {
                return this.definedClasses[i];
            }
            i = i + 1;
        }
        return null;
    }

    // What THIS loader defined. That it is per instance and not a query to the VM is the difference
    // between "loaded" and "loaded by me", and it is the one `findLoadedClass` measures: a loader of
    // one's own does not see `java.lang.String` even though the VM has had it for ages, because it
    // was not the one that defined it.
    private String[] definedNames = new String[0];

    private Class<?>[] definedClasses = new Class<?>[0];

    private void remember(String name, Class<?> defined) {
        String[] names = new String[this.definedNames.length + 1];
        Class<?>[] classes = new Class<?>[this.definedClasses.length + 1];
        System.arraycopy(this.definedNames, 0, names, 0, this.definedNames.length);
        System.arraycopy(this.definedClasses, 0, classes, 0, this.definedClasses.length);
        names[this.definedNames.length] = name;
        classes[this.definedClasses.length] = defined;
        this.definedNames = names;
        this.definedClasses = classes;
    }

    /**
     * The type named {@code name}, found the way the bootstrap loader would.
     *
     * @param name the binary name, with dots
     * @throws ClassNotFoundException if there is no such type
     */
    protected final Class<?> findSystemClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }

    // ---- defining a type from bytes ----

    /**
     * A type built from {@code b}, with the name the bytes themselves declare.
     *
     * @param b the class file
     * @param off where the class file starts
     * @param len how long it is
     * @throws ClassFormatError if the bytes are not a valid class file
     * @deprecated the JDK deprecates it in favour of the overload that states the name, because
     *         a caller that does not know what it is defining cannot check that it got it.
     */
    @Deprecated(since = "1.1")
    protected final Class<?> defineClass(byte[] b, int off, int len) {
        return this.defineClass(null, b, off, len);
    }

    /**
     * A type named {@code name}, built from {@code b}.
     *
     * <p>The name is checked against what the bytes declare rather than trusted: a class file
     * that says it is something else would otherwise enter the VM under a name nothing can find
     * it by.
     *
     * @param name the binary name the bytes should declare, or null to accept whatever they say
     * @param b the class file
     * @param off where the class file starts
     * @param len how long it is
     * @throws ClassFormatError if the bytes are not a valid class file, or declare another name
     */
    protected final Class<?> defineClass(String name, byte[] b, int off, int len) {
        Class<?> defined = ClassLoader.defineClass0(name, b, off, len);
        if (defined == null) {
            throw new ClassFormatError(name);
        }
        this.remember(defined.getName(), defined);
        return defined;
    }

    /**
     * A type named {@code name}, built from {@code b}, in a protection domain.
     *
     * @param name the binary name the bytes should declare, or null
     * @param b the class file
     * @param off where the class file starts
     * @param len how long it is
     * @param protectionDomain accepted and ignored: KajiJDK enforces no security policy
     * @throws ClassFormatError if the bytes are not a valid class file
     */
    protected final Class<?> defineClass(String name, byte[] b, int off, int len,
            ProtectionDomain protectionDomain) {
        return this.defineClass(name, b, off, len);
    }

    /**
     * A type named {@code name}, built from the remaining bytes of {@code b}.
     *
     * @param name the binary name the bytes should declare, or null
     * @param b the class file, from its position to its limit
     * @param protectionDomain accepted and ignored
     * @throws ClassFormatError if the bytes are not a valid class file
     */
    protected final Class<?> defineClass(String name, ByteBuffer b,
            ProtectionDomain protectionDomain) {
        int length = b.remaining();
        byte[] bytes = new byte[length];
        b.get(bytes, 0, length);
        return this.defineClass(name, bytes, 0, length);
    }

    private static native Class<?> defineClass0(String name, byte[] b, int off, int len);

    /**
     * Links {@code c}.
     *
     * <p>A no-op, and a correct one: the JVM is permitted to link lazily and this one does, so
     * there is nothing an eager request can bring forward that a first use will not.
     *
     * @param c the type to link
     */
    protected final void resolveClass(Class<?> c) {
    }

    /**
     * Records the signers of {@code c}.
     *
     * <p>A no-op, and it matches {@code Class.getSigners()}, which always answers null: nothing
     * in KajiJDK signs a class, so there is nothing for this to record.
     *
     * @param c the type
     * @param signers its signers
     */
    protected final void setSigners(Class<?> c, Object[] signers) {
    }

    /**
     * The object to lock while loading {@code className}.
     *
     * <p>This loader itself, which is the non-parallel-capable answer and the honest one: the
     * interpreter runs one thread at a time inside a class load.
     *
     * @param className the binary name being loaded
     */
    protected Object getClassLoadingLock(String className) {
        return this;
    }

    /** Registers the calling loader as safe to use from several threads at once. */
    protected static boolean registerAsParallelCapable() {
        return true;
    }

    /** Whether this loader was registered as parallel capable. */
    public final boolean isRegisteredAsParallelCapable() {
        return true;
    }

    /**
     * The absolute path of the native library named {@code libname}, or {@code null}.
     *
     * <p>Always null: KajiJDK loads no native libraries, so there is no path to give.
     *
     * @param libname the library name
     */
    protected String findLibrary(String libname) {
        return null;
    }

    // ---- assertions ----
    //
    // The only thing a KajiJDK loader really keeps. `assert` desugars to a guard over a field each
    // class's `<clinit>` reads from `desiredAssertionStatus`, so these settings affect the classes
    // initialised AFTERWARDS -- which is what the specification says, and not a limitation of
    // ours.

    /**
     * Sets the assertion status for types this loader initializes from now on.
     *
     * @param enabled whether assertions should run
     */
    public void setDefaultAssertionStatus(boolean enabled) {
        this.defaultAssertionStatus = enabled;
    }

    /**
     * Sets the assertion status for one type.
     *
     * @param className the binary name, with dots
     * @param enabled whether assertions should run in it
     */
    public void setClassAssertionStatus(String className, boolean enabled) {
        int i = 0;
        while (i < this.assertionClasses.length) {
            if (this.assertionClasses[i].equals(className)) {
                this.assertionValues[i] = enabled;
                return;
            }
            i = i + 1;
        }
        String[] names = new String[this.assertionClasses.length + 1];
        boolean[] values = new boolean[this.assertionValues.length + 1];
        System.arraycopy(this.assertionClasses, 0, names, 0, this.assertionClasses.length);
        System.arraycopy(this.assertionValues, 0, values, 0, this.assertionValues.length);
        names[this.assertionClasses.length] = className;
        values[this.assertionValues.length] = enabled;
        this.assertionClasses = names;
        this.assertionValues = values;
    }

    /**
     * Sets the assertion status for a package and everything under it.
     *
     * <p>Recorded under the package name, and looked up by longest prefix, so
     * {@code "com.acme"} covers {@code "com.acme.deep.Thing"} unless {@code "com.acme.deep"} says
     * otherwise.
     *
     * @param packageName the package name, or null for the unnamed package
     * @param enabled whether assertions should run in it
     */
    public void setPackageAssertionStatus(String packageName, boolean enabled) {
        String key = packageName;
        if (key == null) {
            key = "";
        }
        this.setClassAssertionStatus(key + ".*", enabled);
    }

    /** Forgets every assertion setting and returns to assertions being off. */
    public void clearAssertionStatus() {
        this.defaultAssertionStatus = false;
        this.assertionClasses = new String[0];
        this.assertionValues = new boolean[0];
    }

    // Whether assertions run in `className`. `Class.desiredAssertionStatus` consults it, and that is
    // why it is package-private and not public: it is the channel between the two classes, not
    // API.
    boolean assertionStatusOf(String className) {
        int i = 0;
        while (i < this.assertionClasses.length) {
            if (this.assertionClasses[i].equals(className)) {
                return this.assertionValues[i];
            }
            i = i + 1;
        }
        // Then the longest package rule that is a prefix of the name.
        String best = null;
        boolean bestValue = this.defaultAssertionStatus;
        i = 0;
        while (i < this.assertionClasses.length) {
            String rule = this.assertionClasses[i];
            if (rule.endsWith(".*")) {
                String prefix = rule.substring(0, rule.length() - 1);
                if (className.startsWith(prefix)) {
                    if (best == null || prefix.length() > best.length()) {
                        best = prefix;
                        bestValue = this.assertionValues[i];
                    }
                }
            }
            i = i + 1;
        }
        return bestValue;
    }

}

// The empty enumeration the resource lookups hand back. Package-private and with no JDK
// counterpart, so it is part of no surface but this file's; raw to match the library's model.
final class EmptyEnumeration implements Enumeration<URL> {

    public boolean hasMoreElements() {
        return false;
    }

    public URL nextElement() {
        throw new NoSuchElementException();
    }
}
