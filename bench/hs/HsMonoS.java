// HsMonoS — segundo escalon de la escalera (ver `HsMonoL` para el argumento completo).
//
// Saca la **guarda de clase del PIC**: la llamada es `invokestatic` a un metodo con el cuerpo de
// `HsOpA.f` letra por letra, asi que esta ligada estaticamente y el emisor no tiene nada que
// comparar antes de entrar. El receptor sigue viniendo de un local, igual que en `HsMonoL`.
//
// **`HsMonoL - HsMonoS` = la guarda de clase.** Es la resta que decide si vale la pena izarla del
// lazo: si da cerca de cero, la guarda es gratis y el 8x esta en otra parte.
//
// El cuerpo de `g` es el de `HsOpA.f` incluido el `if (NEVER)`, porque si no la resta mediria
// tambien la diferencia de cuerpos.
final class HsOpStat {
    static int NEVER = 0;

    static int g(int x) {
        if (HsOpStat.NEVER != 0) {
            return x + 2;
        }
        return x + 1;
    }
}

public class HsMonoS {
    static int NEVER = 0;

    static int step(int n) {
        int acc = 1;
        if (HsMonoS.NEVER != 0) {
            acc = acc + n;
        }
        for (int i = 0; i < n; i++) {
            acc = HsOpStat.g(acc);
            acc = acc ^ (acc >> 7);
        }
        return acc & 0xFFFFF;
    }

    public static int run(int reps) {
        int acc = 0;
        for (int k = 0; k < reps; k++) {
            acc = (acc + HsMonoS.step(300)) & 0xFFFFF;
        }
        return acc;
    }

    public static int bench(int iters, int reps) {
        long[] nanos = new long[iters];
        int acc = 0;
        for (int i = 0; i < iters; i++) {
            long t0 = System.nanoTime();
            acc = acc + HsMonoS.run(reps);
            long t1 = System.nanoTime();
            nanos[i] = t1 - t0;
        }
        for (int i = 0; i < iters; i++) {
            System.out.println(nanos[i]);
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(HsMonoS.bench(Integer.parseInt(args[0]), Integer.parseInt(args[1])));
    }
}
