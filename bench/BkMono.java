// **Tratamiento** — dimensión: **despacho de interfaz monomórfico** (`invokeinterface` con un solo
// tipo de receptor). El sitio de llamada ve siempre `BkOpA`, que es el caso que un caché de sitio de
// llamada puede ganar.
//
// Su par no es sólo su control: es `BkMega`, que corre **el mismo programa** con cuatro receptores
// de cuerpo idéntico. Los dos imprimen el mismo número, así que la diferencia entre esas dos filas
// es despacho puro. Ver `BkOps.java`.
//
// Su gemelo de efecto cero es `BkMonoC`.
public class BkMono {
    static int NEVER = 0;

    static int step(BkOp[] ops, int n) {
        int acc = 1;
        if (NEVER != 0) {
            acc = acc + n;
        }
        for (int i = 0; i < n; i++) {
            acc = ops[i & 3].f(acc);
            acc = acc ^ (acc >> 7);
        }
        return acc & 0xFFFFF;
    }

    static int run() {
        BkOp a = new BkOpA();
        BkOp[] ops = { a, a, a, a };
        int acc = 0;
        if (NEVER != 0) {
            acc = acc + acc;
        }
        for (int k = 0; k < 1000; k++) {
            acc = (acc + step(ops, 300)) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
