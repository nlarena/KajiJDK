// La mitad que SI compila del repro de OvLib3.java: mismas dos sobrecargas, declaradas al reves
// (`of(T)` primero). Con este orden `OvLib5.of(unArregloDeString)` liga bien a la variarg.
import java.util.ArrayList;
public interface OvLib5<T> {
    static <T> ArrayList<T> of(T v) { ArrayList<T> a = new ArrayList<T>(); a.add(v); return a; }
    static <T> ArrayList<T> of(T... vs) {
        ArrayList<T> a = new ArrayList<T>();
        for (int i = 0; i < vs.length; i++) { a.add(vs[i]); }
        return a;
    }
}
