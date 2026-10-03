// Descartar el resultado `long` o `double` de una llamada, en un bucle caliente: `invokestatic …J;
// pop2`. Es la forma más común de `pop2` sobre un valor de categoría 2, y hasta que el mapa de tipos
// aprendió a leer el tipo del tope, rechazaba el método entero como `StackUnderflow`.
//
// **Lo que distingue a esta fixture es la entrada por OSR, no `compiled > 0`.** `lh` y `dh` compilan
// solos con o sin el arreglo, porque ellos no descartan nada; el único con bucle es `run`, así que
// un OSR sólo puede venir de que `run` compiló. Es la trampa de JcCat de nuevo: una aserción que
// cumplen los helpers no dice nada del método que se quiere medir.
//
// **Y el estado es de 64 bits de verdad.** La primera versión hacía `calls += i` y después
// `calls ^= i`, que se cancelan: `calls` quedaba en 0 para siempre y el `java` real imprimía 0, un
// resultado que cualquier implementación rota también da. Ahora `lh` es un paso de LCG y `dh`
// mezcla la mitad alta en la baja, así que perder un `long` o leerlo de un solo slot cambia el
// número.
//
// Compilada con el `javac` del JDK 25, no con el nuestro (FZ-003).
public class JcPop2 {
    static long calls = 1;

    static long lh(int i) {
        calls = calls * 6364136223846793005L + i;
        return calls;
    }

    static double dh(int i) {
        calls ^= calls >>> 29;
        return calls / 3.0;
    }

    static int run() {
        int acc = 0;
        for (int i = 0; i < 20000; i++) {
            lh(i);
            dh(i);
            acc = (acc * 31 + (int) calls + (int) (calls >>> 32)) & 0xFFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
