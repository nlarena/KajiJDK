package java.nio.file.attribute;

import java.io.IOException;

// La vista que sabe leer y cambiar el dueño de un archivo. Su nombre es `"owner"`, salvo cuando se
// llega a ella a traves de `PosixFileAttributeView` o `AclFileAttributeView`, que la heredan y
// devuelven el suyo.
//
// Sin implementacion en KajiJDK: no hay nativo que consulte ni cambie el dueño.
public interface FileOwnerAttributeView extends FileAttributeView {

    /** `"owner"`, o el nombre de la vista que la extiende. */
    String name();

    /** El dueño. */
    UserPrincipal getOwner() throws IOException;

    /** Cambia el dueño. */
    void setOwner(UserPrincipal owner) throws IOException;
}
