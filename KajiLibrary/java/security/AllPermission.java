package java.security;

import java.util.Enumeration;
import java.util.NoSuchElementException;

// El permiso que implica a todos los demas.
//
// Es el equivalente de root, y su `implies` devuelve `true` sin mirar el argumento. Vale la pena
// tenerlo presente al leer una politica: un solo `AllPermission` vuelve irrelevante todo lo que
// haya alrededor.
public final class AllPermission extends Permission {

    // El permiso universal.
    public AllPermission() {
        super("<all permissions>");
    }

    // Igual que el sin argumentos; los dos parametros se ignoran. Existe para que el cargador de
    // politicas pueda construirlo por reflexion con la misma firma que cualquier otro permiso.
    public AllPermission(String name, String actions) {
        this();
    }

    // Siempre true. Ese es el punto.
    public boolean implies(Permission p) {
        return true;
    }

    // Todos los AllPermission son iguales entre si: no tienen estado que los distinga.
    public boolean equals(Object obj) {
        return obj instanceof AllPermission;
    }

    public int hashCode() {
        return 1;
    }

    // "<all actions>", que es lo que devuelve el JDK.
    public String getActions() {
        return "<all actions>";
    }

    public PermissionCollection newPermissionCollection() {
        return new AllPermissionCollection();
    }
}

// La coleccion de AllPermission. Guarda solo si hay alguno: uno o mil dan lo mismo.
final class AllPermissionCollection extends PermissionCollection {

    private boolean tieneAlguno;

    public void add(Permission permission) {
        if (!(permission instanceof AllPermission)) {
            throw new IllegalArgumentException("invalid permission: " + permission);
        }
        if (this.isReadOnly()) {
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");
        }
        this.tieneAlguno = true;
    }

    public boolean implies(Permission permission) {
        return this.tieneAlguno;
    }

    public Enumeration<Permission> elements() {
        return new AllPermEnum(this.tieneAlguno);
    }
}

// Enumeracion de cero o un AllPermission.
final class AllPermEnum implements Enumeration<Permission> {

    private boolean pendiente;

    AllPermEnum(boolean pendiente) {
        this.pendiente = pendiente;
    }

    public boolean hasMoreElements() {
        return this.pendiente;
    }

    public Permission nextElement() {
        if (!this.pendiente) {
            throw new NoSuchElementException();
        }
        this.pendiente = false;
        return new AllPermission();
    }
}
