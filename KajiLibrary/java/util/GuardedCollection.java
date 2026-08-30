package java.util;

// Los envoltorios que devuelven las tres familias de Collections: unmodifiableX, synchronizedX y
// checkedX. Package-private, porque el contrato solo promete la interfaz de vuelta.
//
// El JDK tiene una clase por familia y por interfaz -- UnmodifiableList, SynchronizedList,
// CheckedList, y asi por cada una de las ocho. Aca hay una sola familia con tres interruptores,
// porque las tres hacen exactamente lo mismo (delegar) y solo cambian en que hacen ANTES:
//
//   readOnly   los mutadores tiran UnsupportedOperationException en vez de delegar
//   type       add/set validan la clase del elemento y tiran ClassCastException en el acto
//   (cerrojo)  todo pasa por el monitor de `this`
//
// Sobre el cerrojo hay que ser claro: **todos** los envoltorios toman el monitor, no solo los de
// synchronizedX. Es un monitor que en los de solo lectura nadie mas mira, asi que el costo es un
// monitorenter sin contienda, y a cambio no hay que escribir cada metodo dos veces. Para el que
// SI sincroniza, el monitor es el envoltorio mismo, que es lo que documenta el JDK: quien
// necesite recorrerlo entero tiene que hacer `synchronized (lista) { ... }` por su cuenta,
// porque un iterador no se puede proteger desde adentro.
//
// Lo que checkedX aporta no es obvio hasta que se ve el agujero que tapa: con genericos borrados,
// un `List<String>` pasado como `List` cruda acepta un Integer sin chistar, y la ClassCastException
// aparece mucho despues, en el `get`, lejos de quien la causo. `checkedList` mueve el error al
// momento del `add`.
class GuardedCollection<E> implements Collection<E> {

    final Collection<E> back;

    // La clase que se exige a cada elemento que entra, o null si no se valida nada.
    final Class<E> type;

    final boolean readOnly;

    GuardedCollection(Collection<E> back, Class<E> type, boolean readOnly) {
        if (back == null) {
            throw new NullPointerException();
        }
        this.back = back;
        this.type = type;
        this.readOnly = readOnly;
    }

    // Corta cualquier mutacion si el envoltorio es de solo lectura.
    final void noWrite() {
        if (this.readOnly) {
            throw new UnsupportedOperationException();
        }
    }

    // Valida la clase del elemento que entra. Devuelve el mismo elemento para poder encadenar.
    final E check(E e) {
        if (this.type != null && e != null && !this.type.isInstance(e)) {
            throw new ClassCastException("Attempt to insert " + e.getClass().getName()
                    + " element into collection with element type " + this.type.getName());
        }
        return e;
    }

    // Valida una coleccion entera antes de insertarla, para no dejarla a medio agregar.
    final Collection<E> checkAll(Collection<? extends E> c) {
        Object[] a = c.toArray();
        int i = 0;
        while (i < a.length) {
            this.check((E) a[i]);
            i = i + 1;
        }
        return (Collection<E>) c;
    }

    public int size() {
        synchronized (this) {
            return this.back.size();
        }
    }

    public boolean isEmpty() {
        synchronized (this) {
            return this.back.isEmpty();
        }
    }

    public boolean contains(Object o) {
        synchronized (this) {
            return this.back.contains(o);
        }
    }

    public boolean containsAll(Collection<?> c) {
        synchronized (this) {
            return this.back.containsAll(c);
        }
    }

    public Object[] toArray() {
        synchronized (this) {
            return this.back.toArray();
        }
    }

    public <T> T[] toArray(T[] a) {
        synchronized (this) {
            return this.back.toArray(a);
        }
    }

    // El iterador se envuelve solo cuando hace falta: si el envoltorio es de solo lectura hay que
    // tapar `remove()`, que si no seria la puerta de atras para modificar. En los otros dos casos
    // se devuelve el de adentro tal cual -- envolverlo no aportaria nada, y para el sincronizado
    // seria enganoso: proteger cada llamada por separado no hace segura la recorrida completa.
    public Iterator<E> iterator() {
        synchronized (this) {
            if (this.readOnly) {
                return new GuardedItr<E>(this.back.iterator());
            }
            return this.back.iterator();
        }
    }

    public boolean add(E e) {
        this.noWrite();
        synchronized (this) {
            return this.back.add(this.check(e));
        }
    }

    public boolean remove(Object o) {
        this.noWrite();
        synchronized (this) {
            return this.back.remove(o);
        }
    }

    public boolean addAll(Collection<? extends E> c) {
        this.noWrite();
        synchronized (this) {
            return this.back.addAll(this.checkAll(c));
        }
    }

    public boolean removeAll(Collection<?> c) {
        this.noWrite();
        synchronized (this) {
            return this.back.removeAll(c);
        }
    }

    public boolean retainAll(Collection<?> c) {
        this.noWrite();
        synchronized (this) {
            return this.back.retainAll(c);
        }
    }

    public void clear() {
        this.noWrite();
        synchronized (this) {
            this.back.clear();
        }
    }

    // Los defaults de Collection que mutan tambien tienen que respetar el candado: `removeIf`
    // llega a `iterator().remove()` o a `remove(Object)`, y sin esto un envoltorio de solo
    // lectura fallaria con la excepcion equivocada -- o peor, borraria algo antes de fallar.
    public boolean removeIf(java.util.function.Predicate<? super E> filter) {
        this.noWrite();
        synchronized (this) {
            return this.back.removeIf(filter);
        }
    }

    public void forEach(java.util.function.Consumer<? super E> action) {
        synchronized (this) {
            this.back.forEach(action);
        }
    }

    public String toString() {
        synchronized (this) {
            return this.back.toString();
        }
    }
}

// La version con orden de encuentro: agrega los dos extremos.
class GuardedSequencedCollection<E> extends GuardedCollection<E> implements SequencedCollection<E> {

    GuardedSequencedCollection(SequencedCollection<E> back, Class<E> type, boolean readOnly) {
        super(back, type, readOnly);
    }

    final SequencedCollection<E> seq() {
        return (SequencedCollection<E>) this.back;
    }

    public SequencedCollection<E> reversed() {
        synchronized (this) {
            return new GuardedSequencedCollection<E>(this.seq().reversed(), this.type, this.readOnly);
        }
    }

    public void addFirst(E e) {
        this.noWrite();
        synchronized (this) {
            this.seq().addFirst(this.check(e));
        }
    }

    public void addLast(E e) {
        this.noWrite();
        synchronized (this) {
            this.seq().addLast(this.check(e));
        }
    }

    public E getFirst() {
        synchronized (this) {
            return this.seq().getFirst();
        }
    }

    public E getLast() {
        synchronized (this) {
            return this.seq().getLast();
        }
    }

    public E removeFirst() {
        this.noWrite();
        synchronized (this) {
            return this.seq().removeFirst();
        }
    }

    public E removeLast() {
        this.noWrite();
        synchronized (this) {
            return this.seq().removeLast();
        }
    }
}

// El envoltorio de List. `equals`/`hashCode` delegan porque List los define por contenido: una
// lista envuelta tiene que seguir siendo igual a la de adentro, o `unmodifiableList(x).equals(x)`
// daria false y ningun `assertEquals` pasaria.
class GuardedList<E> extends GuardedSequencedCollection<E> implements List<E> {

    GuardedList(List<E> back, Class<E> type, boolean readOnly) {
        super(back, type, readOnly);
    }

    final List<E> list() {
        return (List<E>) this.back;
    }

    public E get(int index) {
        synchronized (this) {
            return this.list().get(index);
        }
    }

    public E set(int index, E element) {
        this.noWrite();
        synchronized (this) {
            return this.list().set(index, this.check(element));
        }
    }

    public void add(int index, E element) {
        this.noWrite();
        synchronized (this) {
            this.list().add(index, this.check(element));
        }
    }

    public E remove(int index) {
        this.noWrite();
        synchronized (this) {
            return this.list().remove(index);
        }
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        this.noWrite();
        synchronized (this) {
            return this.list().addAll(index, this.checkAll(c));
        }
    }

    public int indexOf(Object o) {
        synchronized (this) {
            return this.list().indexOf(o);
        }
    }

    public int lastIndexOf(Object o) {
        synchronized (this) {
            return this.list().lastIndexOf(o);
        }
    }

    public ListIterator<E> listIterator() {
        return this.listIterator(0);
    }

    public ListIterator<E> listIterator(int index) {
        synchronized (this) {
            return new GuardedLitr<E>(this.list().listIterator(index), this.type, this.readOnly);
        }
    }

    // La sublista se envuelve con la misma guardia, si no seria el agujero por donde escribir en
    // una lista de solo lectura.
    public List<E> subList(int fromIndex, int toIndex) {
        synchronized (this) {
            return new GuardedList<E>(this.list().subList(fromIndex, toIndex), this.type, this.readOnly);
        }
    }

    public List<E> reversed() {
        synchronized (this) {
            return new GuardedList<E>(this.list().reversed(), this.type, this.readOnly);
        }
    }

    public void replaceAll(java.util.function.UnaryOperator<E> operator) {
        this.noWrite();
        synchronized (this) {
            this.list().replaceAll(operator);
        }
    }

    public void sort(Comparator<? super E> c) {
        this.noWrite();
        synchronized (this) {
            this.list().sort(c);
        }
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        synchronized (this) {
            return this.back.equals(o);
        }
    }

    public int hashCode() {
        synchronized (this) {
            return this.back.hashCode();
        }
    }
}

class GuardedSet<E> extends GuardedCollection<E> implements Set<E> {

    GuardedSet(Set<E> back, Class<E> type, boolean readOnly) {
        super(back, type, readOnly);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        synchronized (this) {
            return this.back.equals(o);
        }
    }

    public int hashCode() {
        synchronized (this) {
            return this.back.hashCode();
        }
    }
}

class GuardedSequencedSet<E> extends GuardedSequencedCollection<E> implements SequencedSet<E> {

    GuardedSequencedSet(SequencedSet<E> back, Class<E> type, boolean readOnly) {
        super(back, type, readOnly);
    }

    public SequencedSet<E> reversed() {
        synchronized (this) {
            SequencedSet<E> r = (SequencedSet<E>) this.seq().reversed();
            return new GuardedSequencedSet<E>(r, this.type, this.readOnly);
        }
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        synchronized (this) {
            return this.back.equals(o);
        }
    }

    public int hashCode() {
        synchronized (this) {
            return this.back.hashCode();
        }
    }
}

// Los tres cortes de un SortedSet vuelven envueltos con la misma guardia, por la misma razon que
// la sublista.
class GuardedSortedSet<E> extends GuardedSequencedSet<E> implements SortedSet<E> {

    GuardedSortedSet(SortedSet<E> back, Class<E> type, boolean readOnly) {
        super(back, type, readOnly);
    }

    final SortedSet<E> sorted() {
        return (SortedSet<E>) this.back;
    }

    public Comparator<? super E> comparator() {
        synchronized (this) {
            return this.sorted().comparator();
        }
    }

    public SortedSet<E> subSet(E from, E to) {
        synchronized (this) {
            return new GuardedSortedSet<E>(this.sorted().subSet(from, to), this.type, this.readOnly);
        }
    }

    public SortedSet<E> headSet(E to) {
        synchronized (this) {
            return new GuardedSortedSet<E>(this.sorted().headSet(to), this.type, this.readOnly);
        }
    }

    public SortedSet<E> tailSet(E from) {
        synchronized (this) {
            return new GuardedSortedSet<E>(this.sorted().tailSet(from), this.type, this.readOnly);
        }
    }

    public E first() {
        synchronized (this) {
            return this.sorted().first();
        }
    }

    public E last() {
        synchronized (this) {
            return this.sorted().last();
        }
    }
}

class GuardedNavigableSet<E> extends GuardedSortedSet<E> implements NavigableSet<E> {

    GuardedNavigableSet(NavigableSet<E> back, Class<E> type, boolean readOnly) {
        super(back, type, readOnly);
    }

    final NavigableSet<E> nav() {
        return (NavigableSet<E>) this.back;
    }

    public E lower(E e) {
        synchronized (this) {
            return this.nav().lower(e);
        }
    }

    public E floor(E e) {
        synchronized (this) {
            return this.nav().floor(e);
        }
    }

    public E ceiling(E e) {
        synchronized (this) {
            return this.nav().ceiling(e);
        }
    }

    public E higher(E e) {
        synchronized (this) {
            return this.nav().higher(e);
        }
    }

    public E pollFirst() {
        this.noWrite();
        synchronized (this) {
            return this.nav().pollFirst();
        }
    }

    public E pollLast() {
        this.noWrite();
        synchronized (this) {
            return this.nav().pollLast();
        }
    }

    public NavigableSet<E> descendingSet() {
        synchronized (this) {
            return new GuardedNavigableSet<E>(this.nav().descendingSet(), this.type, this.readOnly);
        }
    }

    public Iterator<E> descendingIterator() {
        synchronized (this) {
            if (this.readOnly) {
                return new GuardedItr<E>(this.nav().descendingIterator());
            }
            return this.nav().descendingIterator();
        }
    }

    public NavigableSet<E> subSet(E from, boolean fromInclusive, E to, boolean toInclusive) {
        synchronized (this) {
            NavigableSet<E> s = this.nav().subSet(from, fromInclusive, to, toInclusive);
            return new GuardedNavigableSet<E>(s, this.type, this.readOnly);
        }
    }

    public NavigableSet<E> headSet(E to, boolean inclusive) {
        synchronized (this) {
            return new GuardedNavigableSet<E>(this.nav().headSet(to, inclusive), this.type, this.readOnly);
        }
    }

    public NavigableSet<E> tailSet(E from, boolean inclusive) {
        synchronized (this) {
            return new GuardedNavigableSet<E>(this.nav().tailSet(from, inclusive), this.type, this.readOnly);
        }
    }
}

class GuardedQueue<E> extends GuardedCollection<E> implements Queue<E> {

    GuardedQueue(Queue<E> back, Class<E> type, boolean readOnly) {
        super(back, type, readOnly);
    }

    final Queue<E> queue() {
        return (Queue<E>) this.back;
    }

    public boolean offer(E e) {
        this.noWrite();
        synchronized (this) {
            return this.queue().offer(this.check(e));
        }
    }

    // `poll` saca, asi que cuenta como mutador aunque su nombre no lo diga.
    public E poll() {
        this.noWrite();
        synchronized (this) {
            return this.queue().poll();
        }
    }

    public E peek() {
        synchronized (this) {
            return this.queue().peek();
        }
    }
}

// El iterador de solo lectura: lo unico que cambia es que `remove()` se niega.
final class GuardedItr<E> implements Iterator<E> {

    private final Iterator<E> back;

    GuardedItr(Iterator<E> back) {
        this.back = back;
    }

    public boolean hasNext() {
        return this.back.hasNext();
    }

    public E next() {
        return this.back.next();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}

// El ListIterator envuelto. Ademas de tapar los mutadores cuando corresponde, valida el tipo en
// `set` y en `add`: son la otra via de entrada a una lista, y checkedList no serviria de mucho si
// se pudiera esquivar pasando por el iterador.
final class GuardedLitr<E> implements ListIterator<E> {

    private final ListIterator<E> back;
    private final Class<E> type;
    private final boolean readOnly;

    GuardedLitr(ListIterator<E> back, Class<E> type, boolean readOnly) {
        this.back = back;
        this.type = type;
        this.readOnly = readOnly;
    }

    private E check(E e) {
        if (this.readOnly) {
            throw new UnsupportedOperationException();
        }
        if (this.type != null && e != null && !this.type.isInstance(e)) {
            throw new ClassCastException("Attempt to insert " + e.getClass().getName()
                    + " element into collection with element type " + this.type.getName());
        }
        return e;
    }

    public boolean hasNext() {
        return this.back.hasNext();
    }

    public E next() {
        return this.back.next();
    }

    public boolean hasPrevious() {
        return this.back.hasPrevious();
    }

    public E previous() {
        return this.back.previous();
    }

    public int nextIndex() {
        return this.back.nextIndex();
    }

    public int previousIndex() {
        return this.back.previousIndex();
    }

    public void remove() {
        if (this.readOnly) {
            throw new UnsupportedOperationException();
        }
        this.back.remove();
    }

    public void set(E e) {
        this.back.set(this.check(e));
    }

    public void add(E e) {
        this.back.add(this.check(e));
    }
}
