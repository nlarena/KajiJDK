// wait(timeout): un wait temporizado vuelve al vencer el plazo aunque NADIE notifique.
// Single-thread: el hilo toma el monitor, hace wait(50) (sin notificador), el plazo vence,
// re-adquiere el monitor y sigue → devuelve 7. (Nuestro Object.wait(long) no declara
// InterruptedException, así que no hace falta try/catch — eso llega con los interrupts.)
public class WaitTimeout {
    static int run() {
        Object lock = new Object();
        synchronized (lock) {
            lock.wait(50); // sin notify → expira por timeout (~50ms en OS, 50 ticks en green)
        }
        return 7;
    }
}
