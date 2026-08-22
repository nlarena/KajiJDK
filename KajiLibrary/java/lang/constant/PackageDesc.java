package java.lang.constant;

// A nominal descriptor for a package. It carries the name in BOTH spellings the class file
// format uses — dotted (`java.lang`) for source, slash-separated (`java/lang`) for the constant
// pool — and the pair of factories exists so a caller never has to guess which one it holds.
public interface PackageDesc {

    // `public` is spelled out on the static methods although interface members are implicitly
    // public: our compiler applies that implicit modifier to abstract methods but not to static
    // ones (finding #116).
    public static PackageDesc of(String name) {
        return new ConstantPackageDesc(DescNames.swap(name, '.', '/'));
    }

    public static PackageDesc ofInternalName(String name) {
        return new ConstantPackageDesc(name);
    }

    // The slash-separated form, as it appears in a class file.
    String internalName();

    // The dotted form. A default, because it is derivable from the internal name.
    default String name() {
        return DescNames.swap(internalName(), '/', '.');
    }

    boolean equals(Object o);
}

// The implementation lives in this file, not its own: interface and implementation reference
// each other, and our compiler builds one file per invocation, so a two-file cycle would need a
// stub-and-recompile bootstrap. Same-file top-level classes are the project's idiom for this
// (the regex node hierarchy is fourteen of them in one file). The gate skips it — the JDK's own
// implementation is a nested private class, so there is no counterpart to compare against.
class ConstantPackageDesc implements PackageDesc {

    private final String internalName;

    ConstantPackageDesc(String internalName) {
        this.internalName = internalName;
    }

    public String internalName() {
        return internalName;
    }

    public boolean equals(Object o) {
        boolean same = false;
        if (o instanceof ConstantPackageDesc) {
            ConstantPackageDesc other = (ConstantPackageDesc) o;
            same = internalName.equals(other.internalName);
        }
        return same;
    }

    public int hashCode() {
        return internalName.hashCode();
    }

    public String toString() {
        return "PackageDesc[" + name() + "]";
    }
}
