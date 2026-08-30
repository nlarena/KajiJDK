package java.security;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.stream.Stream;

// Un conjunto homogeneo de permisos, con un `implies` propio.
//
// No es una `Collection` de `java.util`, y no es un descuido: lo que aporta es el `implies` sobre
// **el conjunto entero**, que una coleccion cualquiera no sabe hacer. Una implementacion puede
// contestarlo mucho mas rapido que preguntandole a cada permiso de a uno — indexando por nombre,
// por ejemplo — y ese es todo el motivo de que el tipo exista.
//
// El estado de solo lectura es de una sola via: una vez cerrada, no se reabre. Es lo que permite
// entregar una coleccion de permisos sin miedo a que el receptor se agregue mas.
public abstract class PermissionCollection implements Serializable {

    private volatile boolean readOnly;

    public PermissionCollection() {
    }

    // Agrega un permiso.
    public abstract void add(Permission permission);

    // Si los permisos de esta coleccion, tomados en conjunto, implican al dado.
    public abstract boolean implies(Permission permission);

    // Los permisos de esta coleccion.
    public abstract Enumeration<Permission> elements();

    // Los permisos, como stream.
    public Stream<Permission> elementsAsStream() {
        Enumeration<Permission> e = this.elements();
        java.util.ArrayList<Permission> lista = new java.util.ArrayList<Permission>();
        while (e.hasMoreElements()) {
            lista.add(e.nextElement());
        }
        Object[] a = new Object[lista.size()];
        int i = 0;
        while (i < lista.size()) {
            a[i] = lista.get(i);
            i = i + 1;
        }
        return (Stream<Permission>) Stream.of(a);
    }

    // Cierra la coleccion. No hay vuelta atras.
    public void setReadOnly() {
        this.readOnly = true;
    }

    // Si la coleccion esta cerrada.
    public boolean isReadOnly() {
        return this.readOnly;
    }

    // El nombre de la clase seguido de los permisos, uno por linea.
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(super.toString());
        b.append(" (\n");
        Enumeration<Permission> e = this.elements();
        while (e.hasMoreElements()) {
            b.append(" ");
            b.append(e.nextElement().toString());
            b.append("\n");
        }
        b.append(")\n");
        return b.toString();
    }
}
