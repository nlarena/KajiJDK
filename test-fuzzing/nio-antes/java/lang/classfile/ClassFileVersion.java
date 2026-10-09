package java.lang.classfile;

// The header's `major_version`/`minor_version` pair (JVMS §4.1), as a class element so a
// transformation can change it just like any other piece.
public interface ClassFileVersion extends ClassElement {

    /** The major version. */
    int majorVersion();

    /** The minor version. */
    int minorVersion();

    /** The pair. */
    public static ClassFileVersion of(int majorVersion, int minorVersion) {
        return new jdk.internal.classfile.impl.ClassFileVersionImpl(majorVersion, minorVersion);
    }
}
