// HsRefA — portado de `bench/BkRefA.java` a la forma parametrizada que mide `bench/hs/medir2.py`.
//
// Dimension: **escritura de referencias a arreglo** (`aastore`). Ademas de la write barrier, un `aastore` debe hacer la comprobacion de tipo de almacenamiento que un `iastore` no tiene. El arreglo se asigna FUERA del lazo caliente y los indices se enmascaran a 8 bits.
//
// Lo unico que cambia respecto del original es que `run` toma las repeticiones como PARAMETRO y que
// se agrega `bench`, que corre K iteraciones en proceso cronometrando cada una con
// `System.nanoTime` y emite la serie completa. El cuerpo de `step` es el del original letra por
// letra, para que las filas se puedan leer contra la linea base de `bench/BASELINE.tsv`.
public class HsRefA {
    static int NEVER = 0;

    static int step(Object[] arr, Object x, Object y, int n) {
        int acc = 1;
        if (HsRefA.NEVER != 0) {
            acc = acc + n;
        }
        for (int i = 0; i < n; i++) {
            arr[i & 255] = x;
            arr[(i + 1) & 255] = y;
            acc = acc + i;
            acc = acc ^ (acc >> 7);
        }
        return acc & 0xFFFFF;
    }

    public static int run(int reps) {
        Object[] arr = new Object[256];
        Object x = new Object();
        Object y = new Object();
        int acc = 0;
        for (int k = 0; k < reps; k++) {
            acc = (acc + HsRefA.step(arr, x, y, 300)) & 0xFFFFF;
        }
        return acc;
    }

    public static int bench(int iters, int reps) {
        long[] nanos = new long[iters];
        int acc = 0;
        for (int i = 0; i < iters; i++) {
            long t0 = System.nanoTime();
            acc = acc + HsRefA.run(reps);
            long t1 = System.nanoTime();
            nanos[i] = t1 - t0;
        }
        for (int i = 0; i < iters; i++) {
            System.out.println(nanos[i]);
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(HsRefA.bench(Integer.parseInt(args[0]), Integer.parseInt(args[1])));
    }
}
