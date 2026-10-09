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
    // The attributes a manifest would bring. They are `null` for a package minted on the fly from a
    // class's name, and have a value for one `ClassLoader.definePackage` created.
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

    // The full constructor, for `ClassLoader.definePackage`.
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
    // A package minted from a class's name --`Class.getPackage()`'s path-- has them all `null`: a
    // class loaded from a directory has no manifest to read, and "absent" is the right answer. One
    // created with `ClassLoader.definePackage` returns what it was handed, which is where the JDK
    // gets them from as well.

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

    /** Whether this package is sealed, that is, whether it was defined with a sealing base. */
    public boolean isSealed() {
        return this.sealBase != null;
    }

    /**
     * Whether this package is sealed **against that** URL.
     *
     * <p>Sealing means "every class of this package comes from the same place", and this question is
     * what checks it: a class arriving from another URL does not get in.
     *
     * @throws SecurityException if the package is not sealed
     */
    public boolean isSealed(URL url) {
        if (url == null) {
            throw new NullPointerException("url");
        }
        if (this.sealBase == null) {
            throw new SecurityException("package " + this.name + " is not sealed");
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
    // In the JDK these two walk **the caller's** loader. Here there is one loader, so the question
    // "whose loader" has only one answer and can be answered: they go to the single loader's
    // register, the same one `definePackage` populates.

    /** @deprecated a caller-sensitive lookup; here it goes to the single loader's register. */
    public static Package getPackage(String name) {
        return ClassLoader.getSystemClassLoader().getDefinedPackage(name);
    }

    /** The packages defined in the single loader. */
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
