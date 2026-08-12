package java.lang;

// java.lang.Comparable — natural ordering: `compareTo` returns <0, 0, >0. The default key for
// sorted/priority structures when no Comparator is supplied.
public interface Comparable<T> {
    int compareTo(T o);
}
