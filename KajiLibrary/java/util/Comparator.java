package java.util;

// KajiLibrary's java.util.Comparator — an external ordering: `compare(a, b)` returns
// negative / zero / positive, letting a caller sort by a rule other than the type's own
// natural ordering. A functional interface.
public interface Comparator<T> {

    int compare(T o1, T o2);
}
