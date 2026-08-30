package java.lang;

// Por import y nombre simple: calificar el tipo en el uso no resuelve
// desde java.lang (finding #210).
import java.util.Spliterator;
import java.util.Spliterators;

import java.util.Iterator;

// KajiLibrary's java.lang.Iterable — anything that can hand out an Iterator over its
// elements. Implementing it is what makes a type usable in a for-each loop.
public interface Iterable<T> {

    Iterator<T> iterator();

    /**
     * Corre `action` sobre cada elemento, en el orden en que los da el iterador.
     *
     * <p>Faltaba, y era el unico miembro publico de Iterable que no estaba: sin el, `forEach` no
     * existia en NINGUNA coleccion de la biblioteca -- lo hereda todo lo que sea Iterable, que es
     * la mitad de java.util. LinkedBlockingDeque y LinkedTransferQueue lo declaraban por su
     * cuenta, y no estaban sobreescribiendo nada.
     *
     * <p>Va como default y no como abstracto por la razon de siempre: declararlo abstracto
     * obligaria a escribirlo en cada uno de los implementores, y el cuerpo seria este mismo.
     */
    default void forEach(java.util.function.Consumer<? super T> action) {
        Iterator<T> it = this.iterator();
        while (it.hasNext()) {
            action.accept(it.next());
        }
    }

    /**
     * A spliterator over these elements.
     *
     * <p>Sin tamano y sin orden: un `Iterable` no promete ninguna de las dos cosas, y este default
     * existe para que toda coleccion tenga un spliterator aunque no sepa nada de si misma.
     */
    default Spliterator<T> spliterator() {
        return Spliterators.spliteratorUnknownSize(this.iterator(), 0);
    }

}
