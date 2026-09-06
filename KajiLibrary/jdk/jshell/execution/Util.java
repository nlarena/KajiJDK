package jdk.jshell.execution;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import jdk.jshell.spi.ExecutionControl;

/**
 * Las piezas para armar un motor de ejecucion que vive del otro lado de un par de flujos.
 *
 * <h2>Los dos extremos</h2>
 *
 * <p>{@link #forwardExecutionControl} es el lado que <strong>ejecuta</strong>: se queda atendiendo
 * comandos hasta que le digan que cierre. {@link #remoteInputOutput} es el lado de
 * <strong>JShell</strong>: arma el par de flujos y devuelve el motor con el que hablarle.
 *
 * <h2>Por que hay que multiplexar</h2>
 *
 * <p>Entre las dos puntas hay una sola conexion y por ahi tienen que pasar varias corrientes: los
 * comandos, lo que el programa del usuario imprime, su salida de error, y su entrada. Mezclarlas sin
 * etiquetar seria imposible de separar; abrir una conexion por cada una multiplicaria por cuatro lo
 * que hay que atravesar en un cortafuegos. Ver {@link MultiplexingOutputStream}.
 *
 * <h2>El orden en que se abren los flujos</h2>
 *
 * <p>{@link ObjectOutputStream} escribe una cabecera al construirse y {@link ObjectInputStream} la
 * lee al construirse. Si las dos puntas crean primero la entrada, las dos quedan esperando una
 * cabecera que nadie escribio. Por eso siempre se crea primero la salida y se la vacia.
 *
 * <p>Es el error clasico de este tipo de codigo y no da ningun sintoma util: los dos procesos quedan
 * vivos y quietos.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Los tres primeros metodos funcionan: son flujos y hilos. {@link #detectJdiExitEvent} necesita
 * la cola de eventos de una maquina virtual depurada, y esta VM no tiene el transporte de JDI; ver
 * {@link JdiExecutionControl}.
 *
 * @since 9
 */
public class Util {

    /** El nombre de la corriente por la que viajan los comandos. */
    private static final String COMANDOS = "$command";

    private Util() {
    }

    /**
     * Atiende comandos sobre ese par de flujos hasta que llegue el de cierre.
     *
     * <p>No devuelve hasta entonces: es el bucle principal del lado que ejecuta.
     *
     * @param ec el motor que de verdad ejecuta
     * @param inStream por donde llegan los comandos
     * @param outStream por donde se contestan
     */
    public static void forwardExecutionControl(ExecutionControl ec, ObjectInput inStream,
            ObjectOutput outStream) {
        new ExecutionControlForwarder(ec, inStream, outStream).commandLoop();
    }

    /**
     * Arma los flujos del lado que ejecuta y se queda atendiendo.
     *
     * <p>Antes de atender, redirige la salida y la entrada del programa del usuario por el canal
     * compartido: sin eso, lo que el fragmento imprima se perderia en el proceso remoto.
     *
     * @param ec el motor que de verdad ejecuta
     * @param inStream la conexion de entrada
     * @param outStream la conexion de salida
     * @param outputStreamMap que hacer con cada corriente de salida que se quiera publicar
     * @param inputStreamMap que hacer con cada corriente de entrada
     * @throws IOException si no se pudieron armar los flujos
     */
    public static void forwardExecutionControlAndIO(ExecutionControl ec, InputStream inStream,
            OutputStream outStream, Map<String, Consumer<OutputStream>> outputStreamMap,
            Map<String, Consumer<InputStream>> inputStreamMap) throws IOException {
        for (final Map.Entry<String, Consumer<OutputStream>> e : outputStreamMap.entrySet()) {
            e.getValue().accept(multiplexada(e.getKey(), outStream));
        }
        // La salida va primero y se vacia: la cabecera de ObjectOutputStream tiene que llegar antes
        // de que el otro lado intente leer la suya. Ver la nota de la clase.
        final ObjectOutputStream out = new ObjectOutputStream(multiplexada(COMANDOS, outStream));
        out.flush();
        final Map<String, OutputStream> destinos = new HashMap<String, OutputStream>();
        final List<Closeable> cerrar = new ArrayList<Closeable>();
        final PipedInputStream comandosIn = new PipedInputStream();
        final PipedOutputStream comandosOut = new PipedOutputStream(comandosIn);
        destinos.put(COMANDOS, comandosOut);
        cerrar.add(comandosOut);
        for (final Map.Entry<String, Consumer<InputStream>> e : inputStreamMap.entrySet()) {
            final PipedInputStream pin = new PipedInputStream();
            final PipedOutputStream pout = new PipedOutputStream(pin);
            destinos.put(e.getKey(), pout);
            cerrar.add(pout);
            e.getValue().accept(pin);
        }
        new DemultiplexInput(inStream, destinos, cerrar).start();
        final ObjectInputStream in = new ObjectInputStream(comandosIn);
        forwardExecutionControl(ec, in, out);
    }

    /**
     * Arma los flujos del lado de JShell y devuelve el motor con el que hablarle.
     *
     * @param inStream la conexion de entrada
     * @param outStream la conexion de salida
     * @param outputStreamMap a donde mandar cada corriente que llegue
     * @param inputStreamMap de donde sacar cada corriente que se mande
     * @param factory como construir el motor sobre el par de flujos ya armado
     * @return el motor
     * @throws IOException si no se pudieron armar los flujos
     */
    public static ExecutionControl remoteInputOutput(InputStream inStream, OutputStream outStream,
            Map<String, OutputStream> outputStreamMap, Map<String, InputStream> inputStreamMap,
            BiFunction<ObjectInput, ObjectOutput, ExecutionControl> factory) throws IOException {
        final Map<String, OutputStream> destinos =
                new HashMap<String, OutputStream>(outputStreamMap);
        final List<Closeable> cerrar = new ArrayList<Closeable>();
        final PipedInputStream comandosIn = new PipedInputStream();
        final PipedOutputStream comandosOut = new PipedOutputStream(comandosIn);
        destinos.put(COMANDOS, comandosOut);
        cerrar.add(comandosOut);
        for (final Map.Entry<String, InputStream> e : inputStreamMap.entrySet()) {
            copiarEnSegundoPlano(e.getValue(), multiplexada(e.getKey(), outStream));
        }
        final ObjectOutputStream out = new ObjectOutputStream(multiplexada(COMANDOS, outStream));
        out.flush();
        new DemultiplexInput(inStream, destinos, cerrar).start();
        final ObjectInputStream in = new ObjectInputStream(comandosIn);
        return factory.apply(in, out);
    }

    /**
     * Avisa cuando la maquina virtual depurada se termina.
     *
     * <p>Se queda escuchando la cola de eventos en un hilo aparte y llama al informante cuando llega
     * un {@link VMDeathEvent} o un {@link VMDisconnectEvent}. Los dos importan y no son lo mismo: el
     * primero es la otra VM terminando de forma ordenada, el segundo es la conexion cayendose. Para
     * JShell la consecuencia es la misma --no hay mas motor-- y por eso los dos avisan.
     *
     * <p>El hilo es demonio: si lo unico que queda vivo es este vigilante, no hay nada que vigilar.
     *
     * <p>En esta VM la cola no se llena nunca, porque el transporte de JDI es lo que la alimenta y no
     * lo hay. El hilo arranca igual y lo que pase depende de la maquina que le pasen; ver
     * {@link JdiExecutionControl}.
     *
     * @param vm la maquina depurada
     * @param reporter a quien avisarle
     */
    public static void detectJdiExitEvent(VirtualMachine vm, Consumer<String> reporter) {
        final EventQueue cola = vm.eventQueue();
        final Thread t = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    final EventSet conjunto;
                    try {
                        conjunto = cola.remove();
                    } catch (InterruptedException e) {
                        return;
                    } catch (VMDisconnectedException e) {
                        reporter.accept("VM disconnected");
                        return;
                    }
                    for (final Event ev : conjunto) {
                        if (ev instanceof VMDeathEvent) {
                            reporter.accept("VM died");
                            return;
                        }
                        if (ev instanceof VMDisconnectEvent) {
                            reporter.accept("VM disconnected");
                            return;
                        }
                    }
                    conjunto.resume();
                }
            }
        }, "JDI exit watcher");
        t.setDaemon(true);
        t.start();
    }

    private static OutputStream multiplexada(String nombre, OutputStream destino) {
        return new MultiplexingOutputStream(nombre, destino);
    }

    /** Copia un flujo en otro, en un hilo aparte, hasta que se termine. */
    private static void copiarEnSegundoPlano(InputStream de, OutputStream a) {
        final Thread t = new Thread(new Runnable() {
            public void run() {
                final byte[] buf = new byte[1024];
                try {
                    while (true) {
                        final int n = de.read(buf);
                        if (n < 0) {
                            return;
                        }
                        a.write(buf, 0, n);
                        a.flush();
                    }
                } catch (IOException e) {
                    // La corriente se termino; no hay a quien avisarle ni que hacer.
                }
            }
        }, "input copier");
        t.setDaemon(true);
        t.start();
    }
}
