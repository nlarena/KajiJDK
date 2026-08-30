package java.util;

// El ListIterator que devuelve todo AbstractList. Package-private, como AbstractListItr.
//
// Hasta ahora `ListIterator` era una interfaz **declarada y sin ningun implementor** en toda la
// biblioteca: existia el tipo y no habia nada que devolver, asi que `listIterator()` no se podia
// escribir en ninguna lista. Esta es esa implementacion.
//
// El cursor va **entre** elementos, que es lo que distingue a un ListIterator de un Iterator:
// `nextIndex()` es el hueco donde esta parado, `previous()` retrocede sobre lo ya recorrido, y
// `add` inserta en ese hueco. `ultimo` recuerda el indice del elemento que devolvio la ultima
// llamada a next()/previous(), porque `set` y `remove` operan sobre **ese**, no sobre el cursor.
final class AbstractListLitr<E> implements ListIterator<E> {

    private final List<E> list;

    // El hueco donde esta parado: 0 es antes del primero, size() es despues del ultimo.
    private int cursor;

    // El indice del ultimo elemento devuelto, o -1 si no hubo next()/previous() desde la ultima
    // modificacion. Es lo que hace que `set` y `remove` sepan sobre que operar.
    private int ultimo;

    AbstractListLitr(List<E> list, int index) {
        if (index < 0 || index > list.size()) {
            throw new IndexOutOfBoundsException();
        }
        this.list = list;
        this.cursor = index;
        this.ultimo = -1;
    }

    public boolean hasNext() {
        return this.cursor < this.list.size();
    }

    public E next() {
        if (this.cursor >= this.list.size()) {
            throw new NoSuchElementException();
        }
        E e = this.list.get(this.cursor);
        this.ultimo = this.cursor;
        this.cursor = this.cursor + 1;
        return e;
    }

    public boolean hasPrevious() {
        return this.cursor > 0;
    }

    public E previous() {
        if (this.cursor <= 0) {
            throw new NoSuchElementException();
        }
        this.cursor = this.cursor - 1;
        this.ultimo = this.cursor;
        return this.list.get(this.cursor);
    }

    public int nextIndex() {
        return this.cursor;
    }

    public int previousIndex() {
        return this.cursor - 1;
    }

    // Quita el ultimo devuelto. Si venia de next(), el cursor retrocede uno: lo que quedaba
    // adelante corrio un lugar hacia atras y no hay que saltearselo.
    public void remove() {
        if (this.ultimo < 0) {
            throw new IllegalStateException();
        }
        this.list.remove(this.ultimo);
        if (this.ultimo < this.cursor) {
            this.cursor = this.cursor - 1;
        }
        this.ultimo = -1;
    }

    // Reemplaza el ultimo devuelto. No mueve el cursor ni invalida `ultimo`: cambiar el valor de
    // una posicion no cambia por donde va el recorrido.
    public void set(E e) {
        if (this.ultimo < 0) {
            throw new IllegalStateException();
        }
        this.list.set(this.ultimo, e);
    }

    // Inserta en el hueco actual. El nuevo queda **detras** del cursor, asi que `next()` sigue
    // devolviendo lo que iba a devolver; y `ultimo` se invalida, porque despues de un add no hay
    // "ultimo devuelto" sobre el que valga operar.
    public void add(E e) {
        this.list.add(this.cursor, e);
        this.cursor = this.cursor + 1;
        this.ultimo = -1;
    }
}
