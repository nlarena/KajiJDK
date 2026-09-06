package jdk.jshell.execution;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


/**
 * El agente: el programa que corre en el <strong>otro</strong> proceso y ejecuta los fragmentos.
 *
 * <h2>Como arranca</h2>
 *
 * <p>JShell lanza una maquina virtual cuya clase principal es esta. El {@link #main} se conecta al
 * puerto que le pasaron, arma el canal multiplexado, y se queda atendiendo comandos hasta que le
 * digan que cierre. Del otro lado del socket hay un {@link StreamingExecutionControl}.
 *
 * <h2>Por que redefine puede funcionar aca</h2>
 *
 * <p>Porque este proceso existe solo para ejecutar fragmentos: puede permitirse reemplazar el codigo
 * de una clase sin cuidar a nadie mas. En el motor local, la misma operacion tocaria clases que
 * JShell esta usando.
 *
 * <h2>Por que la salida del usuario esta desviada</h2>
 *
 * <p>{@code System.out} de este proceso no lo ve nadie: la consola es la de JShell, que esta del
 * otro lado. {@link #main} lo reemplaza por una corriente del canal multiplexado, y por eso un
 * {@code System.out.println} en un fragmento aparece donde el usuario lo espera.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>La clase funciona --hereda de {@link DirectExecutionControl}, que anda-- salvo {@link #main},
 * que necesita conectarse por un socket. Esta VM no tiene ese transporte, asi que el agente no se
 * puede lanzar; el motor que si se puede usar es {@link LocalExecutionControl}.
 *
 * @since 9
 */
public class RemoteExecutionControl extends DirectExecutionControl {

    /** Si se pidio cortar y todavia no se atendio. */
    private volatile boolean cortando;

    /**
     * Un agente con ese cargador.
     *
     * @param loaderDelegate quien instala las clases
     */
    public RemoteExecutionControl(LoaderDelegate loaderDelegate) {
        super(loaderDelegate);
    }

    /** Un agente con el cargador por omision. */
    public RemoteExecutionControl() {
        super();
    }

    /**
     * El punto de entrada del proceso agente.
     *
     * <p>Se conecta al puerto que le pasan, desvia la salida y la entrada del usuario por el canal,
     * y atiende hasta que le digan que cierre.
     *
     * @param args el puerto al que conectarse
     * @throws Exception si no se pudo conectar o armar el canal
     */
    public static void main(String[] args) throws Exception {
        final int puerto = Integer.parseInt(args[0]);
        final Socket s = new Socket(InetAddress.getLoopbackAddress(), puerto);
        final InputStream entrada = s.getInputStream();
        final OutputStream salida = s.getOutputStream();
        final Map<String, Consumer<OutputStream>> salidas =
                new HashMap<String, Consumer<OutputStream>>();
        salidas.put("out", new Consumer<OutputStream>() {
            public void accept(OutputStream os) {
                System.setOut(new PrintStream(os, true));
            }
        });
        salidas.put("err", new Consumer<OutputStream>() {
            public void accept(OutputStream os) {
                System.setErr(new PrintStream(os, true));
            }
        });
        final Map<String, Consumer<InputStream>> entradas =
                new HashMap<String, Consumer<InputStream>>();
        entradas.put("in", new Consumer<InputStream>() {
            public void accept(InputStream is) {
                System.setIn(is);
            }
        });
        try {
            Util.forwardExecutionControlAndIO(new RemoteExecutionControl(), entrada, salida,
                    salidas, entradas);
        } finally {
            s.close();
        }
    }

    /**
     * Reemplaza el codigo de esas clases.
     *
     * @param cbcs las clases y su bytecode nuevo
     * @throws ClassInstallException si no se pudieron reemplazar
     * @throws NotImplementedException si no se puede redefinir
     * @throws EngineTerminationException si el motor ya no esta
     */
    @Override
    public void redefine(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        // Reemplazar el codigo de una clase ya cargada necesita la instrumentacion de la VM. Lo que
        // se puede hacer sin ella es dejar la version nueva para las cargas que vengan, que es lo
        // que hace el cargador; los objetos que ya existen conservan el comportamiento anterior.
        load(cbcs);
    }

    /**
     * Corta lo que se este ejecutando.
     *
     * @throws EngineTerminationException si el motor ya no esta
     * @throws InternalException si no se pudo cortar
     */
    @Override
    public void stop() throws EngineTerminationException, InternalException {
        cortando = true;
    }

    /** Aviso de que se va a entrar al codigo del usuario. */
    @Override
    protected void clientCodeEnter() {
        cortando = false;
    }

    /**
     * Aviso de que se salio del codigo del usuario.
     *
     * <p>Si habia un corte pedido y el codigo termino igual, algo no funciono: la peticion se
     * perdio, o el fragmento la ignoro. Se espera un poco --el corte y el retorno pueden cruzarse--
     * y recien despues se lo reporta como problema interno, que es lo que hace el JDK.
     *
     * @throws InternalException si se pidio cortar y el corte no llego a ocurrir
     */
    @Override
    protected void clientCodeLeave() throws InternalException {
        if (!cortando) {
            return;
        }
        for (int i = 0; i < 10 && cortando; i++) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (cortando) {
            cortando = false;
            throw new InternalException("Expected stop exception not encountered.");
        }
    }
}
