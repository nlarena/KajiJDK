package java.util;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.Collection;

// KajiLibrary's java.util.Queue<E> — a Collection ordered for processing, typically FIFO.
// `offer` enqueues (returning false if capacity-bounded and full), `poll` dequeues the
// head (null if empty), `peek` looks at the head without removing (null if empty). A
// KajiLibrary subset (the JDK also has the throwing variants add/remove/element).
public interface Queue<E> extends Collection<E> {

    boolean offer(E e);

    E poll();

    E peek();

    /**
     * Saca la cabeza, o **lanza** si la cola esta vacia.
     *
     * <p>Es el par de `poll()`, y la diferencia es toda la razon de que existan los dos: `poll`
     * devuelve null porque el vacio es un resultado esperado --se lo usa en un bucle que consume
     * hasta agotar--, y `remove` lanza porque el vacio ahi es un error --se lo usa cuando el que
     * llama ya sabe que hay algo--. Elegir el equivocado convierte un bug en un `null` que viaja.
     */
    default E remove() {
        E e = this.poll();
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }

    // El par de `peek()`, con la misma distincion.
    default E element() {
        E e = this.peek();
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }
}
