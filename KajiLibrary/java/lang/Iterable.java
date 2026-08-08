package java.lang;

import java.util.Iterator;

// KajiLibrary's java.lang.Iterable — anything that can hand out an Iterator over its
// elements. Implementing it is what makes a type usable in a for-each loop.
public interface Iterable<T> {

    Iterator<T> iterator();
}
