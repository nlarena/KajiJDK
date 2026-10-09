// HsMonoL — primer escalon de la escalera que descompone el 8,17x del despacho monomorfico.
//
// # Por que una escalera y no un arreglo del emisor
//
// La fila `HsMono` esta 8,17x abajo de HotSpot y la lectura facil es "nos falta inlinear". Es falsa
// y se comprobo: con `JVM_JIT_LOG`, `HsOpA.f` **no aparece en la lista de compilados** porque ya
// esta expandido adentro de `HsMono.step`. El PIC lo liga, `plan` lo inlinea, y HotSpot cuesta 0,77
// ns por iteracion interna contra nuestros ~6,3. O sea que lo que falta no es inlinear sino lo que
// viene DESPUES de inlinear, y eso puede ser tres cosas distintas que la fila suma en un numero:
//
//   1. el acceso al arreglo de receptores (`aaload`, con su chequeo de nulo y sus dos de rango),
//   2. la guarda de clase del PIC,
//   3. el costo de la llamada inlineada misma.
//
// La escalera le saca una por vez y resta. Este escalon saca la (1): **el receptor viaja en un
// local** en vez de salir de `ops[i & 3]`. Como `HsMono` tiene las cuatro posiciones apuntando al
// MISMO objeto, `ops[i & 3]` es siempre `a` y este programa calcula exactamente lo mismo: la unica
// diferencia es de donde sale la referencia.
//
// `HsMono - HsMonoL` = el acceso al arreglo. Los otros dos escalones son `HsMonoS` (llamada
// estatica, sin guarda) y `HsMonoN` (sin llamada), y las restas estan en el encabezado de cada uno.
//
// El `if (NEVER)` se conserva igual que en las otras filas, por lo mismo: sin el, el cuerpo de
// `step` deja de ser comparable con la linea base de `bench/`.
public class HsMonoL {
    static int NEVER = 0;

    static int step(HsOp o, int n) {
        int acc = 1;
        if (HsMonoL.NEVER != 0) {
            acc = acc + n;
        }
        for (int i = 0; i < n; i++) {
            acc = o.f(acc);
            acc = acc ^ (acc >> 7);
        }
        return acc & 0xFFFFF;
    }

    public static int run(int reps) {
        HsOp a = new HsOpA();
        int acc = 0;
        for (int k = 0; k < reps; k++) {
            acc = (acc + HsMonoL.step(a, 300)) & 0xFFFFF;
        }
        return acc;
    }

    public static int bench(int iters, int reps) {
        long[] nanos = new long[iters];
        int acc = 0;
        for (int i = 0; i < iters; i++) {
            long t0 = System.nanoTime();
            acc = acc + HsMonoL.run(reps);
            long t1 = System.nanoTime();
            nanos[i] = t1 - t0;
        }
        for (int i = 0; i < iters; i++) {
            System.out.println(nanos[i]);
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(HsMonoL.bench(Integer.parseInt(args[0]), Integer.parseInt(args[1])));
    }
}
