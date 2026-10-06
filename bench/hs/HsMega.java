// HsMega — portado de `bench/BkMega.java` a la forma parametrizada que mide `bench/hs/medir2.py`.
//
// Dimension: **despacho de interfaz megamorfico**. Es `HsMono` con la unica diferencia de que el sitio de llamada ve cuatro clases en vez de una; los cuerpos son identicos letra por letra, asi que las dos devuelven el mismo numero y toda diferencia de tiempo entre las dos filas es despacho.
//
// Lo unico que cambia respecto del original es que `run` toma las repeticiones como PARAMETRO y que
// se agrega `bench`, que corre K iteraciones en proceso cronometrando cada una con
// `System.nanoTime` y emite la serie completa. El cuerpo de `step` es el del original letra por
// letra, para que las filas se puedan leer contra la linea base de `bench/BASELINE.tsv`.
public class HsMega {
    static int NEVER = 0;

    static int step(HsOp[] ops, int n) {
        int acc = 1;
        if (HsMega.NEVER != 0) {
            acc = acc + n;
        }
        for (int i = 0; i < n; i++) {
            acc = ops[i & 3].f(acc);
            acc = acc ^ (acc >> 7);
        }
        return acc & 0xFFFFF;
    }

    public static int run(int reps) {
        HsOp[] ops = { new HsOpA(), new HsOpB(), new HsOpC(), new HsOpD() };
        int acc = 0;
        for (int k = 0; k < reps; k++) {
            acc = (acc + HsMega.step(ops, 300)) & 0xFFFFF;
        }
        return acc;
    }

    public static int bench(int iters, int reps) {
        long[] nanos = new long[iters];
        int acc = 0;
        for (int i = 0; i < iters; i++) {
            long t0 = System.nanoTime();
            acc = acc + HsMega.run(reps);
            long t1 = System.nanoTime();
            nanos[i] = t1 - t0;
        }
        for (int i = 0; i < iters; i++) {
            System.out.println(nanos[i]);
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(HsMega.bench(Integer.parseInt(args[0]), Integer.parseInt(args[1])));
    }
}
