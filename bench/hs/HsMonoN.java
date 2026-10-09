// HsMonoN — tercer escalon de la escalera y su piso (ver `HsMonoL` para el argumento completo).
//
// **Sin llamada**: el cuerpo de `HsOpA.f` escrito en el lugar, en la fuente. Es lo que `HsMonoS`
// mide si la llamada inlineada no cuesta absolutamente nada.
//
// **`HsMonoS - HsMonoN` = lo que cuesta una llamada ligada estaticamente DESPUES de inlinearla.**
// Si da cerca de cero, el inlining de este tier es tan bueno como el de C2 para este caso y todo el
// 8x que queda esta en el acceso al arreglo y en la guarda. Si da mucho, lo que cuesta no es la
// llamada sino lo que el emisor hace alrededor de un cuerpo expandido --escribir y releer los homes
// de los operandos en vez de dejarlos en registros-- y entonces el trabajo es ahi.
//
// El `if (NEVER)` del cuerpo expandido se conserva, por lo mismo que en los otros dos escalones: la
// resta tiene que medir la llamada y no la diferencia de cuerpos.
public class HsMonoN {
    static int NEVER = 0;
    static int OPNEVER = 0;

    static int step(int n) {
        int acc = 1;
        if (HsMonoN.NEVER != 0) {
            acc = acc + n;
        }
        for (int i = 0; i < n; i++) {
            if (HsMonoN.OPNEVER != 0) {
                acc = acc + 2;
            } else {
                acc = acc + 1;
            }
            acc = acc ^ (acc >> 7);
        }
        return acc & 0xFFFFF;
    }

    public static int run(int reps) {
        int acc = 0;
        for (int k = 0; k < reps; k++) {
            acc = (acc + HsMonoN.step(300)) & 0xFFFFF;
        }
        return acc;
    }

    public static int bench(int iters, int reps) {
        long[] nanos = new long[iters];
        int acc = 0;
        for (int i = 0; i < iters; i++) {
            long t0 = System.nanoTime();
            acc = acc + HsMonoN.run(reps);
            long t1 = System.nanoTime();
            nanos[i] = t1 - t0;
        }
        for (int i = 0; i < iters; i++) {
            System.out.println(nanos[i]);
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(HsMonoN.bench(Integer.parseInt(args[0]), Integer.parseInt(args[1])));
    }
}
