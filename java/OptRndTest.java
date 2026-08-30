// La tanda de Optional/Random/SplittableRandom, comprobada contra `java` real.
//
// Con todo en verde devuelve -1; si algo falla devuelve el **indice de la primera** que fallo, que
// es lo que hace falta para ir a buscarla.
//
// El primer intento fue una mascara de bits, y estaba mal: son 39 comprobaciones y un `int` tiene
// 32, asi que `1 << 32` volvia al bit 0 y las ultimas siete se solapaban con las primeras --que ya
// estaban en 1--. La prueba daba -1 igual, con siete comprobaciones que no se observaban. El
// contador de abajo no tiene ese techo.
//
// Lo que NO esta aca, a proposito: las fabricas de flujos **sin cantidad** (`ints()`, `longs()`,
// `doubles()`, `splits()`), que en esta biblioteca se niegan porque sus flujos son ansiosos. Es una
// divergencia deliberada y documentada; meterla en una prueba comparativa seria comparar dos cosas
// que sabemos distintas.
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Random;
import java.util.SplittableRandom;

public class OptRndTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    // ---- Optional<T> ---------------------------------------------------------------------------
    static void optional() {
        Optional<String> hay = Optional.of("perro");
        Optional<String> no = Optional.empty();

        ok("perro".equals(hay.orElseThrow()));
        boolean tiro = false;
        try {
            no.orElseThrow();
        } catch (NoSuchElementException e) {
            tiro = true;
        }
        ok(tiro);

        // orElseThrow(Supplier): la excepcion la arma quien llama, que es el que sabe por que.
        tiro = false;
        try {
            no.orElseThrow(() -> new IllegalStateException("vacio"));
        } catch (IllegalStateException e) {
            tiro = "vacio".equals(e.getMessage());
        }
        ok(tiro);

        // flatMap aplana el nivel de mas; con vacio no llama a la funcion.
        //
        // Escrito asi --con destino explicito y sin tocar el parametro-- por el finding #307:
        // nuestro javac todavia no infiere el `U` de `map`/`flatMap` desde el cuerpo de la lambda
        // cuando no hay tipo esperado, y en `flatMap` tampoco le da tipo al parametro (su firma
        // anida un comodin adentro de otro). La limitacion es del compilador, no de la clase que se
        // esta probando: lo que se comprueba aca sigue siendo que flatMap **aplana** el nivel de mas
        // y devuelve lo que la funcion devolvio.
        Optional<String> aplanado = hay.flatMap(x -> Optional.of("PERRO"));
        ok("PERRO".equals(aplanado.orElseThrow()));
        Optional<String> aplanadoVacio = no.flatMap(x -> Optional.of("nunca"));
        ok(!aplanadoVacio.isPresent());

        // or: la alternativa se calcula **solo** si hace falta.
        ok("perro".equals(hay.or(() -> Optional.of("gato")).orElseThrow()));
        ok("gato".equals(no.or(() -> Optional.of("gato")).orElseThrow()));

        // ifPresentOrElse: una rama u otra, nunca las dos.
        int[] cuenta = new int[2];
        hay.ifPresentOrElse(x -> cuenta[0]++, () -> cuenta[1]++);
        no.ifPresentOrElse(x -> cuenta[0]++, () -> cuenta[1]++);
        ok(cuenta[0] == 1 && cuenta[1] == 1);

        // stream: cero o un elemento.
        ok(hay.stream().count() == 1L);
        ok(no.stream().count() == 0L);
    }

    // ---- OptionalInt / Long / Double -----------------------------------------------------------
    static void primitivos() {
        OptionalInt oi = OptionalInt.of(7);
        OptionalInt vi = OptionalInt.empty();
        ok(oi.orElseThrow() == 7);
        ok(vi.orElseGet(() -> 42) == 42);
        ok(oi.orElseGet(() -> 42) == 7);
        boolean tiro = false;
        try {
            vi.orElseThrow(() -> new IllegalStateException("x"));
        } catch (IllegalStateException e) {
            tiro = true;
        }
        ok(tiro);
        ok(oi.stream().sum() == 7);
        ok(vi.stream().sum() == 0);

        OptionalLong ol = OptionalLong.of(9L);
        ok(ol.orElseThrow() == 9L);
        ok(OptionalLong.empty().orElseGet(() -> 3L) == 3L);
        ok(ol.stream().sum() == 9L);

        OptionalDouble od = OptionalDouble.of(2.5d);
        ok(od.orElseThrow() == 2.5d);
        ok(OptionalDouble.empty().orElseGet(() -> 1.5d) == 1.5d);
        int[] visto = new int[1];
        od.ifPresent(x -> visto[0]++);
        OptionalDouble.empty().ifPresent(x -> visto[0]++);
        ok(visto[0] == 1);
        int[] par = new int[2];
        od.ifPresentOrElse(x -> par[0]++, () -> par[1]++);
        OptionalDouble.empty().ifPresentOrElse(x -> par[0]++, () -> par[1]++);
        ok(par[0] == 1 && par[1] == 1);
    }

    // ---- Random --------------------------------------------------------------------------------
    static void random() {
        // La secuencia con semilla fija es parte del contrato: tiene que dar lo mismo que el JDK.
        Random r = new Random(1234L);
        ok(r.nextInt() == new Random(1234L).nextInt());

        // nextGaussian: el valor exacto, no "parecido". Se compara por bits.
        long g0 = Double.doubleToRawLongBits(new Random(5L).nextGaussian());
        long g1 = Double.doubleToRawLongBits(new Random(5L).nextGaussian());
        ok(g0 == g1);

        // El par guardado: el 2do valor sale del mismo calculo que el 1ro, asi que dos Random con
        // la misma semilla coinciden tambien en el segundo.
        Random a = new Random(11L);
        Random b = new Random(11L);
        a.nextGaussian();
        b.nextGaussian();
        ok(Double.doubleToRawLongBits(a.nextGaussian())
                == Double.doubleToRawLongBits(b.nextGaussian()));

        // Los flujos con cantidad si son comparables.
        ok(new Random(3L).ints(10L).count() == 10L);
        ok(new Random(3L).doubles(5L).count() == 5L);
        long[] ls = new Random(3L).longs(4L).toArray();
        ok(ls.length == 4);

        // ints(streamSize, origin, bound): todos dentro del rango.
        int[] xs = new Random(8L).ints(50L, 10, 20).toArray();
        boolean dentro = xs.length == 50;
        int i = 0;
        while (i < xs.length) {
            if (xs[i] < 10 || xs[i] >= 20) {
                dentro = false;
            }
            i = i + 1;
        }
        ok(dentro);

        // Un tamaño negativo se rechaza.
        boolean tiro = false;
        try {
            new Random(1L).ints(-1L);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok(tiro);
    }

    // ---- Random.from ---------------------------------------------------------------------------
    static void from() {
        // El puente: un generador moderno pasado como `Random`. Lo que se comprueba es que
        // **delega** -- los dos lados tienen que dar la misma secuencia.
        SplittableRandom fuente = new SplittableRandom(99L);
        Random puente = Random.from(new SplittableRandom(99L));
        ok(puente.nextLong() == fuente.nextLong());
        ok(puente.nextInt() == fuente.nextInt());
        ok(puente.nextDouble() == fuente.nextDouble());

        // Y que se niega a la semilla, porque la suya vive en el generador de atras. Ojo: esto
        // tiene que pasar al **llamarlo**, no al construirlo -- el constructor heredado llama a
        // `setSeed`, asi que un adaptador mal armado explota antes de que nadie lo use.
        boolean tiro = false;
        try {
            Random.from(new SplittableRandom(1L)).setSeed(5L);
        } catch (UnsupportedOperationException e) {
            tiro = true;
        }
        ok(tiro);

        boolean npe = false;
        try {
            Random.from(null);
        } catch (NullPointerException e) {
            npe = true;
        }
        ok(npe);
    }

    // ---- SplittableRandom ----------------------------------------------------------------------
    static void splittable() {
        // split() reproducible: dos generadores iguales dan hijos iguales.
        SplittableRandom p = new SplittableRandom(77L);
        SplittableRandom q = new SplittableRandom(77L);
        ok(p.split().nextLong() == q.split().nextLong());

        // split(source): la entropia sale del source, asi que el hijo no depende de cuanto haya
        // consumido el padre por su cuenta.
        SplittableRandom base1 = new SplittableRandom(5L);
        SplittableRandom base2 = new SplittableRandom(5L);
        SplittableRandom p1 = new SplittableRandom(1L);
        SplittableRandom p2 = new SplittableRandom(2L);
        p2.nextLong();
        p2.nextLong();
        ok(p1.split(base1).nextLong() == p2.split(base2).nextLong());

        ok(new SplittableRandom(4L).splits(6L).count() == 6L);
        ok(new SplittableRandom(4L).splits(3L, new SplittableRandom(9L)).count() == 3L);
    }

    public static int run() {
        optional();
        primitivos();
        random();
        from();
        splittable();
        // -1 = todas pasaron. Si no, el indice de la primera que fallo.
        return primerFallo;
    }

    // Para el arnes de regresion, que corre la clase con el `java` real y compara la salida.
    public static void main(String[] args) {
        System.out.println(run());
    }
}
