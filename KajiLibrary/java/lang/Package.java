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

    // Package-private: only the class library (i.e. {@link Class}) mints these, from a loaded
    // class's package name.
    Package(String name) {
        this.name = name;
    }

    /** The fully-qualified name of this package (e.g. {@code java.lang}); {@code ""} for the default. */
    public String getName() {
        return this.name;
    }

    // ---- manifest attributes ----
    //
    // All null: a class loaded from a directory has no manifest to read a title, version or vendor
    // from. The methods exist so code that probes for them degrades to "absent" rather than failing.

    public String getSpecificationTitle() {
        return null;
    }

    public String getSpecificationVersion() {
        return null;
    }

    public String getSpecificationVendor() {
        return null;
    }

    public String getImplementationTitle() {
        return null;
    }

    public String getImplementationVersion() {
        return null;
    }

    public String getImplementationVendor() {
        return null;
    }

    /** Whether this package is sealed. Nothing is, without a JAR to seal it against. */
    public boolean isSealed() {
        return false;
    }

    /** Whether this package is sealed with respect to {@code url}. Never, here. */
    public boolean isSealed(URL url) {
        return false;
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
    // The JDK walks the calling class's loader for these. KajiLibrary does not track defined
    // packages behind the loader, so the honest answers are "none".

    /** @deprecated a caller-sensitive lookup; returns {@code null} here. */
    public static Package getPackage(String name) {
        return null;
    }

    /** The packages known to the caller's loader — none are tracked here. */
    public static Package[] getPackages() {
        return new Package[0];
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
