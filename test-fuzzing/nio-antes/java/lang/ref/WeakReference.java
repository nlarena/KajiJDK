package java.lang.ref;

// Same-package imports are redundant in Java, but for now the project javac's class-finder does not
// auto-load an **unqualified** reference to a class that only lives on the classpath (not in the
// source unit nor in the `JAVA_LANG` list). Workaround until that is fixed.
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
