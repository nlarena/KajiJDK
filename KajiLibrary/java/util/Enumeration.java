package java.util;

// The original iteration protocol, from Java 1.0, kept alive by the legacy classes that still
// hand one out (Vector, Hashtable). {@link Iterator} replaced it: same job, shorter names, and
// the ability to remove.
public interface Enumeration<E> {

    boolean hasMoreElements();

    E nextElement();
}
