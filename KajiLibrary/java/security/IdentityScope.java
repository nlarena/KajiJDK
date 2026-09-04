package java.security;

import java.util.Enumeration;

// Un conjunto de identidades, que a su vez **es** una identidad.
//
// Que un ambito sea tambien una identidad es lo que permite anidarlos: un ambito tiene nombre,
// puede tener clave, y puede vivir dentro de otro. Con eso se armaba una jerarquia de espacios de
// nombres —el ambito del sistema, el de un usuario, el de una aplicacion— donde el nombre completo
// de una identidad la ubica sin ambigüedad.
//
// Obsoleto desde 1.2, reemplazado por `KeyStore`. La invariante que impone —dentro de un ambito no
// puede haber dos identidades con el mismo nombre ni dos con la misma clave— la hacen cumplir las
// subclases en `addIdentity`; esta clase no puede, porque no guarda nada: todos los metodos que
// tocan la coleccion son abstractos.
@Deprecated
public abstract class IdentityScope extends Identity {

    // El ambito del sistema. En KajiLibrary nadie lo instala, asi que arranca —y se queda— en
    // null mientras nadie llame a `setSystemScope`. Devolver null es lo correcto: no hay ninguno.
    private static IdentityScope scope;

    protected IdentityScope() {
        this("restoring...");
    }

    public IdentityScope(String name) {
        super(name);
    }

    public IdentityScope(String name, IdentityScope scope) throws KeyManagementException {
        super(name, scope);
    }

    // El ambito del sistema, o null si no hay ninguno instalado.
    public static IdentityScope getSystemScope() {
        return scope;
    }

    // Instala el ambito del sistema. `protected` y estatico: solo una subclase puede hacerlo, que
    // era la forma de que no cualquiera reemplazara el almacen de identidades del proceso.
    protected static void setSystemScope(IdentityScope scope) {
        IdentityScope.scope = scope;
    }

    // Cuantas identidades hay.
    public abstract int size();

    public abstract Identity getIdentity(String name);

    // Por principal: se resuelve por nombre.
    //
    // Concreto y no abstracto porque no aporta nada nuevo — un `Principal` es un nombre— y
    // obligar a implementarlo daria lugar a que una subclase lo hiciera distinto de
    // `getIdentity(String)` sin querer.
    public Identity getIdentity(Principal principal) {
        return this.getIdentity(principal.getName());
    }

    public abstract Identity getIdentity(PublicKey key);

    public abstract void addIdentity(Identity identity) throws KeyManagementException;

    public abstract void removeIdentity(Identity identity) throws KeyManagementException;

    public abstract Enumeration<Identity> identities();

    @Override
    public String toString() {
        return super.toString() + "[" + this.size() + "]";
    }
}
