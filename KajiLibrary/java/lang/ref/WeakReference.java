package java.lang.ref;

// Los imports del mismo paquete son redundantes en Java, pero por ahora el class-finder del
// javac propio no auto-carga una referencia **sin cualificar** de una clase que solo está en el
// classpath (no en la unidad de fuente ni en la lista `JAVA_LANG`). Workaround hasta que se arregle.
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;

// KajiLibrary's java.lang.ref.WeakReference — a reference the GC clears as soon as its
// referent is no longer strongly reachable. The canonical "cache that doesn't keep its
// entries alive" reference.
public class WeakReference<T> extends Reference<T> {

    public WeakReference(T referent) {
        super(referent, null);
    }

    public WeakReference(T referent, ReferenceQueue<? super T> queue) {
        super(referent, queue);
    }
}
