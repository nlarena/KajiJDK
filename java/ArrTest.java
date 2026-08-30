import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// Comportamiento de java.util.Arrays: orden, busqueda, copia, comparacion y las variantes hondas.
public class ArrTest {

    public static int run() {
        int r = 0;

        // ---- sort de primitivos ----------------------------------------------------------------
        int[] a = { 5, 3, 9, 1, 7, 3 };
        Arrays.sort(a);
        r = r + (Arrays.toString(a).equals("[1, 3, 3, 5, 7, 9]") ? 1 : 0);

        // por rango: solo se toca [1, 4)
        int[] b = { 9, 5, 3, 1, 7 };
        Arrays.sort(b, 1, 4);
        r = r + (Arrays.toString(b).equals("[9, 1, 3, 5, 7]") ? 10 : 0);

        // double con -0.0 y NaN: el orden total que exige la especificacion
        double[] d = { 1.0d, 0.0d / 0.0d, -0.0d, 0.0d, -1.0d };
        Arrays.sort(d);
        r = r + (d[0] == -1.0d ? 100 : 0);
        r = r + (Double.doubleToRawLongBits(d[1]) == Double.doubleToRawLongBits(-0.0d) ? 1000 : 0);
        r = r + (Double.isNaN(d[4]) ? 10000 : 0);          // NaN al final

        // ---- sort de objetos: estable -----------------------------------------------------------
        //
        // Dos elementos que empatan tienen que quedar en el orden en que venian. Se ordena por
        // largo; "bb" y "cc" empatan y "bb" venia primero.
        String[] s = { "dddd", "bb", "cc", "a" };
        Comparator<String> porLargo = new PorLargo();
        Arrays.sort(s, porLargo);
        r = r + (Arrays.toString(s).equals("[a, bb, cc, dddd]") ? 100000 : 0);

        String[] nat = { "pera", "banana", "anana" };
        Arrays.sort(nat);
        r = r + (nat[0].equals("anana") ? 1 : 0);

        // ---- binarySearch -----------------------------------------------------------------------
        int[] orden = { 1, 3, 5, 7, 9 };
        r = r + Arrays.binarySearch(orden, 7) * 10;                    // 3 -> 30
        // el negativo codifica DONDE iria: -(2)-1 = -3 para el 4
        r = r + (Arrays.binarySearch(orden, 4) == -3 ? 100 : 0);
        r = r + (Arrays.binarySearch(orden, 0) == -1 ? 1000 : 0);
        r = r + (Arrays.binarySearch(orden, 99) == -6 ? 10000 : 0);

        // ---- copyOf / copyOfRange ---------------------------------------------------------------
        int[] corto = Arrays.copyOf(orden, 3);
        r = r + (Arrays.toString(corto).equals("[1, 3, 5]") ? 1 : 0);
        // agrandar rellena con el valor por defecto
        int[] largo = Arrays.copyOf(orden, 7);
        r = r + (largo[6] == 0 ? 10 : 0);
        int[] rango = Arrays.copyOfRange(orden, 1, 4);
        r = r + (Arrays.toString(rango).equals("[3, 5, 7]") ? 100 : 0);
        // el generico conserva el tipo dinamico
        String[] copiaS = Arrays.copyOf(nat, 2);
        r = r + (copiaS.length == 2 && copiaS[0].equals("anana") ? 1000 : 0);

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

    public static void main(String[] args) {
        System.out.println(run());
    }
}

class PorLargo implements Comparator<String> {
    public int compare(String x, String y) {
        return x.length() - y.length();
    }
}

class Doble implements java.util.function.IntUnaryOperator {
    public int applyAsInt(int i) {
        return i * 2;
    }
}

class Suma implements java.util.function.IntBinaryOperator {
    public int applyAsInt(int x, int y) {
        return x + y;
    }
}
