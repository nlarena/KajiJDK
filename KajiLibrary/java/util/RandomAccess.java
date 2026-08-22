package java.util;

// A marker a List implements to say its `get(int)` is fast — constant-time, as in an array —
// so an algorithm can choose between indexing it and walking its iterator. ArrayList carries
// it; LinkedList deliberately does not.
public interface RandomAccess {
}
