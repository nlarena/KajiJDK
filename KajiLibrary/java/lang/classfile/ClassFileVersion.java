package java.lang.classfile;

// El par `major_version`/`minor_version` del encabezado (JVMS §4.1), como elemento de clase para que
// una transformación pueda cambiarlo igual que a cualquier otra pieza.
public interface ClassFileVersion extends ClassElement {

    /** La versión mayor. */
    int majorVersion();

    /** La versión menor. */
    int minorVersion();

    /** El par. */
    public static ClassFileVersion of(int majorVersion, int minorVersion) {
        return new jdk.internal.classfile.impl.ClassFileVersionImpl(majorVersion, minorVersion);
    }
}
