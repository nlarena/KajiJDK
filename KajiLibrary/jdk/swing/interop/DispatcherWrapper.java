package jdk.swing.interop;

import java.awt.AccesoDespachador;
import java.awt.EventQueue;
import java.awt.SecondaryLoop;

/**
 * Quien se hace cargo del despacho de eventos de AWT cuando otro juego de herramientas manda.
 *
 * <h2>El problema</h2>
 *
 * <p>JavaFX y AWT tienen cada uno su hilo de eventos. Al incrustar Swing dentro de JavaFX --que es
 * para lo que existe este paquete-- los dos hilos manipularian los mismos componentes, y ninguna
 * cantidad de sincronizacion arregla eso: el orden en que pasan las cosas dejaria de estar definido.
 *
 * <p>La salida es que uno ceda. AWT deja de atender su cola y le pasa cada despacho a este
 * envoltorio, que lo corre en el hilo del otro. Desde ahi hay un solo hilo tocando la interfaz.
 *
 * <h2>Las tres preguntas</h2>
 *
 * <p>{@link #isDispatchThread} porque medio Swing pregunta si esta en el hilo correcto antes de
 * hacer nada. {@link #scheduleDispatch} porque es la unica forma de llegar a ese hilo.
 * {@link #createSecondaryLoop} porque un dialogo modal tiene que bloquear a quien lo abrio sin
 * congelar el hilo de eventos, y el unico que sabe como hacerlo es el que lo maneja.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Funciona: {@link #setFwDispatcher} instala el envoltorio en la cola de verdad, y desde ahi los
 * despachos, la pregunta por el hilo y los bucles secundarios de esa cola pasan por aca.
 *
 * @since 9
 */
public abstract class DispatcherWrapper {

    private final AccesoDespachador.Despachador fwd = new Puente(this);

    /** Uno. */
    public DispatcherWrapper() {
    }

    /**
     * Si el hilo actual es aquel en el que este despachador corre los eventos.
     *
     * @return cierto si lo es
     */
    public abstract boolean isDispatchThread();

    /**
     * Corre eso en el hilo de despacho, cuando pueda.
     *
     * <p>Vuelve enseguida: el que llama esta casi siempre en otro hilo y no puede esperar.
     *
     * @param r que correr
     */
    public abstract void scheduleDispatch(Runnable r);

    /**
     * Un bucle secundario suyo.
     *
     * @return el bucle
     */
    public abstract SecondaryLoop createSecondaryLoop();

    /**
     * Le hace ceder el despacho de esa cola a ese envoltorio.
     *
     * <p>Pasar {@code null} como despachador no devuelve la cola a su hilo propio: el JDK
     * desreferencia el envoltorio antes de mirar la cola, asi que tira. Se lo deja igual porque el
     * unico uso real es instalar, y una llamada con {@code null} es un error de quien llama.
     *
     * @param eventQueue la cola que cede el despacho
     * @param dispatcher quien se hace cargo
     * @throws NullPointerException si alguno de los dos es {@code null}
     */
    public static void setFwDispatcher(EventQueue eventQueue, DispatcherWrapper dispatcher) {
        AccesoDespachador.instalar(eventQueue, dispatcher.fwd);
    }

    /** Lo que ve `java.awt` de este envoltorio; los nombres de alla son otros. */
    private static final class Puente implements AccesoDespachador.Despachador {

        private final DispatcherWrapper d;

        Puente(DispatcherWrapper d) {
            this.d = d;
        }

        public boolean esHiloDeDespacho() {
            return this.d.isDispatchThread();
        }

        public void programarDespacho(Runnable tarea) {
            this.d.scheduleDispatch(tarea);
        }

        public SecondaryLoop bucleSecundario() {
            return this.d.createSecondaryLoop();
        }
    }
}
