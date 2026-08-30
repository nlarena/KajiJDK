import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// Comportamiento de java.util.Arrays: orden, busqueda, copia, comparacion y las variantes hondas.
public class ArrP1 {

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

        return r;
    }

    public static void main(String[] x) { System.out.println(run()); }
}
