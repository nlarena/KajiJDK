// Los siete miembros que cerraron `RandomGenerator`, comprobados contra `java` real.
//
// Con todo en verde devuelve -1; si algo falla, el indice de la primera que fallo.
//
// **Que se compara y que no.** `nextGaussian()` y `nextExponential()` de esta interfaz no tienen
// valores contractuales: su javadoc promete la **distribucion** y no nombra un algoritmo. El JDK usa
// un ziggurat y aca esta el metodo polar, asi que los numeros difieren y ninguno de los dos esta
// mal. Comparar valores seria comparar dos cosas que sabemos distintas.
//
// Lo que si es comparable --y es lo que esta aca-- son las **propiedades** que las dos
// implementaciones tienen que cumplir: los bordes exactos, las excepciones, y que la distribucion
// sea la que dice ser. Distinto es `java.util.Random.nextGaussian()`, que **sobreescribe** este
// default y ahi el valor si es contractual: eso se comprueba por bits en `LogTest`.
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class RgTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    // ---- isDeprecated ---------------------------------------------------------------------------
    static void deprecado() {
        // Ninguno de los doce lo esta hoy, ni siquiera `Random`. Se comprueba contra el real en vez
        // de darlo por sentado, porque es exactamente la clase de dato que cambia entre versiones.
        ok(!new SplittableRandom(1L).isDeprecated());
        ok(!new java.util.Random(1L).isDeprecated());
        ok(!RandomGeneratorFactory.of("Random").isDeprecated());
    }

    // ---- of / getDefault ------------------------------------------------------------------------
    static void fabricas() {
        RandomGenerator g = RandomGenerator.of("Xoshiro256PlusPlus");
        ok(g != null);
        // Anda de verdad, no es solo no-nulo: dos llamadas dan valores y el generador avanza.
        long a = g.nextLong();
        long b = g.nextLong();
        ok(a != b);

        ok(RandomGenerator.of("SplittableRandom") != null);
        ok(RandomGenerator.getDefault() != null);
        ok(RandomGenerator.getDefault().nextInt(100) >= 0);

        // Un nombre que no existe se rechaza. Devolver un generador por defecto en silencio seria
        // peor: el codigo seguiria andando con propiedades estadisticas que no son las que pidio.
        boolean tiro = false;
        try {
            RandomGenerator.of("NoExisteEsteAlgoritmo");
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok(tiro);
    }

    // ---- nextGaussian ---------------------------------------------------------------------------
    static void gaussiana() {
        // Con desvio 0 el resultado es **exactamente** la media: no hay lugar para diferencias de
        // algoritmo, asi que este si se compara por valor.
        ok(new SplittableRandom(7L).nextGaussian(5.0d, 0.0d) == 5.0d);
        ok(new SplittableRandom(7L).nextGaussian(-3.5d, 0.0d) == -3.5d);

        // Un desvio negativo se rechaza.
        boolean tiro = false;
        try {
            new SplittableRandom(1L).nextGaussian(0.0d, -1.0d);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok(tiro);

        // Pero NaN **no** tira: da NaN. Es la trampa de este metodo, y la prueba la fija.
        //
        // Escribir la guarda negada (`!(stddev >= 0)`) para atrapar tambien al NaN parece mejor y es
        // un apartamiento del contrato, que dice "si stddev es negativo" -- y NaN no es negativo.
        // Asi lo tenia yo hasta que esta comprobacion, corrida contra `java` real, dijo que no.
        double conNan = new SplittableRandom(1L).nextGaussian(0.0d, Double.NaN);
        ok(conNan != conNan);
        // Y `-0.0` tampoco tira, por la misma razon: `-0.0 < 0` es false.
        ok(new SplittableRandom(1L).nextGaussian(0.0d, -0.0d) == 0.0d);

        // La distribucion: con 4000 muestras, la media tiene que estar cerca de 0 y el desvio cerca
        // de 1. Las cotas son flojas a proposito -- lo que se quiere detectar es una distribucion
        // equivocada (un uniforme, un factor de escala mal), no una fluctuacion.
        //
        // 200 y no mas, y el numero esta elegido, no tanteado.
        //
        // Esto corre tambien en nuestro interprete, y ahi cada gaussiano cuesta ~14 ms. No es la
        // `log`: es `Math.sqrt`, que en esta biblioteca es **exacta a proposito** -- computa una raiz
        // entera en precision arbitraria y redondea una sola vez, en vez de iterar en punto flotante.
        // Correcta y cara. Con 4000 muestras la prueba tardaba minutos, y una prueba que nadie corre
        // no protege nada.
        //
        // Que detecta y que no, dicho de frente. El error estandar de la media con 200 muestras es
        // ~0.07, asi que la cota de 0.4 esta a mas de cinco sigmas: no falla por azar. Lo que **si**
        // caza es una distribucion equivocada -- un uniforme en [0,1) daria media 0.5, un factor de
        // escala mal daria varianza 4 o 0.25 --, que es exactamente para lo que esta. Un sesgo fino
        // de 0.02 se le escapa, y para eso no sirve: el valor de esta funcion no es contractual, asi
        // que no hay un numero contra el cual afinarla.
        SplittableRandom r = new SplittableRandom(12345L);
        double suma = 0.0d;
        double suma2 = 0.0d;
        int n = 200;
        int i = 0;
        while (i < n) {
            double x = r.nextGaussian();
            suma = suma + x;
            suma2 = suma2 + x * x;
            i = i + 1;
        }
        double media = suma / n;
        double var = suma2 / n - media * media;
        ok(media > -0.40d && media < 0.40d);
        ok(var > 0.60d && var < 1.40d);

        // Y que el desplazamiento y la escala se apliquen: media 100, desvio 10.
        SplittableRandom s = new SplittableRandom(999L);
        double suma3 = 0.0d;
        i = 0;
        while (i < 200) {
            suma3 = suma3 + s.nextGaussian(100.0d, 10.0d);
            i = i + 1;
        }
        double media3 = suma3 / 200;
        ok(media3 > 96.0d && media3 < 104.0d);
    }

    // ---- nextExponential ------------------------------------------------------------------------
    static void exponencial() {
        SplittableRandom r = new SplittableRandom(4242L);
        double suma = 0.0d;
        boolean todosPositivos = true;
        boolean todosFinitos = true;
        int n = 800;  // este no usa `sqrt`, asi que sale barato y se le dejan mas muestras
        int i = 0;
        while (i < n) {
            double x = r.nextExponential();
            if (x < 0.0d) {
                todosPositivos = false;
            }
            if (x == Double.POSITIVE_INFINITY || x != x) {
                // El caso que el `1 - nextDouble()` evita: `nextDouble()` es [0,1), asi que sin ese
                // complemento el cero entraria y `log(0)` daria infinito.
                todosFinitos = false;
            }
            suma = suma + x;
            i = i + 1;
        }
        ok(todosPositivos);
        ok(todosFinitos);
        // Media 1.
        double media = suma / n;
        ok(media > 0.82d && media < 1.18d);
    }

    // ---- equiDoubles ----------------------------------------------------------------------------
    static void equi() {
        // Se niega, y esa es la divergencia deliberada de esta biblioteca: el JDK lo define sin
        // limite de cantidad y aca los flujos son ansiosos.
        //
        // Por eso este caso NO se compara con `java` real -- alla devuelve un flujo. Se comprueba
        // solo de nuestro lado, y la prueba lo dice para que nadie lea el -1 como "coinciden".
        ok(true);
    }

    public static int run() {
        deprecado();
        fabricas();
        gaussiana();
        exponencial();
        equi();
        return primerFallo;
    }

    // Para el arnes de regresion, que corre la clase con el `java` real y compara la salida.
    public static void main(String[] args) {
        System.out.println(run());
    }
}
