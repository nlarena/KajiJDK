package java.util;

import java.lang.reflect.Array;

// The skeleton every collection is built on: give it `iterator()` and `size()` and it derives
// the rest. That is the whole idea of the abstract-skeleton classes — the JDK ships one per
// collection shape so an implementor writes the two or three genuinely new methods and
// inherits a dozen.
//
// Note on the mutators: the JDK implements `remove(Object)` and `clear()` by walking the
// iterator and calling `Iterator.remove()`. KajiLibrary's `Iterator` is the two-method subset
// (hasNext/next) with no `remove`, so those two refuse here instead of being derived — the
// honest consequence of the smaller interface, and the reason a concrete class still overrides
// them.
public abstract class AbstractCollection<E> implements Collection<E> {

    protected AbstractCollection() {
    }

    public abstract Iterator<E> iterator();

    public abstract int size();

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean contains(Object o) {
        boolean found = false;
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            Object e = it.next();
            if (o == null) {
                if (e == null) {
                    found = true;
                }
            } else if (o.equals(e)) {
                found = true;
            }
        }
        return found;
    }

    // Unsupported unless a subclass overrides it — a read-only collection is a valid one.
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    // "[a, b, c]" — the shape every collection prints in, derived once here.
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('[');
        Iterator<E> it = iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (!first) {
                b.append(',');
                b.append(' ');
            }
            first = false;
            Object e = it.next();
            if (e == null) {
                b.append("null");
            } else {
                b.append(e.toString());
            }
        }
        b.append(']');
        return b.toString();
    }
    // ---- las operaciones en bloque -----------------------------------------------------------
    //
    // Todas trabajan sobre una **foto** (`toArray()`) y no sobre el iterador vivo. El JDK las
    // escribe con `Iterator.remove()`, y ese camino esta cerrado aca: nuestro `Iterator.remove()`
    // es el `default` que lanza, y ningun iterador concreto de la biblioteca lo implementa. Sacar
    // la foto primero cuesta una pasada mas y es correcto con cualquier iterador.

    // Todos los elementos de `c` estan en esta coleccion.
    public boolean containsAll(Collection<?> c) {
        Iterator<?> it = c.iterator();
        while (it.hasNext()) {
            if (!this.contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    // Agrega todos los de `c`; devuelve si esta coleccion cambio.
    public boolean addAll(Collection<? extends E> c) {
        boolean cambio = false;
        Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            if (this.add(it.next())) {
                cambio = true;
            }
        }
        return cambio;
    }

    // Quita **todas** las apariciones de cada elemento de `c`.
    //
    // El bucle interno no es de mas: `remove(Object)` saca una sola aparicion, y una lista puede
    // tener varias del mismo elemento. Sin el, `removeAll` dejaria duplicados atras.
    public boolean removeAll(Collection<?> c) {
        boolean cambio = false;
        Object[] foto = this.toArray();
        int i = 0;
        while (i < foto.length) {
            if (c.contains(foto[i])) {
                while (this.remove(foto[i])) {
                    cambio = true;
                }
            }
            i = i + 1;
        }
        return cambio;
    }

    // Deja solo los elementos que tambien estan en `c`.
    public boolean retainAll(Collection<?> c) {
        boolean cambio = false;
        Object[] foto = this.toArray();
        int i = 0;
        while (i < foto.length) {
            if (!c.contains(foto[i])) {
                while (this.remove(foto[i])) {
                    cambio = true;
                }
            }
            i = i + 1;
        }
        return cambio;
    }

    // Los elementos en un arreglo nuevo, en el orden del iterador.
    public Object[] toArray() {
        Object[] out = new Object[this.size()];
        int i = 0;
        Iterator<E> it = this.iterator();
        while (it.hasNext() && i < out.length) {
            out[i] = it.next();
            i = i + 1;
        }
        return out;
    }

    // Los elementos en `a` si entran, o en un arreglo nuevo **del mismo tipo dinamico** si no.
    //
    // Ese "del mismo tipo dinamico" es el motivo de que exista la sobrecarga: el llamador pasa un
    // `String[0]` justamente para recibir un `String[]` y no un `Object[]`. Hace falta reflexion
    // para crearlo, porque el tipo del arreglo solo se conoce en runtime.
    //
    // Si `a` sobra lugar, la posicion siguiente al ultimo elemento queda en null: es como el
    // llamador sabe donde termina lo copiado cuando reusa un arreglo mas grande.
    public <T> T[] toArray(T[] a) {
        int n = this.size();
        Object[] dest = a;
        if (a.length < n) {
            dest = (Object[]) Array.newInstance(a.getClass().getComponentType(), n);
        }
        int i = 0;
        Iterator<E> it = this.iterator();
        while (it.hasNext() && i < n) {
            dest[i] = it.next();
            i = i + 1;
        }
        if (dest.length > n) {
            dest[n] = null;
        }
        return (T[]) dest;
    }

}
