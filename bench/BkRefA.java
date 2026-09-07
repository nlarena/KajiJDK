// **Tratamiento** — dimensión: **escritura de referencias a arreglo** (`aastore`). El otro frente
// que esta sesión agregó y que nunca se midió: además de la write barrier, un `aastore` debe hacer
// la comprobación de tipo de almacenamiento que un `iastore` no tiene.
//
// El arreglo se asigna **fuera** del lazo caliente, igual que en `JdArray`, para que lo que se mida
// sea la escritura y no el colector. Los índices se enmascaran a 8 bits para que el arreglo entre en
// caché y la fila hable de la barrera y no de la memoria.
//
// Su gemelo de efecto cero es `BkRefAC`. Los dos imprimen el mismo número.
public class BkRefA {
    static int NEVER = 0;

    static int step(Object[] arr, Object x, Object y, int n) {
        int acc = 1;
        if (NEVER != 0) {
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

    static int run() {
        Object[] arr = new Object[256];
        Object x = new Object();
        Object y = new Object();
        int acc = 0;
        if (NEVER != 0) {
            acc = acc + acc;
        }
        for (int k = 0; k < 1000; k++) {
            acc = (acc + step(arr, x, y, 300)) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
