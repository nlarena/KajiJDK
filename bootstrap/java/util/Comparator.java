package java.util;

// java.util.Comparator — an external ordering over T (so T need not be Comparable itself).
public interface Comparator<T> {
    int compare(T a, T b);
}
