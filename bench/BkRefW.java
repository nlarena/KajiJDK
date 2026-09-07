// **Tratamiento** — dimensión: **escritura de referencias a campo** (`putfield` de tipo `Object`),
// o sea la *write barrier*. Es uno de los frentes que esta sesión agregó al subconjunto y del que
// no existía ni un número.
//
// El lazo escribe dos campos de referencia por iteración sobre un objeto asignado **fuera** del
// lazo, y la aritmética que lo acompaña no depende de esas escrituras: son puro costo. La forma es
// la de `JdField` — el lazo caliente es un método propio, la asignación se queda en `run` — para que
// lo que se mide sea la escritura y no el colector.
//
// Su gemelo de efecto cero es `BkRefWC`. Los dos imprimen el mismo número.
public class BkRefW {
    static int NEVER = 0;

    Object a;
    Object b;

    static int step(BkRefW o, Object x, Object y, int n) {
        int acc = 1;
        if (NEVER != 0) {
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

    static int run() {
        BkRefW o = new BkRefW();
        Object x = new Object();
        Object y = new Object();
        int acc = 0;
        if (NEVER != 0) {
            acc = acc + acc;
        }
        for (int k = 0; k < 1000; k++) {
            acc = (acc + step(o, x, y, 300)) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
