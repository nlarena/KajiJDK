// HsMono — portado de `bench/BkMono.java` a la forma parametrizada que mide `bench/hs/medir2.py`.
//
// Dimension: **despacho de interfaz monomorfico** (`invokeinterface` con un solo tipo de receptor). El sitio de llamada ve siempre `HsOpA`, que es el caso que un cache de sitio de llamada puede ganar. Su par no es un control sino `HsMega`, que corre EL MISMO programa con cuatro receptores de cuerpo identico.
//
// Lo unico que cambia respecto del original es que `run` toma las repeticiones como PARAMETRO y que
// se agrega `bench`, que corre K iteraciones en proceso cronometrando cada una con
// `System.nanoTime` y emite la serie completa. El cuerpo de `step` es el del original letra por
// letra, para que las filas se puedan leer contra la linea base de `bench/BASELINE.tsv`.
public class HsMono {
    static int NEVER = 0;

    static int step(HsOp[] ops, int n) {
        int acc = 1;
        if (HsMono.NEVER != 0) {
            acc = acc + n;
        }
        for (int i = 0; i < n; i++) {
            acc = ops[i & 3].f(acc);
            acc = acc ^ (acc >> 7);
        }
        return acc & 0xFFFFF;
    }

    public static int run(int reps) {
        HsOp a = new HsOpA();
        HsOp[] ops = { a, a, a, a };
        int acc = 0;
        for (int k = 0; k < reps; k++) {
            acc = (acc + HsMono.step(ops, 300)) & 0xFFFFF;
        }
        return acc;
    }

    public static int bench(int iters, int reps) {
        long[] nanos = new long[iters];
        int acc = 0;
        for (int i = 0; i < iters; i++) {
            long t0 = System.nanoTime();
            acc = acc + HsMono.run(reps);
            long t1 = System.nanoTime();
            nanos[i] = t1 - t0;
        }
        for (int i = 0; i < iters; i++) {
            System.out.println(nanos[i]);
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(HsMono.bench(Integer.parseInt(args[0]), Integer.parseInt(args[1])));
    }
}
