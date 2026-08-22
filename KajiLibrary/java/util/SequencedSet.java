package java.util;

// A set that also has an encounter order — LinkedHashSet, or a sorted set. Nothing new beyond
// narrowing reversed() to return a set again.
public interface SequencedSet<E> extends SequencedCollection<E>, Set<E> {

    SequencedSet<E> reversed();
}
