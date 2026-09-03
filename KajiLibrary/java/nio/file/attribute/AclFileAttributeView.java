package java.nio.file.attribute;

import java.io.IOException;
import java.util.List;

// La vista `"acl"`: la lista de control de acceso al estilo NFSv4, que es la que usa Windows.
//
// La ACL es una **lista** y no un conjunto porque el orden decide: se evalua de arriba hacia abajo y
// la primera entrada que aplica gana, asi que una `DENY` antes de una `ALLOW` no es lo mismo que al
// reves.
//
// Sin implementacion en KajiJDK: no hay nativo que lea ni escriba ACLs.
public interface AclFileAttributeView extends FileOwnerAttributeView {

    /** Siempre `"acl"`. */
    String name();

    /** La ACL, en orden de evaluacion. */
    List<AclEntry> getAcl() throws IOException;

    /** Reemplaza la ACL entera. */
    void setAcl(List<AclEntry> acl) throws IOException;
}
