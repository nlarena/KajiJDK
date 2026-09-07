import java.util.ArrayList;
import java.util.LinkedList;
import java.util.RandomAccess;
import java.util.Vector;
public class CLO2 {
    /**
     * Que {@code RandomAccess} este donde el JDK lo pone, y solo ahi.
     *
     * <p>No es decorativo: {@code Collections.binarySearch} y {@code Collections.shuffle} preguntan
     * por el para elegir entre recorrer por indice o por iterador. Con una lista enlazada marcada
     * como de acceso directo, una busqueda binaria pasa de logaritmica a cuadratica sin dar ningun
     * error -- anda, y tarda.
     *
     * @return cuantas fallan; 0 es lo correcto
     */
    public static int run() {
        int n = 0;
        if (!(new ArrayList<String>() instanceof RandomAccess)) { n++; }
        if (!(new Vector<String>() instanceof RandomAccess)) { n++; }
        if (!(new java.util.Stack<String>() instanceof RandomAccess)) { n++; }
        // Y al reves: la enlazada NO tiene que tenerlo.
        if (new LinkedList<String>() instanceof RandomAccess) { n++; }
        return n;
    }
    public static void main(String[] a) { System.out.println(run()); }
}
