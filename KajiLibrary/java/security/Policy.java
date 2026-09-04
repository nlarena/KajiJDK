package java.security;

import java.util.Enumeration;
import java.util.NoSuchElementException;

// La politica: dado un dominio, que permisos tiene.
//
// ===============================================================================================
// LA POLITICA VIGENTE ES LA QUE NO CONCEDE NADA, Y NO SE PUEDE CAMBIAR
// ===============================================================================================
//
// Esto sorprende y es lo que hace el JDK 25, verificado contra el:
//
//   - `getPolicy()` devuelve un objeto **no nulo** cuya respuesta a todo es "no". `implies` da
//     `false` y `getPermissions` devuelve `UNSUPPORTED_EMPTY_COLLECTION`.
//   - `setPolicy(...)` tira `UnsupportedOperationException`. **No se puede instalar una politica.**
//   - `getInstance(...)` tira `NoSuchAlgorithmException`: no hay ningun proveedor de tipo
//     "Policy".
//
// Que `getPolicy()` no devuelva null es lo que permite que `ProtectionDomain.implies` la consulte
// sin preguntarse nada; que no conceda nada es lo que hace que consultarla sea inofensivo. El
// mecanismo quedo con la forma intacta y el contenido vaciado, y esta clase reproduce eso en vez
// de simular una politica que el JDK ya no deja instalar.
//
// `UNSUPPORTED_EMPTY_COLLECTION` merece una nota aparte: **no es** una coleccion vacia comun. Su
// `add` tira `SecurityException` y su `implies` siempre da `false`. La diferencia con una vacia
// normal es semantica — significa "no se puede contestar", no "no hay permisos"— y sirve para que
// un llamador que igual intente agregarle algo se entere en vez de creer que lo logro.
public abstract class Policy {

    // La respuesta cuando no hay politica que consultar. Ver la cabecera: niega y no se deja
    // modificar.
    public static final PermissionCollection UNSUPPORTED_EMPTY_COLLECTION =
        new ColeccionNoSoportada();

    // La unica politica que existe. Estatica y final: `setPolicy` no la cambia.
    private static final Policy VIGENTE = new PoliticaVacia();

    public Policy() {
    }

    // La politica vigente. Nunca null.
    public static Policy getPolicy() {
        return VIGENTE;
    }

    // Siempre tira. Instalar una politica global dejo de estar soportado cuando el
    // `SecurityManager` quedo deshabilitado, y fingir que se instalo seria peor: el llamador
    // creeria que sus reglas rigen.
    public static void setPolicy(Policy p) {
        throw new UnsupportedOperationException(
            "Setting a system-wide Policy object is not supported");
    }

    public static Policy getInstance(String type, Policy.Parameters params)
            throws NoSuchAlgorithmException {
        return buscar(type, null);
    }

    public static Policy getInstance(String type, Policy.Parameters params, String provider)
            throws NoSuchProviderException, NoSuchAlgorithmException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return buscar(type, p);
    }

    public static Policy getInstance(String type, Policy.Parameters params, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        return buscar(type, provider);
    }

    // No hay ningun proveedor de tipo "Policy" registrado, ni lo habra: escribir un proveedor de
    // politicas requiere un parser de archivos de politica que esta biblioteca no tiene. La
    // busqueda se hace igual —contra los proveedores que haya— para que el dia que exista uno,
    // esto lo encuentre sin tocar nada.
    private static Policy buscar(String type, Provider unico) throws NoSuchAlgorithmException {
        if (type == null) {
            throw new NullPointerException("null type name");
        }
        Provider[] provs = unico == null ? Security.getProviders() : new Provider[] {unico};
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("Policy", type);
            if (s != null) {
                Object o = s.newInstance(null);
                if (o instanceof Policy) {
                    return (Policy) o;
                }
                throw new NoSuchAlgorithmException(
                    "class configured for Policy is not a Policy: " + s.getClassName());
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(type + " Policy not available");
    }

    // El proveedor del que salio, o null si no salio de una fabrica.
    public Provider getProvider() {
        return null;
    }

    public String getType() {
        return null;
    }

    public Policy.Parameters getParameters() {
        return null;
    }

    // Los permisos que esta politica le da a ese origen.
    public PermissionCollection getPermissions(CodeSource codesource) {
        return UNSUPPORTED_EMPTY_COLLECTION;
    }

    // Los permisos que esta politica le da a ese dominio: los propios del dominio mas los que
    // correspondan por origen.
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        PermissionCollection propios = domain == null ? null : domain.getPermissions();
        PermissionCollection porOrigen =
            domain == null ? null : this.getPermissions(domain.getCodeSource());
        if (porOrigen == UNSUPPORTED_EMPTY_COLLECTION && propios == null) {
            return UNSUPPORTED_EMPTY_COLLECTION;
        }
        Permissions juntos = new Permissions();
        agregarTodo(juntos, propios);
        if (porOrigen != UNSUPPORTED_EMPTY_COLLECTION) {
            agregarTodo(juntos, porOrigen);
        }
        return juntos;
    }

    private static void agregarTodo(Permissions destino, PermissionCollection origen) {
        if (origen == null) {
            return;
        }
        Enumeration<Permission> e = origen.elements();
        while (e.hasMoreElements()) {
            destino.add(e.nextElement());
        }
    }

    // Si esta politica le concede el permiso a ese dominio.
    public boolean implies(ProtectionDomain domain, Permission permission) {
        if (domain == null) {
            return false;
        }
        PermissionCollection pc = this.getPermissions(domain);
        if (pc == null) {
            return false;
        }
        return pc.implies(permission);
    }

    // Relee la politica de donde sea que venga. La base no tiene de donde: no hace nada.
    public void refresh() {
    }

    // Marca los parametros de configuracion de una politica. Vacia, como
    // `AlgorithmParameterSpec`: solo hace falta el tipo comun.
    public interface Parameters {
    }
}

// La politica que no concede nada. Ver la cabecera de `Policy`.
final class PoliticaVacia extends Policy {

    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
        return Policy.UNSUPPORTED_EMPTY_COLLECTION;
    }

    @Override
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        return Policy.UNSUPPORTED_EMPTY_COLLECTION;
    }

    @Override
    public boolean implies(ProtectionDomain domain, Permission permission) {
        return false;
    }
}

// "No se puede contestar", disfrazado de coleccion.
//
// No es lo mismo que una coleccion vacia: `add` tira en vez de aceptar en silencio, para que quien
// crea que esta configurando permisos se entere de que no.
final class ColeccionNoSoportada extends PermissionCollection {

    @Override
    public void add(Permission permission) {
        throw new SecurityException(
            "attempt to add a Permission to a readonly PermissionCollection");
    }

    @Override
    public boolean implies(Permission permission) {
        return false;
    }

    @Override
    public Enumeration<Permission> elements() {
        return new EnumVacia();
    }
}

final class EnumVacia implements Enumeration<Permission> {

    public boolean hasMoreElements() {
        return false;
    }

    public Permission nextElement() {
        throw new NoSuchElementException();
    }
}
