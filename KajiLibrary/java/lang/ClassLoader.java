package java.lang;

// Por import y nombre simple: calificar el tipo en el uso no resuelve desde java.lang
// (finding #210).
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;

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
 * <p><strong>What is missing and why.</strong> Every resource method ({@code getResource},
 * {@code getResourceAsStream}, {@code getResources}, {@code resources} and their static twins) and
 * every package method ({@code getPackage}, {@code definePackage}, {@code getDefinedPackages})
 * needs machinery that does not exist yet -- reading a non-class file off the classpath, and
 * {@code java.lang.Package}. {@code getUnnamedModule} needs {@code java.lang.Module}. Those
 * members are absent rather than answering null, because a {@code getResource} that always missed
 * would be indistinguishable from a resource that is not there.
 */
public class ClassLoader {

    private final ClassLoader parent;

    private final String name;

    // El unico cargador que hay. `getSystemClassLoader` y `getPlatformClassLoader` devuelven
    // este mismo, y eso es exacto: no hay dos.
    private static final ClassLoader THE_LOADER = new ClassLoader(null, "app");

    // El estado de aserciones, que es lo unico que un cargador de KajiJDK realmente lleva. Vive
    // acá y no en la VM porque `assert` se desugariza a una lectura de `desiredAssertionStatus`
    // en el `<clinit>` de cada clase: cambiarlo despues de que una clase se inicializo no la
    // afecta, y eso es exactamente lo que la especificacion dice.
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
     * <p>{@code resolve} is accepted and ignored, and that is not laziness: the JVM is allowed to
     * resolve lazily, this one does, and asking for eager resolution cannot make a correct
     * program behave differently.
     *
     * @param name the binary name, with dots
     * @param resolve whether to link the type as well
     * @throws ClassNotFoundException if there is no such type
     */
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> found = this.findLoadedClass(name);
        if (found != null) {
            return found;
        }
        if (this.parent != null) {
            return this.parent.loadClass(name, resolve);
        }
        return Class.forName(name);
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

    // Lo que ESTE cargador definio. Que sea por instancia y no una consulta a la VM es la
    // diferencia entre "cargada" y "cargada por mi", y es la que `findLoadedClass` mide: un
    // cargador propio no ve `java.lang.String` aunque la VM la tenga hace rato, porque no fue el
    // quien la definio.
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
    // Lo unico que un cargador de KajiJDK realmente lleva. `assert` se desugariza a una guarda
    // sobre un campo que el `<clinit>` de cada clase lee de `desiredAssertionStatus`, asi que
    // estos ajustes afectan a las clases que se inicialicen DESPUES -- que es lo que la
    // especificacion dice, y no una limitacion nuestra.

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

    // Si las aserciones corren en `className`. Lo consulta `Class.desiredAssertionStatus`, y por
    // eso es package-private y no publico: es el canal entre las dos clases, no API.
    boolean assertionStatusOf(String className) {
        int i = 0;
        while (i < this.assertionClasses.length) {
            if (this.assertionClasses[i].equals(className)) {
                return this.assertionValues[i];
            }
            i = i + 1;
        }
        // Despues la regla de paquete mas larga que sea prefijo del nombre.
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

    /** {@code "app"} or the loader's name, which is what the JDK prints. */
    public String toString() {
        if (this.name == null) {
            return super.toString();
        }
        return this.name;
    }
}
