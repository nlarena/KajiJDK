package java.util.concurrent;

// A handle on a result that does not exist yet: the task was handed to an executor, and
// this is the receipt. {@link #get} blocks until the answer is there, turning an
// asynchronous submission back into a synchronous read at the moment the caller needs it.
public interface Future<V> {

    // Where a task stands, for a caller that does not want to block to find out. The four
    // constants are the JDK's, and the order matters: a Future is RUNNING until it is done,
    // and exactly one of the three terminal states afterwards.
    enum State {
        // Not finished yet.
        RUNNING,
        // Finished and produced a value.
        SUCCESS,
        // Finished by throwing.
        FAILED,
        // Cancelled before it could produce a value.
        CANCELLED
    }

    // Attempt to cancel; reports whether it succeeded (a finished task cannot be).
    boolean cancel(boolean mayInterruptIfRunning);

    boolean isCancelled();

    boolean isDone();

    // The result, waiting for it if the task has not finished. Raises the task's own
    // failure wrapped in an ExecutionException.
    V get() throws InterruptedException, ExecutionException;

    // The result, waiting at most the given time.
    V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * Where the task stands, without blocking.
     *
     * <p>Written as a {@code default} rather than an abstract method for the same reason as in the
     * JDK: it is derivable from the three predicates every implementor already writes, so adding it
     * costs no implementor anything. The one call to {@link #get} it makes cannot block, because it
     * happens only after {@link #isDone} has said the answer is already there.
     *
     * <p>The JDK retries {@code get} in a loop around {@code InterruptedException}; here the catch
     * re-marks the thread and answers once. The loop exists there to survive an interrupt flag that
     * was already set on entry, and this library's {@code get} does not consult that flag when the
     * task is already done -- so a second attempt would take the same branch as the first.
     */
    default State state() {
        State s;
        if (!isDone()) {
            s = State.RUNNING;
        } else if (isCancelled()) {
            s = State.CANCELLED;
        } else {
            try {
                get();
                s = State.SUCCESS;
            } catch (InterruptedException e) {
                // `state()` no declara `throws`, y el JDK tampoco: es una consulta, no una espera.
                // Que `get()` pueda bloquear aca es solo porque la tarea ya termino --se llego por
                // `isDone()`-- asi que la interrupcion no deberia llegar nunca. Si llega, se remarca
                // el hilo y se reporta lo unico que se sabe: que la tarea corrio.
                Thread.currentThread().interrupt();
                s = State.RUNNING;
            } catch (CancellationException e) {
                // A cancellation that landed between isDone() and get(); still CANCELLED.
                s = State.CANCELLED;
            } catch (ExecutionException e) {
                s = State.FAILED;
            }
        }
        return s;
    }

    /**
     * The result of a task already known to have succeeded.
     *
     * @throws IllegalStateException if the task is unfinished, failed or cancelled -- asking those
     *         for a value is a bug in the caller, and returning null would hide it
     */
    default V resultNow() {
        if (!isDone()) {
            throw new IllegalStateException("Task has not completed");
        }
        V value = null;
        String failure = null;
        try {
            value = get();
        } catch (InterruptedException e) {
            // Misma razon que en state(): se llego aca con la tarea ya terminada, asi que get() no
            // espera y la interrupcion no deberia llegar. Si llega, se remarca el hilo -- tragarsela
            // seria peor que el problema -- y se reporta como "sin completar", que es lo unico que
            // este metodo sabe decir sin inventar un valor.
            Thread.currentThread().interrupt();
            failure = "Task has not completed";
        } catch (CancellationException e) {
            failure = "Task was cancelled";
        } catch (ExecutionException e) {
            failure = "Task completed with exception";
        }
        if (failure != null) {
            throw new IllegalStateException(failure);
        }
        return value;
    }

    /**
     * The exception a task already known to have failed threw -- the cause, not the
     * {@link ExecutionException} wrapper, which is an artefact of {@code get} and not of the task.
     *
     * @throws IllegalStateException if the task is unfinished, cancelled, or succeeded
     */
    default Throwable exceptionNow() {
        if (!isDone()) {
            throw new IllegalStateException("Task has not completed");
        }
        if (isCancelled()) {
            throw new IllegalStateException("Task was cancelled");
        }
        Throwable cause = null;
        String failure = "Task completed with a result";
        try {
            get();
        } catch (InterruptedException e) {
            // Igual que en state() y resultNow(): la tarea ya termino, get() no espera.
            Thread.currentThread().interrupt();
            failure = "Task has not completed";
        } catch (ExecutionException e) {
            cause = e.getCause();
            failure = null;
        }
        if (failure != null) {
            throw new IllegalStateException(failure);
        }
        return cause;
    }
}
