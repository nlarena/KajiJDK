package java.util;

// The skeleton for lists backed by an index: give it `get(int)` and `size()` and it derives
// iteration and search. Its counterpart {@link AbstractSequentialList} does the same for lists
// backed by links, deriving indexed access from an iterator instead — the two skeletons exist
// precisely because a list can be efficient at one or the other, rarely both.
public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {

    protected AbstractList() {
    }

    public abstract E get(int index);

    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    // Appending is inserting at the end — so a subclass that implements add(int, E) gets this.
    public boolean add(E e) {
        add(size(), e);
        return true;
    }

    public int indexOf(Object o) {
        int found = -1;
        int n = size();
        for (int i = 0; i < n; i++) {
            if (found < 0) {
                Object e = get(i);
                if (o == null) {
                    if (e == null) {
                        found = i;
                    }
                } else if (o.equals(e)) {
                    found = i;
                }
            }
        }
        return found;
    }

    // Walks by index, which is exactly what "backed by an index" buys.
    public Iterator<E> iterator() {
        return new AbstractListItr<E>(this);
    }

    // ---- lo que List agrega sobre Collection, derivado del indice ---------------------------

    /**
     * Cuantas veces se modifico estructuralmente esta lista.
     *
     * <p>Lo lleva el JDK para que un iterador pueda detectar que la lista cambio debajo suyo y
     * tirar ConcurrentModificationException. Aca se declara porque es API protegida —una subclase
     * de otro paquete puede leerlo— pero **todavia no lo consulta nadie**: los iteradores de la
     * biblioteca no detectan modificacion concurrente. Queda dicho para que nadie lo suponga.
     */
    protected transient int modCount = 0;

    // El indice de la ULTIMA aparicion de `o`, o -1. Se recorre desde el final, que es lo que lo
    // distingue de indexOf: la primera coincidencia yendo hacia atras es la ultima yendo hacia
    // adelante, y asi se corta antes en el caso tipico.
    public int lastIndexOf(Object o) {
        int i = this.size() - 1;
        while (i >= 0) {
            E e = this.get(i);
            if (o == null) {
                if (e == null) {
                    return i;
                }
            } else if (o.equals(e)) {
                return i;
            }
            i = i - 1;
        }
        return -1;
    }

    // Un cursor bidireccional desde el principio.
    public ListIterator<E> listIterator() {
        return new AbstractListLitr<E>(this, 0);
    }

    // Un cursor bidireccional desde `index`.
    public ListIterator<E> listIterator(int index) {
        return new AbstractListLitr<E>(this, index);
    }

    // Una **vista** de [fromIndex, toIndex): escribir en ella escribe en esta lista.
    public List<E> subList(int fromIndex, int toIndex) {
        return new SubList<E>(this, fromIndex, toIndex);
    }

    // Inserta todos los de `c` a partir de `index`, en el orden de su iterador.
    public boolean addAll(int index, Collection<? extends E> c) {
        if (index < 0 || index > this.size()) {
            throw new IndexOutOfBoundsException();
        }
        boolean cambio = false;
        int at = index;
        Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            this.add(at, it.next());
            at = at + 1;
            cambio = true;
        }
        return cambio;
    }

    /**
     * Borra [fromIndex, toIndex).
     *
     * <p>Protegido y no publico a proposito, igual que en el JDK: es el gancho que una subclase
     * usa para dar una implementacion barata del borrado por rango — `SubList.clear()` pasa por
     * aca — sin ofrecerselo a un llamador cualquiera, que tiene `subList(a, b).clear()`.
     */
    protected void removeRange(int fromIndex, int toIndex) {
        int i = toIndex;
        while (i > fromIndex) {
            this.remove(i - 1);
            i = i - 1;
        }
    }

    /**
     * Igualdad por contenido: dos listas son iguales si tienen los mismos elementos en el mismo
     * orden, sin importar de que clase sean.
     *
     * <p>Faltaba, y es de las ausencias que no se ven midiendo firmas: `equals` y `hashCode`
     * figuran como heredados de Object, asi que ninguna cuenta de miembros los marca. Lo que se
     * heredaba de Object es la igualdad por **identidad**, y con eso
     * `new ArrayList(...).equals(new ArrayList(...))` daba false con el mismo contenido, ningun
     * `List` servia de clave de un mapa, y `List.of("x").equals(List.of("x"))` tambien era false.
     *
     * <p>Va aca y no en cada lista porque la especificacion lo exige simetrico entre
     * implementaciones distintas: un ArrayList tiene que ser igual a un LinkedList con los mismos
     * elementos. Un `equals` por clase concreta romperia justamente eso.
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof List)) {
            return false;
        }
        List<?> other = (List<?>) o;
        Iterator<E> a = this.iterator();
        Iterator<?> b = other.iterator();
        while (a.hasNext() && b.hasNext()) {
            if (!Objects.equals(a.next(), b.next())) {
                return false;
            }
        }
        // Se comparan los dos iteradores en vez de los dos size(): asi la igualdad no depende de
        // que size() sea barato, y no se recorre nada de mas cuando difieren en el primer
        // elemento.
        return !a.hasNext() && !b.hasNext();
    }

    /**
     * El hash que exige el contrato de List: 31 por el acumulado mas el hash del elemento, en
     * orden. La formula esta especificada al detalle, y no es negociable -- dos listas iguales de
     * clases distintas tienen que dar el mismo numero, y eso solo se logra fijando la cuenta.
     */
    public int hashCode() {
        int h = 1;
        Iterator<E> it = this.iterator();
        while (it.hasNext()) {
            E e = it.next();
            h = 31 * h + (e == null ? 0 : e.hashCode());
        }
        return h;
    }
}

// The index-walking iterator every AbstractList hands out.
final class AbstractListItr<E> implements Iterator<E> {

    private final AbstractList<E> list;
    private int cursor;

    AbstractListItr(AbstractList<E> list) {
        this.list = list;
    }

    public boolean hasNext() {
        return cursor < list.size();
    }

    public E next() {
        if (cursor >= list.size()) {
            throw new NoSuchElementException();
        }
        E e = list.get(cursor);
        cursor++;
        return e;
    }
}
