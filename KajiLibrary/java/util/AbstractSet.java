package java.util;

// The skeleton for sets. It adds no operations to {@link AbstractCollection} — a set differs
// from a collection by a *contract* (no duplicates), not by an interface — but it fixes the
// equality rule: two sets are equal when they hold the same elements, whatever their order or
// implementation, and a set hash is the sum of its element hashes so that rule survives it.
public abstract class AbstractSet<E> extends AbstractCollection<E> implements Set<E> {

    protected AbstractSet() {
    }

    public boolean equals(Object o) {
        boolean same;
        if (o == this) {
            same = true;
        } else if (!(o instanceof Set)) {
            same = false;
        } else {
            Collection<E> other = (Collection<E>) o;
            if (other.size() != size()) {
                same = false;
            } else {
                same = true;
                Iterator<E> it = other.iterator();
                while (it.hasNext()) {
                    if (!contains(it.next())) {
                        same = false;
                    }
                }
            }
        }
        return same;
    }

    // Order-independent by construction: addition commutes, so two equal sets agree.
    public int hashCode() {
        int h = 0;
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            Object e = it.next();
            if (e != null) {
                h = h + e.hashCode();
            }
        }
        return h;
    }
    // Quita de este conjunto todo lo que este en `c`.
    //
    // Se recorre **el mas chico de los dos**, que es la optimizacion que el JDK hace aca y que
    // cambia el orden de magnitud cuando uno es mucho menor: quitar diez elementos de un conjunto
    // de un millon no tiene por que costar un millon de consultas.
    public boolean removeAll(Collection<?> c) {
        boolean cambio = false;
        if (this.size() > c.size()) {
            Iterator<?> it = c.iterator();
            while (it.hasNext()) {
                if (this.remove(it.next())) {
                    cambio = true;
                }
            }
            return cambio;
        }
        Object[] foto = this.toArray();
        int i = 0;
        while (i < foto.length) {
            if (c.contains(foto[i])) {
                if (this.remove(foto[i])) {
                    cambio = true;
                }
            }
            i = i + 1;
        }
        return cambio;
    }

}
