// Workload para comparar contra HotSpot midiendo ADENTRO del proceso.
//
// Por que in-process y no un proceso por punto, que es lo que hacia `medir.py`: con un proceso por
// medicion, cada punto arrastra el arranque de la VM, la carga de la biblioteca y el layout de codigo
// de un proceso distinto, y esa varianza entra entera en la medicion -- fue la que nos dio 94% de
// dispersion. Midiendo K iteraciones adentro de UN proceso, la unica varianza que queda entre
// iteraciones es la del motor.
//
// Y emite la SERIE COMPLETA, una linea por iteracion, en vez de un resumen. Eso no es verbosidad:
// resumir antes de clasificar asume que la serie llego a estado estacionario, que es exactamente lo
// que Barrett et al. (OOPSLA 2017) midieron y encontraron falso en la mayoria de los casos --- a lo
// sumo 43.5% de los pares VM/benchmark llegan consistentemente a pico, y algunos se vuelven MAS
// lentos con las iteraciones. Quien resume es el arnes, despues de clasificar.
//
// Las dos entradas corren EL MISMO metodo: nuestra VM invoca `bench` directo con argumentos int
// (`run-headless <class> bench <iters> <reps>`) y HotSpot pasa por `main`, porque su CLI no sabe
// invocar un metodo estatico. El reloj es `System.nanoTime` de los dos lados.
public class HsBench {
    static int NEVER = 0;

    // La dimension medida: aritmetica sobre locales en un metodo caliente, que es el piso del motor.
    // Es la misma forma que `BkArith` de la linea base del Hito F-bench, para que los numeros se
    // puedan leer contra ella.
    static int step(int seed, int n) {
        int acc = seed;
        if (HsBench.NEVER != 0) {
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
            acc = HsBench.step(acc, 300);
        }
        return acc;
    }

    // `iters` iteraciones en proceso, cada una de `reps` repeticiones. Los tiempos se guardan y se
    // imprimen AL FINAL: imprimir dentro del lazo metería la E/S en lo medido.
    public static int bench(int iters, int reps) {
        long[] nanos = new long[iters];
        int acc = 0;
        for (int i = 0; i < iters; i++) {
            long t0 = System.nanoTime();
            acc = acc + HsBench.run(reps);
            long t1 = System.nanoTime();
            nanos[i] = t1 - t0;
        }
        for (int i = 0; i < iters; i++) {
            System.out.println(nanos[i]);
        }
        // El acumulador se devuelve para que nada de esto se pueda considerar codigo muerto.
        return acc;
    }

    public static void main(String[] args) {
        int iters = Integer.parseInt(args[0]);
        int reps = Integer.parseInt(args[1]);
        System.out.println(HsBench.bench(iters, reps));
    }
}
