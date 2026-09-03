package java.nio.file.attribute;

import java.io.IOException;

// El servicio que traduce un nombre a un `UserPrincipal` o a un `GroupPrincipal`.
//
// Abstracta y **sin implementacion en KajiJDK**: no hay nativo que consulte la base de usuarios del
// sistema. Por eso `FileSystem.getUserPrincipalLookupService()` de `KajiFileSystem` levanta
// `UnsupportedOperationException` --que es lo que la spec ya contempla para un sistema de archivos
// que no soporta principals-- en vez de devolver un servicio que conteste nombres inventados.
public abstract class UserPrincipalLookupService {

    /** Para las subclases. */
    protected UserPrincipalLookupService() {
    }

    /**
     * Busca un usuario por nombre.
     *
     * @throws UserPrincipalNotFoundException si no existe
     */
    public abstract UserPrincipal lookupPrincipalByName(String name) throws IOException;

    /**
     * Busca un grupo por nombre.
     *
     * @throws UserPrincipalNotFoundException si no existe
     */
    public abstract GroupPrincipal lookupPrincipalByGroupName(String group) throws IOException;
}
