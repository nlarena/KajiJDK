import java.util.ArrayList;
import java.util.HashMap;
public class CLO1 {
    /**
     * Que {@code clone()} funcione en las colecciones que lo declaran.
     *
     * <p>{@code Cloneable} no aporta ningun metodo: lo unico que hace es que
     * {@code Object.clone()} no tire {@code CloneNotSupportedException}. Por eso una clase que tiene
     * su {@code clone()} escrito pero no declara la interfaz falla recien al ejecutarlo, y con una
     * excepcion que no menciona la causa.
     *
     * @return cuantas fallan; 0 es lo correcto
     */
    public static int run() {
        int n = 0;
        try {
            ArrayList<String> a = new ArrayList<String>();
            a.add("x");
            Object c = a.clone();
            if (!(c instanceof ArrayList) || ((ArrayList<?>) c).size() != 1) { n++; }
        } catch (Throwable e) { n++; }
        try {
            HashMap<String, String> m = new HashMap<String, String>();
            m.put("k", "v");
            Object c = m.clone();
            if (!(c instanceof HashMap) || ((HashMap<?, ?>) c).size() != 1) { n++; }
        } catch (Throwable e) { n++; }
        if (!Cloneable.class.isAssignableFrom(ArrayList.class)) { n++; }
        if (!Cloneable.class.isAssignableFrom(HashMap.class)) { n++; }
        return n;
    }
    public static void main(String[] a) { System.out.println(run()); }
}
