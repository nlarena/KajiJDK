package java.security;

// Un acceso fue denegado, y opcionalmente que permiso hubiera hecho falta.
//
// Lo unico que agrega sobre `SecurityException` es `getPermission()`, y ese dato es la diferencia
// entre un mensaje inutil y uno accionable: quien la atrapa puede decir exactamente que linea de
// politica falta. En KajiLibrary nada la tira sola —no hay control de acceso activo— pero se
// puede construir y sigue siendo el tipo que las firmas nombran.
@Deprecated
public class AccessControlException extends SecurityException {

    // El permiso que falto, o null si el que la tiro no lo dijo.
    private final Permission perm;

    public AccessControlException(String s) {
        super(s);
        this.perm = null;
    }

    public AccessControlException(String s, Permission p) {
        super(s);
        this.perm = p;
    }

    // El permiso que hubiera hecho falta, o null.
    public Permission getPermission() {
        return this.perm;
    }
}
