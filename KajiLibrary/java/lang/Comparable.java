package java.lang;

// KajiLibrary's java.lang.Comparable — the natural-ordering contract. A type implements it
// to define how its instances compare (`compareTo` returns negative / zero / positive).
public interface Comparable<T> {

    int compareTo(T o);
}
