package java.lang;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.net.URL;

// KajiLibrary's java.lang.Package — the run-time handle for a package: its name, the version and
// vendor strings a JAR manifest would carry, and whether it is sealed. KajiJDK loads classes from
// plain directories, not from versioned, sealed, manifest-bearing JARs, so the manifest-derived
// answers are all "unknown" (null) and nothing is sealed. The name is the real, load-bearing part
// — {@link Class#getPackage()} hands one of these back for a loaded class.
//
// The JDK's `Package` extends the package-private `NamedPackage` and is built by the class loader
// from a module and a manifest. This one carries the name directly; the superclass is an
// implementation detail with no public surface, so dropping it changes nothing observable.
public final class Package implements AnnotatedElement {

    private final String name;
    // Los atributos que un manifiesto traeria. Son `null` para un paquete que se mintio al vuelo
    // desde el nombre de una clase, y tienen valor para uno que `ClassLoader.definePackage` creo.
    private final String specTitle;
    private final String specVersion;
    private final String specVendor;
    private final String implTitle;
    private final String implVersion;
    private final String implVendor;
    private final URL sealBase;

    // Package-private: only the class library (i.e. {@link Class}) mints these, from a loaded
    // class's package name.
    Package(String name) {
        this(name, null, null, null, null, null, null, null);
    }

    // El constructor completo, para `ClassLoader.definePackage`.
    Package(String name, String specTitle, String specVersion, String specVendor,
            String implTitle, String implVersion, String implVendor, URL sealBase) {
        this.name = name;
        this.specTitle = specTitle;
        this.specVersion = specVersion;
        this.specVendor = specVendor;
        this.implTitle = implTitle;
        this.implVersion = implVersion;
        this.implVendor = implVendor;
        this.sealBase = sealBase;
    }

    /** The fully-qualified name of this package (e.g. {@code java.lang}); {@code ""} for the default. */
    public String getName() {
        return this.name;
    }

    // ---- manifest attributes ----
    //
    // Un paquete que se minto desde el nombre de una clase --el camino de `Class.getPackage()`--
    // los tiene todos en `null`: una clase cargada de un directorio no tiene manifiesto que leer, y
    // "ausente" es la respuesta correcta. Uno creado con `ClassLoader.definePackage` devuelve lo que
    // le pasaron, que es de donde el JDK los saca tambien.

    public String getSpecificationTitle() {
        return this.specTitle;
    }

    public String getSpecificationVersion() {
        return this.specVersion;
    }

    public String getSpecificationVendor() {
        return this.specVendor;
    }

    public String getImplementationTitle() {
        return this.implTitle;
    }

    public String getImplementationVersion() {
        return this.implVersion;
    }

    public String getImplementationVendor() {
        return this.implVendor;
    }

    /** Si este paquete esta sellado, o sea si se lo definio con una base de sellado. */
    public boolean isSealed() {
        return this.sealBase != null;
    }

    /**
     * Si este paquete esta sellado **contra esa** URL.
     *
     * <p>Sellar quiere decir "todas las clases de este paquete vienen del mismo lugar", y esta
     * pregunta es la que lo comprueba: una clase que llega de otra URL no entra.
     *
     * @throws SecurityException si el paquete no esta sellado
     */
    public boolean isSealed(URL url) {
        if (url == null) {
            throw new NullPointerException("url");
        }
        if (this.sealBase == null) {
            throw new SecurityException("el paquete " + this.name + " no esta sellado");
        }
        return this.sealBase.equals(url);
    }

    /**
     * Whether this package's specification version is at least {@code desired}, comparing the two
     * as dotted sequences of non-negative integers.
     *
     * @throws NumberFormatException if either version is not a dotted integer sequence — which
     *         includes the (always, here) case of an absent specification version
     */
    public boolean isCompatibleWith(String desired) throws NumberFormatException {
        String spec = getSpecificationVersion();
        if (spec == null || spec.length() < 1) {
            throw new NumberFormatException("Empty version string");
        }
        int[] have = parseVersion(spec);
        int[] want = parseVersion(desired);
        int n = Math.max(have.length, want.length);
        int i = 0;
        while (i < n) {
            int a = i < have.length ? have[i] : 0;
            int b = i < want.length ? want[i] : 0;
            if (a != b) {
                return a > b;
            }
            i = i + 1;
        }
        return true;
    }

    private static int[] parseVersion(String s) {
        // Count components first: our compiler has no dynamic list of ints handy, so size the array
        // by counting dots, then fill it.
        int dots = 0;
        int j = 0;
        while (j < s.length()) {
            if (s.charAt(j) == '.') {
                dots = dots + 1;
            }
            j = j + 1;
        }
        int[] out = new int[dots + 1];
        int start = 0;
        int idx = 0;
        int k = 0;
        while (k <= s.length()) {
            if (k == s.length() || s.charAt(k) == '.') {
                String part = s.substring(start, k);
                if (part.length() < 1) {
                    throw new NumberFormatException("Empty version component");
                }
                out[idx] = Integer.parseInt(part);
                idx = idx + 1;
                start = k + 1;
            }
            k = k + 1;
        }
        return out;
    }

    // ---- deprecated caller-sensitive lookups ----
    //
    // En el JDK estas dos caminan el loader de **quien llama**. Aca hay un solo loader, asi que la
    // pregunta "el loader de quien" no tiene mas que una respuesta y se la puede contestar: van al
    // registro del loader unico, el mismo que `definePackage` puebla.

    /** @deprecated una busqueda sensible al llamador; aca va al registro del loader unico. */
    public static Package getPackage(String name) {
        return ClassLoader.getSystemClassLoader().getDefinedPackage(name);
    }

    /** Los paquetes definidos en el loader unico. */
    public static Package[] getPackages() {
        return ClassLoader.getSystemClassLoader().getDefinedPackages();
    }

    // ---- annotations ----
    //
    // A package is annotated through its `package-info` class. KajiLibrary does not model
    // `package-info` annotations, so a package presents as carrying none. All seven methods are
    // spelled out because the reference declares them all on `Package` rather than inheriting the
    // `AnnotatedElement` defaults.

    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return null;
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
        return (A[]) Array.newInstance(annotationClass, 0);
    }

    public Annotation[] getAnnotations() {
        return new Annotation[0];
    }

    public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass) {
        return null;
    }

    public <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<A> annotationClass) {
        return (A[]) Array.newInstance(annotationClass, 0);
    }

    public Annotation[] getDeclaredAnnotations() {
        return new Annotation[0];
    }

    public int hashCode() {
        return this.name.hashCode();
    }

    public String toString() {
        String spec = getSpecificationTitle();
        String ver = getSpecificationVersion();
        if (spec != null && spec.length() > 0 && ver != null && ver.length() > 0) {
            return "package " + this.name + ", " + spec + ", version " + ver;
        }
        return "package " + this.name;
    }
}
