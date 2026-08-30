package java.util;

// The original iteration protocol, from Java 1.0, kept alive by the legacy classes that still
// hand one out (Vector, Hashtable). {@link Iterator} replaced it: same job, shorter names, and
// the ability to remove.
public interface Enumeration<E> {

    boolean hasMoreElements();

    E nextElement();

    /**
     * Esta misma enumeracion vista como {@link Iterator}.
     *
     * <p>Es el puente que faltaba del lado de la interfaz vieja: `Collections.enumeration` va de
     * Iterator a Enumeration, y este vuelve. Sin el, cualquier API anterior a 1.2 que devuelva una
     * Enumeration queda fuera del for-each y de los streams.
     *
     * <p>El Iterator que sale **no** soporta `remove()`: una Enumeration no tiene con que.
     */
    default Iterator<E> asIterator() {
        return new EnumerationItr<E>(this);
    }
}

// El adaptador de `asIterator`. Package-private, y top-level en vez de anidado por el miscompilado
// de una anidada dentro de una generica (#13).
final class EnumerationItr<E> implements Iterator<E> {

    private final Enumeration<E> e;

    EnumerationItr(Enumeration<E> e) {
        this.e = e;
    }

    public boolean hasNext() {
        return this.e.hasMoreElements();
    }

    public E next() {
        return this.e.nextElement();
    }
}
