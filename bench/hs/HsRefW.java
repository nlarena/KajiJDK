// HsRefW — portado de `bench/BkRefW.java` a la forma parametrizada que mide `bench/hs/medir2.py`.
//
// Dimension: **escritura de referencias a campo** (`putfield` de tipo `Object`), o sea la write barrier. El objeto se asigna FUERA del lazo y la aritmetica que acompana no depende de las escrituras: son puro costo.
//
// Lo unico que cambia respecto del original es que `run` toma las repeticiones como PARAMETRO y que
// se agrega `bench`, que corre K iteraciones en proceso cronometrando cada una con
// `System.nanoTime` y emite la serie completa. El cuerpo de `step` es el del original letra por
// letra, para que las filas se puedan leer contra la linea base de `bench/BASELINE.tsv`.
public class HsRefW {
    static int NEVER = 0;

    Object a;
    Object b;

    static int step(HsRefW o, Object x, Object y, int n) {
        int acc = 1;
        if (HsRefW.NEVER != 0) {
            acc = acc + n;
        }
        for (int i = 0; i < n; i++) {
            o.a = x;
            o.b = y;
            acc = acc + i;
            acc = acc ^ (acc >> 7);
        }
        return acc & 0xFFFFF;
    }

    public static int run(int reps) {
        HsRefW o = new HsRefW();
        Object x = new Object();
        Object y = new Object();
        int acc = 0;
        for (int k = 0; k < reps; k++) {
            acc = (acc + HsRefW.step(o, x, y, 300)) & 0xFFFFF;
        }
        return acc;
    }

    public static int bench(int iters, int reps) {
        long[] nanos = new long[iters];
        int acc = 0;
        for (int i = 0; i < iters; i++) {
            long t0 = System.nanoTime();
            acc = acc + HsRefW.run(reps);
            long t1 = System.nanoTime();
            nanos[i] = t1 - t0;
        }
        for (int i = 0; i < iters; i++) {
            System.out.println(nanos[i]);
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(HsRefW.bench(Integer.parseInt(args[0]), Integer.parseInt(args[1])));
    }
}
