package java.util.concurrent.locks;

// A condition variable factored out of a {@link Lock} — the {@code Object.wait}/{@code
// notify} of the explicit-lock world. A thread {@code await}s a condition (atomically
// releasing the lock and parking); another thread {@code signal}s it (waking a waiter,
// which re-acquires the lock before returning). The caller must hold the owning lock.
//
// The timed forms ({@code awaitNanos}, {@code await(long,TimeUnit)}) and {@code
// awaitUntil(Date)} are intentionally omitted for now: distinguishing a signal from a
// deadline cleanly needs a per-waiter signalled flag not yet built, and {@code awaitUntil}
// needs {@code java.util.Date}. The four core methods below are complete.
public interface Condition {

    // Release the lock and wait until signalled, then re-acquire the lock.
    void await() throws InterruptedException;

    // Like {@link #await}, but not responsive to interruption (a no-op distinction on
    // KajiJDK, which has none — kept for source compatibility).
    void awaitUninterruptibly();

    // Wake one thread waiting on this condition.
    /**
     * Espera hasta que la senialen o pase `time`.
     *
     * @return `false` si el plazo se agoto antes de la senial
     */
    boolean await(long time, java.util.concurrent.TimeUnit unit)
            throws InterruptedException;

    /**
     * Espera hasta que la senialen o pasen `nanosTimeout` nanosegundos.
     *
     * <p>Devuelve **lo que sobro del plazo**, y ese detalle es el que la hace util en un bucle: una
     * espera puede despertar sin senial --un *spurious wakeup*-- y hay que volver a esperar, pero
     * solo por el resto. Con un `boolean` no se puede, porque no se sabe cuanto paso.
     *
     * @return los nanosegundos que sobraron; cero o menos si el plazo se agoto
     */
    long awaitNanos(long nanosTimeout) throws InterruptedException;

    /**
     * Espera hasta que la senialen o llegue `deadline`.
     *
     * @return `false` si la fecha llego antes de la senial
     */
    boolean awaitUntil(java.util.Date deadline) throws InterruptedException;

    void signal();

    // Wake all threads waiting on this condition.
    void signalAll();
}
