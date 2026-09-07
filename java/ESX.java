import java.io.Serializable;
import java.util.EnumSet;
public class ESX {
    enum E { A, B, C }
    /**
     * Que el conjunto que devuelve {@code EnumSet} sea serializable.
     *
     * <p>La clase concreta es privada del paquete, asi que no se la puede nombrar desde aca: la
     * unica forma de preguntarle es a traves de la instancia, que es ademas como se ve desde
     * cualquier codigo real.
     *
     * @return 3 si las tres comprobaciones dan bien
     */
    public static int run() {
        int n = 0;
        Object s = EnumSet.noneOf(E.class);
        if (s instanceof Serializable) { n++; }
        if (Serializable.class.isAssignableFrom(s.getClass())) { n++; }
        if (Serializable.class.isAssignableFrom(EnumSet.class)) { n++; }
        return n;
    }
    public static void main(String[] a) { System.out.println(run() + " " + EnumSet.noneOf(E.class).getClass().getName()); }
}
