import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// Comportamiento de java.util.Arrays: orden, busqueda, copia, comparacion y las variantes hondas.
public class ArrP2 {

    public static int run() {
        int r = 0;
        // ---- equals / compare / mismatch --------------------------------------------------------
        // Los arreglos van en locales tipados: con la sobrecarga generica `T[]` en juego, pasar
        // un `new int[]{...}` en linea hace que la resolucion se declare ambigua (familia de #279).
        int[] p12 = { 1, 2 };
        int[] q12 = { 1, 2 };
        int[] p13 = { 1, 3 };
        int[] p123 = { 1, 2, 3 };
        int[] p129 = { 1, 2, 9 };
        r = r + (Arrays.equals(p12, q12) ? 1 : 0);
        r = r + (Arrays.equals(p12, p13) ? 7777 : 0);
        // lexicografico: el mas corto gana si es prefijo
        // Forma de rango: `Arrays.compare(int[], int[])` se declara ambigua (#290).
        r = r + (Arrays.compare(p12, 0, 2, p123, 0, 3) < 0 ? 10 : 0);
        r = r + (Arrays.compare(p13, 0, 2, p129, 0, 3) > 0 ? 100 : 0);
        r = r + Arrays.mismatch(p129, p123) * 1000;   // 2 -> 2000
        r = r + (Arrays.mismatch(p12, q12) == -1 ? 10000 : 0);
        // sin signo: 0xFF vale 255, no -1
        byte[] u1 = { -1 };
        byte[] u2 = { 1 };
        r = r + (Arrays.compare(u1, 0, 1, u2, 0, 1) < 0 ? 100000 : 0);
        r = r + (Arrays.compareUnsigned(u1, u2) > 0 ? 1000000 : 0);

        // ---- fill -------------------------------------------------------------------------------
        int[] relleno = new int[5];
        Arrays.fill(relleno, 1, 4, 7);
        r = r + (Arrays.toString(relleno).equals("[0, 7, 7, 7, 0]") ? 10000000 : 0);

        // ---- hashCode / asList ------------------------------------------------------------------
        r = r + (Arrays.hashCode(p12) == Arrays.hashCode(q12) ? 1 : 0);
        List<String> lista = Arrays.asList("x", "y", "z");
        r = r + lista.size() * 10;                                     // 30
        r = r + (lista.get(1).equals("y") ? 100 : 0);

        // ---- setAll / parallelPrefix ------------------------------------------------------------
        int[] gen = new int[4];
        Arrays.setAll(gen, new Doble());
        r = r + (Arrays.toString(gen).equals("[0, 2, 4, 6]") ? 1000 : 0);
        int[] pref = { 1, 2, 3, 4 };
        Arrays.parallelPrefix(pref, new Suma());
        r = r + (Arrays.toString(pref).equals("[1, 3, 6, 10]") ? 10000 : 0);

        return r;
    }

    public static void main(String[] x) { System.out.println(run()); }
}
