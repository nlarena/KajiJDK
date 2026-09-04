package java.security;

// La cara del proveedor para una `Policy`.
//
// Solo `engineImplies` es abstracto, y eso dice que es lo minimo que hace falta para ser una
// politica: contestar si un dominio tiene un permiso. Las otras dos —enumerar los permisos de un
// origen o de un dominio— tienen implementacion base que devuelve
// `Policy.UNSUPPORTED_EMPTY_COLLECTION`, porque hay politicas que saben decidir sin saber
// enumerar: una regla como "todo lo firmado por X puede leer /var/datos" contesta `implies` al
// instante y no tiene una lista finita que devolver.
//
// KajiLibrary no trae ninguna implementacion. Ver `Policy`: el JDK 25 ya no deja instalar una
// politica global.
public abstract class PolicySpi {

    public PolicySpi() {
    }

    protected abstract boolean engineImplies(ProtectionDomain domain, Permission permission);

    // Relee la politica. La base no tiene de donde.
    protected void engineRefresh() {
    }

    protected PermissionCollection engineGetPermissions(CodeSource codesource) {
        return Policy.UNSUPPORTED_EMPTY_COLLECTION;
    }

    protected PermissionCollection engineGetPermissions(ProtectionDomain domain) {
        return Policy.UNSUPPORTED_EMPTY_COLLECTION;
    }
}
