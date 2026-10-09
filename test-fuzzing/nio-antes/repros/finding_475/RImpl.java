import java.util.Collection;
import java.util.Iterator;
public class RImpl implements R {
    public int size() { return 0; }
    public boolean isEmpty() { return true; }
    public boolean contains(Object o) { return false; }
    public Iterator iterator() { return null; }
    public Object[] toArray() { return new Object[0]; }
    public Object[] toArray(Object[] a) { return a; }
    public boolean add(Object o) { return false; }
    public boolean remove(Object o) { return false; }
    public boolean containsAll(Collection c) { return false; }
    public boolean addAll(Collection c) { return false; }
    public boolean removeAll(Collection c) { return false; }
    public boolean retainAll(Collection c) { return false; }
    public void clear() { }
}
