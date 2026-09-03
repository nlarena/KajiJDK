package java.lang.classfile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

// Un elemento que además es una secuencia de elementos más chicos: una clase, un campo, un método o
// un cuerpo de método. `forEach` es la operación primitiva —lo que sabe hacer un modelo es
// *recorrerse*— y todo lo demás sale de ahí.
public interface CompoundElement<E extends ClassFileElement> extends ClassFileElement, Iterable<E> {

    /** Le pasa cada pieza a `consumer`, en el orden del archivo. */
    void forEach(Consumer<? super E> consumer);

    /** Un iterador sobre las piezas. */
    default Iterator<E> iterator() {
        return elementList().iterator();
    }

    /** Las piezas como flujo. */
    default Stream<E> elementStream() {
        return elementList().stream();
    }

    /** Las piezas como lista. */
    default List<E> elementList() {
        Colector<E> c = new Colector<E>();
        forEach(c);
        return c.lista;
    }

    /** Una vista textual de las piezas, una por línea. Es para depurar; el formato no es estable. */
    default String toDebugString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.toString()).append('\n');
        List<E> piezas = elementList();
        for (int i = 0; i < piezas.size(); i++) {
            sb.append("  ").append(String.valueOf(piezas.get(i))).append('\n');
        }
        return sb.toString();
    }
}

// El acumulador de `elementList`. Es una clase con nombre y no una anónima porque el `default` de
// una interfaz es donde menos conviene depender de la captura de variables.
final class Colector<E> implements Consumer<E> {

    final List<E> lista = new ArrayList<E>();

    public void accept(E e) {
        this.lista.add(e);
    }
}
