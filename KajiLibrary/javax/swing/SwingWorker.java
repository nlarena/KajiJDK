package javax.swing;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Una tarea larga que corre fuera del hilo de la interfaz y va contando como le va.
 *
 * <p>Resuelve el problema mas viejo de una interfaz grafica: el hilo que dibuja es uno solo, y
 * cualquier cosa que tarde --leer un archivo, consultar una base-- lo congela. Pero mover la tarea
 * a otro hilo no alcanza, porque **los componentes no se pueden tocar desde otro hilo**. Hacen
 * falta las dos mitades, y eso son los dos parametros de tipo:
 *
 * <ul>
 *   <li>`T` es el resultado final: lo devuelve {@link #doInBackground} y lo recoge {@link #get}.
 *   <li>`V` son los avances intermedios: {@link #publish} los manda desde el hilo de fondo y
 *       {@link #process} los recibe --en el JDK, ya en el hilo de la interfaz.
 * </ul>
 *
 * <p>Y por eso {@link #done} existe aparte de `doInBackground`: es el gancho para tocar la interfaz
 * cuando termino.
 *
 * <p>Un `SwingWorker` **se usa una sola vez**. {@link #execute} llamado dos veces no vuelve a
 * correr nada: el estado va de `PENDING` a `STARTED` a `DONE` y no vuelve.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Todo lo que es concurrencia esta hecho de verdad: la tarea corre en un pool, el resultado
 * viaja por un {@link FutureTask}, {@link #cancel} y {@link #get} son los de siempre, y el progreso
 * y el estado se avisan por {@link PropertyChangeSupport}.
 *
 * <p>Lo que **no** esta es el salto al hilo de la interfaz. En el JDK, `process`, `done` y los
 * avisos de propiedad se ejecutan en el EDT, y `publish` ademas acumula los avances para no
 * inundarlo. Esta biblioteca no tiene EDT, asi que `process` y `done` corren **en el hilo de
 * fondo** y cada `publish` llega entero. Esta documentado en cada metodo: quien escriba un
 * `SwingWorker` contra esta biblioteca tiene que saber que no hereda la seguridad de hilo que el
 * nombre promete.
 */
public abstract class SwingWorker<T, V> implements RunnableFuture<T> {

    /** En que anda el trabajo. */
    public enum StateValue {

        /** Creado, todavia no arrancado. */
        PENDING,

        /** Corriendo. */
        STARTED,

        /** Terminado, cancelado o roto: en cualquier caso, no va a hacer nada mas. */
        DONE
    }

    /** Cuantos trabajos pueden correr a la vez. El mismo numero que usa el JDK. */
    private static final int MAX_WORKER_THREADS = 10;

    private static ExecutorService executorService;

    private volatile int progress;
    private volatile StateValue state = StateValue.PENDING;
    private final FutureTask<T> future;
    private final PropertyChangeSupport propertyChangeSupport;

    /** Un trabajo sin arrancar. */
    public SwingWorker() {
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.future = new Tarea<T>(this, new Cuerpo<T>(this));
    }

    /**
     * El trabajo largo. Lo escribe la subclase.
     *
     * <p>Corre en un hilo de fondo, asi que **no puede tocar componentes**. Lo que tenga que
     * mostrarse sale por {@link #publish} o se devuelve y se recoge en {@link #done}.
     *
     * @return el resultado
     * @throws Exception lo que sea que falle; sale despues envuelto por {@link #get}
     */
    protected abstract T doInBackground() throws Exception;

    /** Corre el trabajo en el hilo actual. Lo llama el pool; no se llama a mano. */
    public final void run() {
        this.future.run();
    }

    /**
     * Manda avances intermedios a {@link #process}.
     *
     * <p>Se llama desde {@link #doInBackground}. En el JDK los avances se acumulan y llegan en
     * lotes al hilo de la interfaz; aca llegan uno por llamada y en el mismo hilo. Ver la nota de
     * la clase.
     */
    @SafeVarargs
    protected final void publish(V... chunks) {
        // El local intermedio es un rodeo por #285: `new ArrayList<V>(Arrays.asList(chunks))` en
        // una sola expresion no compila porque `V` es variable de **la clase**. Nombrar el tipo es
        // justo lo que la inferencia no dedujo. Sacarlo cuando #285 se cierre.
        List<V> lote = Arrays.asList(chunks);
        process(new ArrayList<V>(lote));
    }

    /**
     * Recibe los avances de {@link #publish}. Lo redefine la subclase.
     *
     * <p>En el JDK corre en el hilo de la interfaz; aca, en el de fondo. Ver la nota de la clase.
     */
    protected void process(List<V> chunks) {
    }

    /**
     * Se llama cuando {@link #doInBackground} termino --bien, mal o cancelado.
     *
     * <p>Es donde va lo que toca la interfaz. En el JDK corre en el hilo de la interfaz; aca, en el
     * de fondo. Ver la nota de la clase.
     */
    protected void done() {
    }

    /**
     * Fija el avance, entre 0 y 100, y avisa a los escuchas de la propiedad {@code "progress"}.
     *
     * <p>Si el valor no cambia no se avisa: un trabajo que informa el mismo numero mil veces no
     * tiene por que despertar a nadie.
     *
     * @throws IllegalArgumentException si esta fuera de 0..100
     */
    protected final void setProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("the value should be from 0 to 100");
        }
        int viejo;
        synchronized (this) {
            if (this.progress == progress) {
                return;
            }
            viejo = this.progress;
            this.progress = progress;
        }
        firePropertyChange("progress", Integer.valueOf(viejo), Integer.valueOf(progress));
    }

    /** El avance informado, entre 0 y 100. */
    public final int getProgress() {
        return this.progress;
    }

    /**
     * Arranca el trabajo en un hilo de fondo.
     *
     * <p>Vuelve en el acto. Llamarlo dos veces no arranca nada la segunda.
     */
    public final void execute() {
        getWorkersExecutorService().execute(this);
    }

    /**
     * Cancela el trabajo.
     *
     * @param mayInterruptIfRunning si se puede interrumpir el hilo que lo esta corriendo
     * @return `false` si ya habia terminado o ya estaba cancelado
     */
    public final boolean cancel(boolean mayInterruptIfRunning) {
        return this.future.cancel(mayInterruptIfRunning);
    }

    /** Si se cancelo antes de terminar. */
    public final boolean isCancelled() {
        return this.future.isCancelled();
    }

    /** Si ya no va a hacer nada mas: termino, se rompio o se cancelo. */
    public final boolean isDone() {
        return this.future.isDone();
    }

    /**
     * El resultado, esperando a que este.
     *
     * <p><b>Bloquea.</b> Llamarlo desde el hilo de la interfaz la congela, que es exactamente lo
     * que esta clase existe para evitar: el lugar de `get` es {@link #done}, donde ya se sabe que
     * el resultado esta.
     *
     * @throws InterruptedException si interrumpen la espera
     * @throws ExecutionException si {@link #doInBackground} lanzo
     */
    public final T get() throws InterruptedException, ExecutionException {
        return this.future.get();
    }

    /**
     * El resultado, esperando a lo sumo ese plazo.
     *
     * @throws InterruptedException si interrumpen la espera
     * @throws ExecutionException si {@link #doInBackground} lanzo
     * @throws TimeoutException si se vence el plazo
     */
    public final T get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return this.future.get(timeout, unit);
    }

    /** Agrega un escucha de las propiedades {@code "state"} y {@code "progress"}. */
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /** Saca un escucha. */
    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Avisa un cambio de propiedad.
     *
     * <p>En el JDK el aviso llega en el hilo de la interfaz; aca, en el que lo dispara.
     */
    public final void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        getPropertyChangeSupport().firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * El soporte de propiedades, para agregar escuchas de una propiedad concreta.
     *
     * <p>Esta expuesto porque {@link #addPropertyChangeListener} solo agrega escuchas generales, y
     * un cliente que solo quiere el progreso tiene derecho a no despertarse por el estado.
     */
    public final PropertyChangeSupport getPropertyChangeSupport() {
        return this.propertyChangeSupport;
    }

    /** En que anda el trabajo. */
    public final StateValue getState() {
        return isDone() ? StateValue.DONE : this.state;
    }

    /** Cambia el estado y lo avisa como la propiedad {@code "state"}. */
    private void setState(StateValue state) {
        StateValue viejo = this.state;
        this.state = state;
        firePropertyChange("state", viejo, state);
    }

    /** Lo llama {@link Tarea} cuando el `FutureTask` termina, sea como sea. */
    private void alTerminar() {
        setState(StateValue.DONE);
        done();
    }

    /** El pool compartido, armado la primera vez que alguien lo necesita. */
    private static synchronized ExecutorService getWorkersExecutorService() {
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(MAX_WORKER_THREADS);
        }
        return executorService;
    }

    /**
     * El cuerpo del trabajo, como {@link Callable}.
     *
     * <p>Es una clase con nombre y no anonima --el JDK la tiene anonima-- porque asi se lee: lo
     * unico que hace es marcar el arranque y delegar.
     */
    private static final class Cuerpo<T> implements Callable<T> {

        private final SwingWorker<T, ?> duenio;

        Cuerpo(SwingWorker<T, ?> duenio) {
            this.duenio = duenio;
        }

        public T call() throws Exception {
            this.duenio.setState(StateValue.STARTED);
            return this.duenio.doInBackground();
        }
    }

    /** El {@link FutureTask} que avisa al trabajo cuando termina. */
    private static final class Tarea<T> extends FutureTask<T> {

        private final SwingWorker<T, ?> duenio;

        Tarea(SwingWorker<T, ?> duenio, Callable<T> cuerpo) {
            super(cuerpo);
            this.duenio = duenio;
        }

        protected void done() {
            this.duenio.alTerminar();
        }
    }
}
