package java.lang;

// Por import y nombre simple: calificar el tipo en el uso no resuelve desde java.lang
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
    // El loader **si** lleva un registro de los paquetes que definio. Antes no lo llevaba, y eso
    // dejaba el par cortado por la mitad: estaban las dos consultas (`getDefinedPackage`,
    // `getDefinedPackages`) y no estaba lo unico que podria poblarlas (`definePackage`), asi que las
    // dos contestaban "ninguno" para siempre.
    //
    // Lo que **no** cambia es de donde salen: aca nadie lee manifiestos, asi que el registro solo
    // tiene lo que alguien haya definido a mano. `Class.getPackage()` sigue acuniando un `Package`
    // al vuelo desde el nombre de la clase, sin pasar por aca -- son dos preguntas distintas.

    private final java.util.HashMap<String, Package> paquetes =
            new java.util.HashMap<String, Package>();

    /**
     * Define un paquete en este loader, con los atributos que un manifiesto traeria.
     *
     * <p>Es `protected` porque es una operacion del loader sobre si mismo: quien define clases es
     * quien sabe de que JAR vinieron y, por lo tanto, quien puede decir su version y su vendor.
     *
     * @param name el nombre del paquete
     * @param sealBase la URL contra la que el paquete queda sellado, o `null` para no sellarlo
     * @throws IllegalArgumentException si el paquete ya estaba definido en este loader
     * @throws NullPointerException si `name` es `null`
     */
    protected Package definePackage(String name, String specTitle, String specVersion,
            String specVendor, String implTitle, String implVersion, String implVendor,
            URL sealBase) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        synchronized (this.paquetes) {
            if (this.paquetes.containsKey(name)) {
                throw new IllegalArgumentException("el paquete " + name + " ya esta definido");
            }
            Package p = new Package(name, specTitle, specVersion, specVendor,
                    implTitle, implVersion, implVendor, sealBase);
            this.paquetes.put(name, p);
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
        synchronized (this.paquetes) {
            return this.paquetes.get(name);
        }
    }

    /** The packages defined by this loader. */
    public final Package[] getDefinedPackages() {
        synchronized (this.paquetes) {
            return this.paquetes.values().toArray(new Package[0]);
        }
    }

    /**
     * El paquete de ese nombre, buscando **tambien en los padres**.
     *
     * <p>Es la diferencia con `getDefinedPackage`, que mira solo este loader. Aca hay un solo loader
     * y por lo tanto ningun padre, asi que las dos respuestas coinciden -- pero la delegacion esta
     * escrita, porque es la parte del contrato que importa el dia que haya jerarquia.
     *
     * @deprecated en el JDK, porque no distingue paquetes del mismo nombre en loaders distintos.
     *     Usar {@link #getDefinedPackage(String)}.
     */
    protected Package getPackage(String name) {
        Package propio = this.getDefinedPackage(name);
        if (propio != null) {
            return propio;
        }
        if (this.parent != null) {
            return this.parent.getPackage(name);
        }
        return null;
    }

    /**
     * Todos los paquetes visibles desde este loader: los suyos y los de sus padres.
     *
     * <p>Los del padre van **primero** y los propios despues, que es el orden en que se los
     * encuentra al delegar.
     */
    protected Package[] getPackages() {
        Package[] propios = this.getDefinedPackages();
        if (this.parent == null) {
            return propios;
        }
        Package[] delPadre = this.parent.getPackages();
        Package[] todos = new Package[delPadre.length + propios.length];
        System.arraycopy(delPadre, 0, todos, 0, delPadre.length);
        System.arraycopy(propios, 0, todos, delPadre.length, propios.length);
        return todos;
    }

    // ---- resources ----
    //
    // KajiJDK serves classes off the class path, not co-located resource files, so every resource
    // lookup honestly misses: a stream is {@code null}, an enumeration or stream of URLs is empty.
    // This is the same answer the real loader gives for a resource that is simply not present.

    // Los tres `find*` de abajo son **los puntos de extension**, y las tres formas publicas los
    // llaman. Ese cableado es la parte que faltaba y no era cosmetica: antes `getResource` devolvia
    // `null` directo, asi que una subclase que sobrescribiera `findResource` --que es la unica forma
    // documentada de servir recursos propios-- **era ignorada**. El metodo publico no cumplia el
    // protocolo que su javadoc promete.

    /**
     * The resource of this name, or {@code null}.
     *
     * <p>Delega primero en el padre y despues en {@link #findResource(String)}, que es el orden del
     * JDK: un recurso del padre gana sobre uno propio del mismo nombre.
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

    /** Every resource of this name: los del padre y despues los propios. */
    public Enumeration<URL> getResources(String name) throws IOException {
        if (name == null) {
            throw new NullPointerException("name");
        }
        java.util.ArrayList<URL> todos = new java.util.ArrayList<URL>();
        if (this.parent != null) {
            Enumeration<URL> delPadre = this.parent.getResources(name);
            while (delPadre.hasMoreElements()) {
                todos.add(delPadre.nextElement());
            }
        }
        Enumeration<URL> propios = this.findResources(name);
        while (propios.hasMoreElements()) {
            todos.add(propios.nextElement());
        }
        return java.util.Collections.enumeration(todos);
    }

    /** Every resource of this name as a stream. */
    public Stream<URL> resources(String name) {
        try {
            Enumeration<URL> e = this.getResources(name);
            java.util.ArrayList<URL> todos = new java.util.ArrayList<URL>();
            while (e.hasMoreElements()) {
                todos.add(e.nextElement());
            }
            return todos.stream();
        } catch (IOException ex) {
            return Stream.empty();
        }
    }

    // ---- los puntos de extension -------------------------------------------------------------------
    //
    // Los tres devuelven "nada": KajiJDK sirve clases del classpath, no archivos de recursos al lado.
    // Es la misma respuesta que da el loader real para un recurso que no esta, y es donde una
    // subclase que quiera servir recursos propios tiene que meter mano.

    /** El recurso de ese nombre que **este** loader sirve, o `null`. */
    protected URL findResource(String name) {
        return null;
    }

    /** El recurso de ese nombre en ese modulo, o `null`. */
    protected URL findResource(String moduleName, String name) throws IOException {
        return null;
    }

    /** Todos los recursos de ese nombre que **este** loader sirve. */
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
                // Delegacion al padre primero (§5.3.2): el que esta mas arriba gana. Es lo que
                // impide que un `java.lang.String` puesto en un classpath cualquiera reemplace al
                // de la plataforma.
                try {
                    found = this.parent != null
                            ? this.parent.loadClass(name, false)
                            : Class.forName(name);
                } catch (ClassNotFoundException noEstaArriba) {
                    found = null;
                }
            }
            if (found == null) {
                // Y recien despues, `findClass`: el gancho que la subclase escribe.
                //
                // Este paso FALTABA, y su ausencia no se veia porque el `findClass` de esta clase
                // tira siempre: sin subclases que lo escribieran, delegar y despues no preguntar
                // daba el mismo resultado. Con `java.net.URLClassLoader` --la primera subclase real
                // del arbol-- se vio de inmediato: encontraba el archivo con `findResource` y
                // despues `loadClass` tiraba `ClassNotFoundException` sin haberlo mirado.
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
