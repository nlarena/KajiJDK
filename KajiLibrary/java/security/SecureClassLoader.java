package java.security;

import java.nio.ByteBuffer;

// Un cargador que le asocia a cada clase el origen del que vino.
//
// Es la pieza que conecta el modelo de permisos con el de carga de clases: `ClassLoader` sabe
// definir una clase a partir de bytes, y esta subclase le agrega el `CodeSource`, que es lo que
// permite que despues alguien pregunte "¿de donde salio esta clase?" y obtenga una respuesta
// verificable en vez de un nombre. Sin esto, todo el codigo del proceso seria indistinguible.
//
// Los dominios se **cachean por origen**: dos clases del mismo jar comparten `ProtectionDomain`, y
// eso no es solo un ahorro de memoria — es lo que hace que concederle un permiso a un jar valga
// para todas sus clases y no haya que repetir la decision por cada una.
public class SecureClassLoader extends ClassLoader {

    private final java.util.HashMap<CodeSource, ProtectionDomain> dominios =
        new java.util.HashMap<CodeSource, ProtectionDomain>();

    protected SecureClassLoader(ClassLoader parent) {
        super(parent);
    }

    protected SecureClassLoader() {
        super();
    }

    protected SecureClassLoader(String name, ClassLoader parent) {
        super(name, parent);
    }

    // Define una clase asociandola al origen dado.
    protected final Class<?> defineClass(String name, byte[] b, int off, int len,
                                         CodeSource cs) {
        return super.defineClass(name, b, off, len, this.dominioPara(cs));
    }

    protected final Class<?> defineClass(String name, ByteBuffer b, CodeSource cs) {
        return super.defineClass(name, b, this.dominioPara(cs));
    }

    // Los permisos que le corresponden a ese origen.
    //
    // La base devuelve una coleccion **vacia**, igual que el JDK, y eso no significa "sin
    // permisos": el dominio que se arma con ella es dinamico, asi que lo que finalmente pueda
    // hacer lo decide la `Policy` en el momento de preguntar. Una subclase que quiera conceder
    // algo fijo —el clasico permiso de leer el propio jar— lo agrega aca.
    protected PermissionCollection getPermissions(CodeSource codesource) {
        return new Permissions();
    }

    // El dominio de ese origen, creandolo la primera vez.
    private synchronized ProtectionDomain dominioPara(CodeSource cs) {
        if (cs == null) {
            return null;
        }
        ProtectionDomain pd = this.dominios.get(cs);
        if (pd == null) {
            // Cuatro argumentos: dominio **dinamico**, para que un cambio de politica alcance a
            // clases ya cargadas. Con el de dos quedarian congeladas con los permisos que hubiera
            // en el momento de definirlas.
            pd = new ProtectionDomain(cs, this.getPermissions(cs), this, null);
            this.dominios.put(cs, pd);
        }
        return pd;
    }
}
