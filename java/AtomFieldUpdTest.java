// Los tres `Atomic*FieldUpdater`, que son reflexion sobre un campo `volatile` ajeno.
//
// Esta prueba se corre con las dos VMs y se compara el entero. Vale la pena decir por que eso es
// mas fuerte que de costumbre aca: con el `java` real la clase que se ejecuta **no es la nuestra**,
// es la del JDK, porque `java.util.concurrent.atomic` viene del bootclasspath. Asi que el .class
// que emite nuestro javac se resuelve contra la firma verdadera del JDK, y cada tipo y mensaje de
// excepcion que aparece abajo es una comparacion contra el original, no contra nuestra copia.
//
// Por eso mismo la clase objetivo es publica con campos publicos: el JDK real hace un chequeo de
// accesibilidad desde la clase llamadora que nosotros no podemos hacer (`StackWalker.
// getCallerClass()` no esta soportado en esta VM), y con todo publico las dos VMs recorren el
// mismo camino en vez de diferir por lo unico que no podemos reproducir.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class AtomFieldUpdTest {

    public static class Caja {
        public volatile int i;
        public volatile long l;
        public volatile String s;
        public volatile Object o;
        public int noVolatil;
        public long noVolatilL;
        public String noVolatilS;
        public static volatile int estatico;
        public static volatile long estaticoL;
        public static volatile String estaticoS;
    }

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    // ---- enteros ----

    static void enteros() {
        AtomicIntegerFieldUpdater<Caja> u =
                AtomicIntegerFieldUpdater.newUpdater(Caja.class, "i");
        Caja c = new Caja();
        ok(u.get(c) == 0);
        u.set(c, 10);
        ok(u.get(c) == 10);
        ok(c.i == 10);                       // el campo de verdad, no una copia interna
        ok(u.compareAndSet(c, 10, 11));
        ok(c.i == 11);
        ok(!u.compareAndSet(c, 10, 99));     // esperado que no coincide: no escribe
        ok(c.i == 11);
        ok(u.weakCompareAndSet(c, 11, 12));
        ok(u.get(c) == 12);
        u.lazySet(c, 20);
        ok(u.get(c) == 20);
        ok(u.getAndSet(c, 30) == 20);
        ok(u.get(c) == 30);

        // devuelven el previo
        ok(u.getAndIncrement(c) == 30);
        ok(u.get(c) == 31);
        ok(u.getAndDecrement(c) == 31);
        ok(u.get(c) == 30);
        ok(u.getAndAdd(c, 5) == 30);
        ok(u.get(c) == 35);
        // devuelven el nuevo
        ok(u.incrementAndGet(c) == 36);
        ok(u.decrementAndGet(c) == 35);
        ok(u.addAndGet(c, -5) == 30);

        ok(u.getAndUpdate(c, x -> x * 2) == 30);
        ok(u.get(c) == 60);
        ok(u.updateAndGet(c, x -> x + 1) == 61);
        ok(u.getAndAccumulate(c, 3, (x, y) -> x - y) == 61);
        ok(u.get(c) == 58);
        ok(u.accumulateAndGet(c, 8, (x, y) -> x + y) == 66);

        // dos instancias no se pisan
        Caja d = new Caja();
        u.set(d, 7);
        ok(u.get(c) == 66 && u.get(d) == 7);

        // el updater ve un subtipo del objetivo
        ok(u.get(new SubCaja()) == 0);
    }

    public static class SubCaja extends Caja {
    }

    // ---- largos ----

    static void largos() {
        AtomicLongFieldUpdater<Caja> u = AtomicLongFieldUpdater.newUpdater(Caja.class, "l");
        Caja c = new Caja();
        ok(u.get(c) == 0L);
        // Un valor que no entra en 32 bits, por si el `long` se partiera en algun lado.
        u.set(c, 4294967296L);
        ok(u.get(c) == 4294967296L);
        ok(c.l == 4294967296L);
        ok(u.compareAndSet(c, 4294967296L, 4294967300L));
        ok(!u.compareAndSet(c, 0L, 1L));
        ok(u.get(c) == 4294967300L);
        ok(u.weakCompareAndSet(c, 4294967300L, 4294967296L));
        u.lazySet(c, 100L);
        ok(u.getAndSet(c, 200L) == 100L);
        ok(u.getAndIncrement(c) == 200L);
        ok(u.getAndDecrement(c) == 201L);
        ok(u.getAndAdd(c, 4294967296L) == 200L);
        ok(u.incrementAndGet(c) == 4294967497L);
        ok(u.decrementAndGet(c) == 4294967496L);
        ok(u.addAndGet(c, -4294967296L) == 200L);
        ok(u.getAndUpdate(c, x -> x * 2L) == 200L);
        ok(u.updateAndGet(c, x -> x + 1L) == 401L);
        ok(u.getAndAccumulate(c, 1L, (x, y) -> x - y) == 401L);
        ok(u.accumulateAndGet(c, 100L, (x, y) -> x + y) == 500L);
    }

    // ---- referencias ----

    static void referencias() {
        AtomicReferenceFieldUpdater<Caja, String> u =
                AtomicReferenceFieldUpdater.newUpdater(Caja.class, String.class, "s");
        Caja c = new Caja();
        ok(u.get(c) == null);
        u.set(c, "a");
        ok(u.get(c).equals("a"));
        ok(c.s.equals("a"));
        // `null` es un valor legitimo en las dos puntas del CAS.
        ok(u.compareAndSet(c, "a", null));
        ok(u.get(c) == null);
        ok(u.compareAndSet(c, null, "b"));
        ok(u.get(c).equals("b"));

        // El CAS compara por **identidad**, no por equals: dos String iguales pero distintos
        // objetos no deben coincidir. `new String` fuerza la copia; el intern no.
        String otro = new String("b");
        ok(otro.equals("b"));
        ok(!u.compareAndSet(c, otro, "z"));
        ok(u.get(c).equals("b"));

        ok(u.weakCompareAndSet(c, u.get(c), "c"));
        u.lazySet(c, "d");
        ok(u.getAndSet(c, "e").equals("d"));
        ok(u.getAndUpdate(c, x -> x + "!").equals("e"));
        ok(u.get(c).equals("e!"));
        ok(u.updateAndGet(c, x -> x + "?").equals("e!?"));
        // Los tipos de los parametros van escritos a mano a proposito: con los tipos inferidos,
        // `x + y` entre dos String se emite como `iadd` y la VM se cae. Ver el repro en
        // scratchpad/zzatomic/LambdaInferSuma.java.
        ok(u.getAndAccumulate(c, "+", (String x, String y) -> x + y).equals("e!?"));
        ok(u.get(c).equals("e!?+"));
        ok(u.accumulateAndGet(c, "-", (String x, String y) -> y + x).equals("-e!?+"));

        // Un campo declarado `Object` acepta cualquier cosa; vclass tiene que ser Object exacto.
        AtomicReferenceFieldUpdater<Caja, Object> v =
                AtomicReferenceFieldUpdater.newUpdater(Caja.class, Object.class, "o");
        v.set(c, Integer.valueOf(5));
        ok(v.get(c).equals(Integer.valueOf(5)));
    }

    // ---- lo que `newUpdater` rechaza, con el tipo y el mensaje del JDK ----

    static String falla(Runnable r) {
        try {
            r.run();
            return "sin excepcion";
        } catch (Throwable e) {
            return e.getClass().getName() + "|" + e.getMessage();
        }
    }

    // Campo que no existe: RuntimeException (no IllegalArgument) envolviendo el
    // NoSuchFieldException, con el `toString` de la causa por mensaje.
    //
    // Se compara por prefijo y sufijo y no por igualdad porque el detalle del medio difiere entre
    // las dos VMs, y no por culpa de esta clase: nuestro `Class.getDeclaredField` califica el
    // nombre del campo con el de la clase ("Caja.noExiste") y el del JDK no ("noExiste"). Lo que
    // esta prueba tiene que fijar es lo que decide esta clase --que el fallo se envuelve en
    // RuntimeException y que la causa es un NoSuchFieldException por ese campo-- no el texto que
    // pone java.lang.Class.
    static boolean noHayCampo(String r, String campo) {
        return r.startsWith("java.lang.RuntimeException|java.lang.NoSuchFieldException: ")
                && r.endsWith(campo);
    }

    static void rechazos() {
        ok(noHayCampo(falla(() -> AtomicIntegerFieldUpdater.newUpdater(Caja.class, "noExiste")),
                "noExiste"));
        // Tipo equivocado. Se chequea ANTES que volatile: por eso un campo `long` no volatil
        // pedido como int dice "integer", no "volatile".
        ok(falla(() -> AtomicIntegerFieldUpdater.newUpdater(Caja.class, "l"))
                .equals("java.lang.IllegalArgumentException|Must be integer type"));
        ok(falla(() -> AtomicIntegerFieldUpdater.newUpdater(Caja.class, "noVolatilL"))
                .equals("java.lang.IllegalArgumentException|Must be integer type"));
        // No volatil, tipo correcto.
        ok(falla(() -> AtomicIntegerFieldUpdater.newUpdater(Caja.class, "noVolatil"))
                .equals("java.lang.IllegalArgumentException|Must be volatile type"));
        // Estatico: IllegalArgumentException pelada, sin mensaje.
        ok(falla(() -> AtomicIntegerFieldUpdater.newUpdater(Caja.class, "estatico"))
                .equals("java.lang.IllegalArgumentException|null"));

        ok(noHayCampo(falla(() -> AtomicLongFieldUpdater.newUpdater(Caja.class, "noExiste")),
                "noExiste"));
        ok(falla(() -> AtomicLongFieldUpdater.newUpdater(Caja.class, "i"))
                .equals("java.lang.IllegalArgumentException|Must be long type"));
        ok(falla(() -> AtomicLongFieldUpdater.newUpdater(Caja.class, "noVolatilL"))
                .equals("java.lang.IllegalArgumentException|Must be volatile type"));
        ok(falla(() -> AtomicLongFieldUpdater.newUpdater(Caja.class, "estaticoL"))
                .equals("java.lang.IllegalArgumentException|null"));

        // Referencias: el tipo del campo tiene que ser **exactamente** vclass, y el fallo es un
        // ClassCastException pelado, tambien antes que el chequeo de volatile.
        ok(noHayCampo(falla(() -> AtomicReferenceFieldUpdater.newUpdater(
                Caja.class, String.class, "noExiste")), "noExiste"));
        ok(falla(() -> AtomicReferenceFieldUpdater.newUpdater(Caja.class, String.class, "i"))
                .equals("java.lang.ClassCastException|null"));
        // Ni supertipo ni subtipo valen: Object no sirve para un campo String...
        ok(falla(() -> AtomicReferenceFieldUpdater.newUpdater(Caja.class, Object.class, "s"))
                .equals("java.lang.ClassCastException|null"));
        // ...ni String para un campo Object.
        ok(falla(() -> AtomicReferenceFieldUpdater.newUpdater(Caja.class, String.class, "o"))
                .equals("java.lang.ClassCastException|null"));
        ok(falla(() -> AtomicReferenceFieldUpdater.newUpdater(Caja.class, String.class, "noVolatilS"))
                .equals("java.lang.IllegalArgumentException|Must be volatile type"));
        // Estatico con el vclass correcto: pasa el chequeo de tipo y cae en el de static, que es
        // IllegalArgumentException pelada, igual que en las otras dos clases.
        ok(falla(() -> AtomicReferenceFieldUpdater.newUpdater(Caja.class, String.class, "estaticoS"))
                .equals("java.lang.IllegalArgumentException|null"));

        // Un campo heredado NO se encuentra: es getDeclaredField, no getField.
        ok(noHayCampo(falla(() -> AtomicIntegerFieldUpdater.newUpdater(SubCaja.class, "i")), "i"));

        // Y sobre un objeto que no es del tipo objetivo (null incluido), ClassCastException pelado.
        AtomicIntegerFieldUpdater<Caja> u = AtomicIntegerFieldUpdater.newUpdater(Caja.class, "i");
        ok(falla(() -> u.get(null)).equals("java.lang.ClassCastException|null"));
        ok(falla(() -> u.set(null, 1)).equals("java.lang.ClassCastException|null"));
        ok(falla(() -> u.compareAndSet(null, 0, 1)).equals("java.lang.ClassCastException|null"));

        AtomicReferenceFieldUpdater<Caja, String> r =
                AtomicReferenceFieldUpdater.newUpdater(Caja.class, String.class, "s");
        ok(falla(() -> r.get(null)).equals("java.lang.ClassCastException|null"));
    }

    // ---- concurrencia determinista: N hilos x M incrementos, total exacto ----

    static final int HILOS = 4;
    static final int VUELTAS = 500;

    static void concurrencia() {
        final AtomicIntegerFieldUpdater<Caja> u =
                AtomicIntegerFieldUpdater.newUpdater(Caja.class, "i");
        final AtomicLongFieldUpdater<Caja> v = AtomicLongFieldUpdater.newUpdater(Caja.class, "l");
        final Caja c = new Caja();
        Thread[] hilos = new Thread[HILOS];
        int k = 0;
        while (k < HILOS) {
            hilos[k] = new Thread(() -> {
                int n = 0;
                while (n < VUELTAS) {
                    u.incrementAndGet(c);
                    v.addAndGet(c, 2L);
                    n = n + 1;
                }
            });
            k = k + 1;
        }
        k = 0;
        while (k < HILOS) {
            hilos[k].start();
            k = k + 1;
        }
        k = 0;
        while (k < HILOS) {
            try {
                hilos[k].join();
            } catch (InterruptedException e) {
                ok(false);
                return;
            }
            k = k + 1;
        }
        // Nada de dormir y confiar: el total es exacto o la prueba falla.
        ok(u.get(c) == HILOS * VUELTAS);
        ok(v.get(c) == 2L * HILOS * VUELTAS);
    }

    public static int run() {
        enteros();
        largos();
        referencias();
        rechazos();
        concurrencia();
        return primerFallo;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
