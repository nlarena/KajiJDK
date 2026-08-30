package java.security;

import java.io.Serializable;

// Un permiso: un nombre, unas acciones opcionales, y la regla de cuando un permiso **implica** a
// otro.
//
// `implies` es el corazon de todo el modelo y lo que lo distingue de una simple lista de
// etiquetas: tener `FilePermission("/tmp/*", "read")` implica tener
// `FilePermission("/tmp/x.txt", "read")`, sin que nadie haya enumerado el segundo. Un chequeo de
// acceso es siempre "¿alguno de los permisos que tengo implica el que hace falta?".
//
// La clase es abstracta porque esa regla no se puede escribir en general: cada clase de permiso
// tiene su propia nocion de "mas amplio que". `BasicPermission` da la de nombres jerarquicos con
// comodin, que es la que usan casi todos.
//
// **Nota sobre el estado del modelo**: desde JDK 24 el `SecurityManager` esta permanentemente
// deshabilitado, asi que estas clases siguen en las firmas del JDK pero ya no gobiernan nada en
// runtime. Se implementan porque son contrato —20 paquetes de `java.base` las nombran— no porque
// hagan cumplir algo. `checkGuard` lo dice explicitamente mas abajo.
public abstract class Permission implements Guard, Serializable {

    // El nombre del permiso. Su significado depende de la subclase: una ruta, una propiedad, un
    // host. Final: cambiarlo convertiria el permiso en otro.
    private final String name;

    // Un permiso con el nombre dado.
    public Permission(String name) {
        this.name = name;
    }

    // Vigila el acceso a `object`.
    //
    // A KajiLibrary subset, y coincide con lo que hace el JDK 24+: el `SecurityManager` esta
    // permanentemente deshabilitado, asi que no hay a quien preguntarle y esto no lanza nunca. Se
    // conserva porque es como `Permission` cumple `Guard`, que es lo que permite pasar un permiso
    // a un `GuardedObject`.
    public void checkGuard(Object object) throws SecurityException {
    }

    // Si este permiso implica al otro. Es la unica pregunta que un chequeo de acceso hace.
    public abstract boolean implies(Permission permission);

    // Abstracto a proposito, aunque `Object` ya lo tenga: dos permisos de la misma clase con el
    // mismo nombre y acciones **deben** ser iguales, porque si no una coleccion de permisos
    // guardaria duplicados que implican lo mismo. Obligar a la subclase a escribirlo es la forma
    // de que nadie herede el de `Object` por descuido.
    public abstract boolean equals(Object obj);

    // Igual que `equals`: abstracto para que sea coherente con el.
    public abstract int hashCode();

    // El nombre de este permiso.
    public final String getName() {
        return this.name;
    }

    // Las acciones, como cadena canonica; "" si esta clase no las usa.
    public abstract String getActions();

    // Una coleccion vacia adecuada para guardar permisos de esta clase, o null si sirve
    // cualquiera.
    //
    // Existe porque algunas clases pueden responder `implies` mucho mas rapido sobre un conjunto
    // que preguntandole a cada permiso de a uno. `null` significa "no tengo nada mejor", y el
    // llamador usa una coleccion generica.
    public PermissionCollection newPermissionCollection() {
        return null;
    }

    // `("clase" "nombre")`, o `("clase" "nombre" "acciones")` si tiene acciones.
    public String toString() {
        String actions = this.getActions();
        if (actions == null || actions.length() == 0) {
            return "(\"" + this.getClass().getName() + "\" \"" + this.name + "\")";
        }
        return "(\"" + this.getClass().getName() + "\" \"" + this.name + "\" \"" + actions + "\")";
    }
}
