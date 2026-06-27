import java.util.*;
public interface SigIf<T extends Number> extends Comparable<T>, Iterable<T> {
    T get(int i);
}
