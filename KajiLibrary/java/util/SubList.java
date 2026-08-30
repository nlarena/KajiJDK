package java.util;

// La vista que devuelve List.subList(from, to). Package-private: el contrato solo promete una
// List de vuelta.
//
// Es una **vista**, no una copia, y ahi esta todo el punto: escribir en la sublista escribe en la
// original, y `list.subList(a, b).clear()` es la forma idiomatica de borrar un rango. Una copia
// haria que esa linea no borrara nada, en silencio.
//
// El tamano propio (`length`) se ajusta con cada insercion o borrado por la vista. Lo que **no**
// se detecta es una modificacion hecha directamente sobre la lista de atras mientras la vista
// existe: el JDK la caza con `modCount` y tira ConcurrentModificationException. Aca no, y queda
// dicho: usar la vista despues de tocar la original por afuera da resultados sin sentido en vez
// de una excepcion.
final class SubList<E> extends AbstractList<E> {

    private final List<E> base;
    private final int offset;
    private int length;

    SubList(List<E> base, int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > base.size() || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException();
        }
        this.base = base;
        this.offset = fromIndex;
        this.length = toIndex - fromIndex;
    }

    private void checkIndex(int index, int limite) {
        if (index < 0 || index >= limite) {
            throw new IndexOutOfBoundsException();
        }
    }

    public E get(int index) {
        this.checkIndex(index, this.length);
        return this.base.get(this.offset + index);
    }

    public int size() {
        return this.length;
    }

    public E set(int index, E element) {
        this.checkIndex(index, this.length);
        return this.base.set(this.offset + index, element);
    }

    public void add(int index, E element) {
        if (index < 0 || index > this.length) {
            throw new IndexOutOfBoundsException();
        }
        this.base.add(this.offset + index, element);
        this.length = this.length + 1;
    }

    public E remove(int index) {
        this.checkIndex(index, this.length);
        E viejo = this.base.remove(this.offset + index);
        this.length = this.length - 1;
        return viejo;
    }

    public boolean add(E e) {
        this.add(this.length, e);
        return true;
    }

    public void clear() {
        int i = this.length;
        while (i > 0) {
            this.base.remove(this.offset + i - 1);
            i = i - 1;
        }
        this.length = 0;
    }
}
