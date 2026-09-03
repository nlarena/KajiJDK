package java.nio.file.attribute;

import java.security.Principal;

// La identidad de un usuario tal como la nombra el sistema de archivos.
//
// Es `java.security.Principal` sin agregar nada: el subtipo existe solo para que las firmas de
// `FileOwnerAttributeView` no acepten cualquier `Principal`. KajiJDK no tiene con que producir uno
// --no hay nativo que consulte usuarios-- y por eso `UserPrincipalLookupService` queda abstracta y
// sin implementacion.
public interface UserPrincipal extends Principal {
}
