package java.util;

// Same-package import is a workaround for the frozen javac's finder, which does not
// auto-load an unqualified same-package type that lives only on the classpath (finding #4).
import java.util.Collection;

// KajiLibrary's java.util.List<E> — an ordered Collection addressable by integer index:
// get/set/insert/remove at a position, and search by value. A KajiLibrary subset (the JDK
// adds listIterator/subList/replaceAll/sort/…).
public interface List<E> extends Collection<E> {

    E get(int index);

    E set(int index, E element);

    void add(int index, E element);

    E remove(int index);

    int indexOf(Object o);
}
