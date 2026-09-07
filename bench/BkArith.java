// **Tratamiento** — dimensión: aritmética sobre locales dentro de un método caliente.
//
// Es la forma de `JtLoop`: el lazo vive en `step`, al que `run` llama 1000 veces. No toca memoria,
// no llama a nada, no asigna. Es el piso del motor y la fila contra la que se leen las demás.
//
// Su gemelo de efecto cero es `BkArithC`, que es este archivo más una rama que nunca se toma y que
// contiene un `invokedynamic`. Los dos imprimen el mismo número; el test lo comprueba.
public class BkArith {
    static int NEVER = 0;

    static int step(int seed, int n) {
        int acc = seed;
        if (NEVER != 0) {
            acc = acc + n;
        }
        for (int i = 0; i < n; i++) {
            acc = acc + i;
            acc = acc ^ (acc >> 7);
            if ((i & 15) == 0) {
                acc = acc - 3;
            }
        }
        return acc & 0xFFFFF;
    }

    static int run() {
        int acc = 1;
        if (NEVER != 0) {
            acc = acc + acc;
        }
        for (int k = 0; k < 1000; k++) {
            acc = step(acc, 300);
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
