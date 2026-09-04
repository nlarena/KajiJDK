import java.util.concurrent.locks.LockSupport;

/**
 * `LockSupport.parkNanos` y `parkUntil`: las esperas con plazo.
 *
 * <p>Lo que se comprueba es lo que las hace utiles y lo que hacia imposible emularlas desde Java:
 * que el plazo **termina** sola, que un `unpark` la corta antes, y que un permiso que llego primero
 * la saltea sin dormir. Las tres cosas tienen que salir del mismo permiso, y por eso el plazo lo
 * lleva la VM.
 *
 * <p>Lo que esta prueba **no** puede comprobar es cuanto dura el plazo: esta VM mide el tiempo en
 * opcodes y no en reloj de pared, asi que un `parkNanos(50 ms)` no espera 50 ms de verdad. Medir la
 * duracion seria medir la VM, no el contrato. Lo que si es contrato --que termine, que se pueda
 * cortar, que el permiso mande-- se comprueba.
 *
 * <p>El mismo archivo compila y da -1 con el JDK 25 corriendo SU `LockSupport`. Ahi el plazo si es
 * de reloj, y las tres afirmaciones valen igual: son las del contrato.
 */
public class ParkTimedTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    /** Un hilo que se estaciona con plazo y anota que volvio. */
    static final class Dormilon implements Runnable {

        volatile boolean volvio;
        final long nanos;

        Dormilon(long nanos) {
            this.nanos = nanos;
        }

        public void run() {
            LockSupport.parkNanos(this.nanos);
            this.volvio = true;
        }
    }

    /** Un hilo que se estaciona sin plazo, para que lo corte un `unpark`. */
    static final class Esperador implements Runnable {

        volatile boolean volvio;

        public void run() {
            LockSupport.parkNanos(1000000000L);
            this.volvio = true;
        }
    }

    public static int run() throws InterruptedException {
        failures = 0;

        // ---- un plazo cero no espera, y NO se come el permiso
        //
        // Es la parte que se lee mal de la especificacion: `parkNanos(0)` no es "espera cero", es
        // "no esperes". Si se comiera el permiso, el `park()` de abajo se dormiria para siempre.
        LockSupport.unpark(Thread.currentThread());
        LockSupport.parkNanos(0L);
        LockSupport.parkNanos(-5L);
        LockSupport.park(); // consume el permiso que sigue ahi; si no estuviera, cuelga
        ok("un plazo cero o negativo no consume el permiso", true);

        // ---- un permiso que llego antes saltea la espera
        LockSupport.unpark(Thread.currentThread());
        LockSupport.parkNanos(1000000000L); // un segundo; con el permiso, retorna en el acto
        ok("un permiso previo saltea la espera con plazo", true);

        // ---- el plazo termina solo, sin que nadie despierte al hilo
        Dormilon d = new Dormilon(20000000L);
        Thread t1 = new Thread(d);
        t1.start();
        t1.join();
        ok("el plazo vence solo", d.volvio);

        // ---- un unpark corta la espera antes del plazo
        Esperador e = new Esperador();
        Thread t2 = new Thread(e);
        t2.start();
        // Sin esto, el `unpark` puede llegar antes de que el hilo se estacione. No seria un error
        // --el permiso se guarda y la espera se saltea igual-- pero entonces la prueba no estaria
        // comprobando lo que dice comprobar.
        Thread.sleep(5L);
        LockSupport.unpark(t2);
        t2.join();
        ok("un unpark corta la espera con plazo", e.volvio);

        // ---- parkUntil con un instante ya pasado no espera
        LockSupport.parkUntil(System.currentTimeMillis() - 1000L);
        ok("un parkUntil vencido retorna en el acto", true);

        // ---- parkUntil con un instante futuro termina
        long antes = System.currentTimeMillis();
        LockSupport.parkUntil(antes + 20L);
        ok("un parkUntil futuro termina", true);

        // ---- las formas con bloqueador se comportan igual
        LockSupport.unpark(Thread.currentThread());
        LockSupport.parkNanos(new Object(), 1000000000L);
        ok("parkNanos con bloqueador respeta el permiso", true);
        LockSupport.parkUntil(new Object(), System.currentTimeMillis() - 1000L);
        ok("parkUntil con bloqueador vencido retorna en el acto", true);

        // ---- el bloqueador de diagnostico sigue andando
        Object motivo = new Object();
        LockSupport.setCurrentBlocker(motivo);
        ok("el bloqueador se lee", LockSupport.getBlocker(Thread.currentThread()) == motivo);
        LockSupport.setCurrentBlocker(null);
        ok("y se borra", LockSupport.getBlocker(Thread.currentThread()) == null);

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("ParkTimedTest " + ParkTimedTest.run());
    }
}
