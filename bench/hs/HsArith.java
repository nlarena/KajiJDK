// Workload parametrizado para comparar contra HotSpot: aritmetica sobre locales en un metodo
// caliente, que es el piso del motor (la misma dimension que `BkArith`, pero con las iteraciones
// como PARAMETRO).
//
// Por que parametrizado: comparar una corrida contra otra mide el arranque de cada VM --HotSpot
// tarda decenas de ms en levantar-- y no el motor. Con dos tamanos, N y 2N, la PENDIENTE
// (t(2N) - t(N)) / N cancela exactamente el arranque y el calentamiento de entrada, de los dos
// lados, sin tener que estimarlos.
//
// Las dos entradas miden EL MISMO metodo `run(int)`: nuestra VM lo invoca directo con un argumento
// int (`run-headless <class> run <n>`), y HotSpot pasa por `main` porque su CLI no sabe invocar un
// metodo estatico. Ese desbalance de entrada es justo lo que la pendiente descarta.
public class HsArith {
    static int NEVER = 0;

    static int step(int seed, int n) {
        int acc = seed;
        if (HsArith.NEVER != 0) {
            acc = acc + n;
        }
        for (int i = 0; i < n; i++) {
            acc = acc + i;
            acc = acc ^ (acc >> 7);
            if ((i & 15) == 0) {
                acc = acc - 3;
            }
        }
        return acc;
    }

    public static int run(int reps) {
        int acc = 1;
        for (int k = 0; k < reps; k++) {
            acc = HsArith.step(acc, 300);
        }
        return acc;
    }

    public static void main(String[] args) {
        int reps = Integer.parseInt(args[0]);
        System.out.println(HsArith.run(reps));
    }
}
