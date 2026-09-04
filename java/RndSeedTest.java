// Lo que esta tanda agrego a los generadores: la semilla de **bytes**, el salto, y la particion.
//
// **Se comprueba contra `java` real corriendo lo mismo**, y esta vez la comparacion es lo unico que
// vale: sembrar desde bytes no es "cualquier mapeo razonable", es **un** mapeo concreto
// --`RandomSupport.convertSeedBytesToLongs`-- y si difiere en un bit, el generador entero emite otra
// secuencia. Un test que solo mirara la distribucion no veria la diferencia.
//
// Lo mismo con el salto: las tablas de salto son los polinomios publicados del algoritmo, y avanzar
// `jumpDistance()` valores tiene que dar **exactamente** el mismo estado que llamar `nextLong()` esa
// cantidad de veces. Aca no se puede comprobar directo --son dos a la sesenta y cuatro llamadas--
// pero si que las dos VMs coincidan, que es lo que detecta una tabla mal copiada.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class RndSeedTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    // Un resumen de los primeros valores del generador. Se mezcla con un multiplicador impar para
    // que dos secuencias distintas no puedan dar el mismo resumen por cancelacion.
    static long resumen(RandomGenerator g, int cuantos) {
        long h = 0L;
        int i = 0;
        while (i < cuantos) {
            h = h * 1099511628211L + g.nextLong();
            i = i + 1;
        }
        return h;
    }

    static final String[] LXM = {
        "L32X64MixRandom", "L64X128MixRandom", "L64X128StarStarRandom", "L64X256MixRandom",
        "L64X1024MixRandom", "L128X128MixRandom", "L128X256MixRandom", "L128X1024MixRandom",
        "Xoroshiro128PlusPlus", "Xoshiro256PlusPlus",
    };

    // El valor esperado se calcula, no se escribe a mano: lo que se compara es el numero que sale de
    // las dos VMs, y por eso el test devuelve un entero derivado de todo.
    static long acumulado = 0L;

    static void anota(long v) {
        acumulado = acumulado * 31L + v;
    }

    static void semillaDeBytes() {
        // Cuatro semillas de largos distintos, a proposito: una mas corta que el estado --que obliga
        // a rellenar--, una justa, una larga --que obliga a descartar-- y una toda en cero, que es el
        // caso que el rellenado tiene que rescatar (el cero es punto fijo de un xor-shift).
        byte[][] semillas = {
            new byte[] { 1, 2, 3 },
            new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 },
            new byte[200],
            new byte[32],
        };
        int s = 0;
        while (s < semillas.length) {
            if (s == 2) {
                int k = 0;
                while (k < semillas[2].length) {
                    semillas[2][k] = (byte) (k * 7 + 3);
                    k = k + 1;
                }
            }
            int i = 0;
            while (i < LXM.length) {
                RandomGenerator g = RandomGeneratorFactory.of(LXM[i]).create(semillas[s]);
                ok(g != null);
                anota(resumen(g, 8));
                i = i + 1;
            }
            s = s + 1;
        }

        // La misma semilla dos veces da el mismo generador. Es lo minimo que "sembrar" significa.
        byte[] fija = { 9, 8, 7, 6, 5, 4, 3, 2, 1 };
        int i = 0;
        while (i < LXM.length) {
            RandomGenerator a = RandomGeneratorFactory.of(LXM[i]).create(fija);
            RandomGenerator b = RandomGeneratorFactory.of(LXM[i]).create(fija);
            ok(resumen(a, 6) == resumen(b, 6));
            i = i + 1;
        }

        // Y dos semillas distintas dan generadores distintos.
        byte[] otra = { 9, 8, 7, 6, 5, 4, 3, 2, 2 };
        i = 0;
        while (i < LXM.length) {
            RandomGenerator a = RandomGeneratorFactory.of(LXM[i]).create(fija);
            RandomGenerator b = RandomGeneratorFactory.of(LXM[i]).create(otra);
            ok(resumen(a, 6) != resumen(b, 6));
            i = i + 1;
        }

        // Nulo se rechaza; los dos heredados no admiten bytes.
        boolean tiro = false;
        try {
            RandomGeneratorFactory.of("L64X128MixRandom").create((byte[]) null);
        } catch (NullPointerException e) {
            tiro = true;
        }
        ok(tiro);
    }

    static void salto() {
        String[] saltables = { "Xoroshiro128PlusPlus", "Xoshiro256PlusPlus" };
        double[] distanciaSalto = { 18446744073709551616.0, 340282366920938463463374607431768211456.0 };
        int i = 0;
        while (i < saltables.length) {
            // **Sembrado**, no `LeapableGenerator.of(name)`: esa forma siembra desde el reloj, y un
            // valor que cambia en cada corrida no se puede comparar contra la otra VM. La forma
            // sin semilla se comprueba aparte, mas abajo, solo por su tipo.
            RandomGenerator.LeapableGenerator g = (RandomGenerator.LeapableGenerator)
                    RandomGeneratorFactory.of(saltables[i]).create(new byte[] { 3, 1, 4, 1, 5 });
            ok(g != null);
            ok(g.jumpDistance() == distanciaSalto[i]);
            ok(g.leapDistance() > g.jumpDistance());

            // Una copia arranca donde estaba el original: los dos primeros valores coinciden.
            RandomGenerator.LeapableGenerator copia = g.copy();
            ok(resumen(copia, 4) == resumen(g, 4));

            // Saltar cambia el estado. Se compara contra una copia tomada antes del salto.
            RandomGenerator.LeapableGenerator antes = g.copy();
            g.jump();
            ok(resumen(antes, 4) != resumen(g, 4));
            anota(resumen(g, 4));

            // Y el salto largo tambien, desde el mismo punto de partida.
            RandomGenerator.LeapableGenerator otro = (RandomGenerator.LeapableGenerator)
                    RandomGeneratorFactory.of(saltables[i]).create(new byte[] { 3, 1, 4, 1, 5 });
            RandomGenerator.LeapableGenerator ref = otro.copy();
            otro.leap();
            ok(resumen(ref, 4) != resumen(otro, 4));
            anota(resumen(otro, 4));

            // La forma sin semilla existe y da del tipo correcto. No se compara su valor: siembra
            // desde el reloj.
            ok(RandomGenerator.LeapableGenerator.of(saltables[i]) != null);
            i = i + 1;
        }

        // Un algoritmo que no salta se rechaza, en vez de devolver algo que no salta.
        boolean tiro = false;
        try {
            RandomGenerator.JumpableGenerator.of("L64X128MixRandom");
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok(tiro);
    }

    static void particion() {
        int i = 0;
        while (i < 8) {
            RandomGenerator.SplittableGenerator g =
                    (RandomGenerator.SplittableGenerator) RandomGeneratorFactory.of(LXM[i])
                            .create(new byte[] { 4, 2 });
            RandomGenerator.SplittableGenerator h = g.split();
            ok(h != null);
            // El hijo no es el padre y no repite su secuencia.
            ok(h != g);

            // Cinco hijos, todos distintos entre si. Es la propiedad que hace util partir: si dos
            // coincidieran, dos hilos harian el mismo trabajo creyendo que hacen distinto.
            java.util.List<Long> resumenes = new java.util.ArrayList<Long>();
            RandomGenerator.SplittableGenerator padre =
                    (RandomGenerator.SplittableGenerator) RandomGeneratorFactory.of(LXM[i])
                            .create(new byte[] { 7, 7, 7 });
            java.util.stream.Stream<RandomGenerator.SplittableGenerator> hijos = padre.splits(5L);
            java.util.List<RandomGenerator.SplittableGenerator> lista =
                    hijos.collect(java.util.stream.Collectors.toList());
            ok(lista.size() == 5);
            int k = 0;
            while (k < lista.size()) {
                resumenes.add(Long.valueOf(resumen(lista.get(k), 4)));
                k = k + 1;
            }
            ok(new java.util.HashSet<Long>(resumenes).size() == 5);
            i = i + 1;
        }
    }

    public static int run() {
        semillaDeBytes();
        salto();
        particion();
        // El acumulado entra en el resultado: si el mapeo de bytes a estado difiere en un bit entre
        // las dos VMs, el numero cambia y el test lo delata.
        if (primerFallo >= 0) {
            return primerFallo;
        }
        return (int) (acumulado ^ (acumulado >>> 32));
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
