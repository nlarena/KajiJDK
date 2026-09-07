// **Tratamiento** — dimensión: **despacho de interfaz megamórfico**. Es `BkMono` con la única
// diferencia de que el arreglo tiene cuatro clases distintas en vez de cuatro veces la misma; los
// cuerpos de las cuatro son idénticos (ver `BkOps.java`), así que la aritmética ejecutada es la
// misma y **el número impreso es el mismo**.
//
// Es la fila que puede salir negativa y por la que vale la pena que exista: un caché de sitio de
// llamada que especula sobre la clase del receptor tiene acá cuatro fallos por cada cuatro llamadas,
// y una guarda que falla siempre es trabajo agregado sobre el intérprete. Si el JIT rinde **peor**
// acá, esta fila es la que lo dice.
//
// Su gemelo de efecto cero es `BkMegaC`.
public class BkMega {
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
        BkOp[] ops = { new BkOpA(), new BkOpB(), new BkOpC(), new BkOpD() };
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
