package jdk.jshell.execution;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import jdk.jshell.spi.ExecutionControl;

/**
 * El motor que no ejecuta nada: le pasa cada operacion a otro por un par de flujos.
 *
 * <h2>Que es</h2>
 *
 * <p>La punta de JShell del protocolo. Cada metodo escribe el nombre del comando y sus argumentos,
 * vacia el flujo, y lee la respuesta. Del otro lado hay un {@link ExecutionControlForwarder} que
 * hace lo inverso y termina llamando a un motor de verdad --normalmente un
 * {@link RemoteExecutionControl} en otro proceso--.
 *
 * <h2>Por que cada respuesta empieza con una marca</h2>
 *
 * <p>Porque por el mismo flujo viaja tambien lo que el programa del usuario imprime. Sin
 * {@code COMMAND_PREFIX} delante, un {@code System.out.println("CMD_LOAD")} del usuario seria
 * indistinguible de una respuesta. Ver {@link RemoteCodes}.
 *
 * <h2>El {@code null} que viaja como texto</h2>
 *
 * <p>{@code writeUTF} no sabe escribir {@code null}, y los valores del usuario pueden serlo. El
 * protocolo usa una cadena centinela con caracteres de control, que ningun {@code toString} razonable
 * produce. Es un compromiso conocido y esta escrito asi en el JDK; se lo reproduce igual porque el
 * otro lado puede ser el agente del JDK.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Funciona. El protocolo es lectura y escritura sobre flujos, y no necesita nada mas. La prueba
 * {@code java/JSH2.java} lo corre de punta a punta contra un {@link DirectExecutionControl} del
 * mismo proceso, con dos tuberias en el medio.
 *
 * @since 9
 */
public class StreamingExecutionControl implements ExecutionControl {

    private final ObjectOutput out;
    private final ObjectInput in;

    /**
     * Un motor sobre ese par de flujos.
     *
     * @param out por donde se mandan los comandos
     * @param in por donde llegan las respuestas
     */
    public StreamingExecutionControl(ObjectOutput out, ObjectInput in) {
        this.out = out;
        this.in = in;
    }

    /**
     * Instala esas clases del otro lado.
     *
     * @param cbcs los nombres y el bytecode de cada una
     * @throws ClassInstallException si alguna no se pudo instalar
     * @throws NotImplementedException si el otro motor no sabe instalar
     * @throws EngineTerminationException si se corto la comunicacion
     */
    @Override
    public void load(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        try {
            writeCommand(RemoteCodes.CMD_LOAD);
            out.writeObject(cbcs);
            out.flush();
            leerResultadoDeInstalacion();
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    /**
     * Reemplaza el codigo de esas clases del otro lado.
     *
     * @param cbcs las clases y su bytecode nuevo
     * @throws ClassInstallException si no se pudieron reemplazar
     * @throws NotImplementedException si el otro motor no sabe redefinir
     * @throws EngineTerminationException si se corto la comunicacion
     */
    @Override
    public void redefine(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        try {
            writeCommand(RemoteCodes.CMD_REDEFINE);
            out.writeObject(cbcs);
            out.flush();
            leerResultadoDeInstalacion();
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    /**
     * Llama a ese metodo del otro lado.
     *
     * @param className la clase
     * @param methodName el metodo
     * @return la representacion del resultado
     * @throws RunException si el codigo del usuario fallo
     * @throws EngineTerminationException si se corto la comunicacion
     * @throws InternalException si fallo el otro motor
     */
    @Override
    public String invoke(String className, String methodName)
            throws RunException, EngineTerminationException, InternalException {
        try {
            writeCommand(RemoteCodes.CMD_INVOKE);
            out.writeUTF(className);
            out.writeUTF(methodName);
            out.flush();
            leerResultadoDeEjecucion();
            return in.readUTF();
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    /**
     * Lee el valor de una variable del otro lado.
     *
     * @param className la clase
     * @param varName la variable
     * @return la representacion del valor, o {@code null}
     * @throws RunException si el codigo del usuario fallo
     * @throws EngineTerminationException si se corto la comunicacion
     * @throws InternalException si fallo el otro motor
     */
    @Override
    public String varValue(String className, String varName)
            throws RunException, EngineTerminationException, InternalException {
        try {
            writeCommand(RemoteCodes.CMD_VAR_VALUE);
            out.writeUTF(className);
            out.writeUTF(varName);
            out.flush();
            leerResultadoDeEjecucion();
            return leerTextoONulo();
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    /**
     * Agrega una entrada al camino de busqueda del otro lado.
     *
     * @param path la entrada
     * @throws EngineTerminationException si se corto la comunicacion
     * @throws InternalException si no se pudo agregar
     */
    @Override
    public void addToClasspath(String path) throws EngineTerminationException, InternalException {
        try {
            writeCommand(RemoteCodes.CMD_ADD_CLASSPATH);
            out.writeUTF(path);
            out.flush();
            leerResultadoSimple();
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    /**
     * Le pide al otro lado que corte lo que este ejecutando.
     *
     * <p>Este comando se manda mientras el otro lado esta ocupado con un {@code invoke}, asi que no
     * espera respuesta: la respuesta que va a llegar es la del {@code invoke} que se corto.
     *
     * @throws EngineTerminationException si se corto la comunicacion
     * @throws InternalException si no se pudo mandar
     */
    @Override
    public void stop() throws EngineTerminationException, InternalException {
        try {
            writeCommand(RemoteCodes.CMD_STOP);
            out.flush();
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    /**
     * Una operacion propia del otro motor.
     *
     * @param command el nombre de la operacion
     * @param arg su argumento
     * @return lo que devuelva
     * @throws RunException si el codigo del usuario fallo
     * @throws EngineTerminationException si se corto la comunicacion
     * @throws InternalException si fallo el otro motor
     */
    @Override
    public Object extensionCommand(String command, Object arg)
            throws RunException, EngineTerminationException, InternalException {
        try {
            writeCommand(command);
            out.writeObject(arg);
            out.flush();
            leerResultadoDeEjecucion();
            return in.readObject();
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        } catch (ClassNotFoundException e) {
            throw new InternalException(String.valueOf(e));
        }
    }

    /**
     * Cierra la comunicacion.
     *
     * <p>Manda el comando de cierre y no mira si llego: si el otro lado ya se murio, no hay nada que
     * hacer con esa noticia, y {@code close} no puede fallar.
     */
    @Override
    public void close() {
        try {
            writeCommand(RemoteCodes.CMD_CLOSE);
            out.flush();
        } catch (IOException e) {
            // El otro lado ya no esta. Es exactamente lo que se queria conseguir.
        }
    }

    private void writeCommand(String cmd) throws IOException {
        out.writeInt(RemoteCodes.COMMAND_PREFIX);
        out.writeUTF(cmd);
    }

    /** Un texto que puede ser {@code null}; ver la nota de la clase. */
    private String leerTextoONulo() throws IOException {
        final String s = in.readUTF();
        return RemoteCodes.NULO.equals(s) ? null : s;
    }

    /** La respuesta de una operacion que no devuelve nada. */
    private void leerResultadoSimple() throws EngineTerminationException, InternalException {
        try {
            final int codigo = in.readInt();
            // Cadena de `if` y no un `switch`: las constantes vienen de otro archivo
            // compilado, y ahi la etiqueta de un `case` no se pliega (hallazgo #503).
            if (codigo == RemoteCodes.RESULT_SUCCESS) {
                return;
            }
            if (codigo == RemoteCodes.RESULT_INTERNAL_PROBLEM) {
                throw new InternalException(in.readUTF());
            }
            if (codigo == RemoteCodes.RESULT_TERMINATED) {
                throw new EngineTerminationException(in.readUTF());
            }
            throw new EngineTerminationException("Bad remote result code: " + codigo);
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    /** La respuesta de {@code load} y {@code redefine}. */
    private void leerResultadoDeInstalacion()
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        try {
            final int codigo = in.readInt();
            // Cadena de `if` y no un `switch`: las constantes vienen de otro archivo
            // compilado, y ahi la etiqueta de un `case` no se pliega (hallazgo #503).
            if (codigo == RemoteCodes.RESULT_SUCCESS) {
                return;
            }
            if (codigo == RemoteCodes.RESULT_NOT_IMPLEMENTED) {
                throw new NotImplementedException(in.readUTF());
            }
            if (codigo == RemoteCodes.RESULT_CLASS_INSTALL_EXCEPTION) {
                throw new ClassInstallException(in.readUTF(), (boolean[]) in.readObject());
            }
            if (codigo == RemoteCodes.RESULT_TERMINATED) {
                throw new EngineTerminationException(in.readUTF());
            }
            throw new EngineTerminationException("Bad remote result code: " + codigo);
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        } catch (ClassNotFoundException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    /**
     * La respuesta de una operacion que ejecuta codigo del usuario.
     *
     * <p>El caso encadenado --{@code RESULT_USER_EXCEPTION_CHAINED}-- trae la excepcion y despues,
     * una tras otra, sus causas, hasta un {@code RESULT_SUCCESS} que hace de terminador. Se las
     * enlaza con {@code initCause} en el orden en que llegan; sin eso, del otro lado se veria la
     * excepcion de arriba sin nada abajo, que es justo lo que no sirve para entender que paso.
     */
    private void leerResultadoDeEjecucion()
            throws RunException, EngineTerminationException, InternalException {
        try {
            final int codigo = in.readInt();
            // Cadena de `if` y no un `switch`: las constantes vienen de otro archivo
            // compilado, y ahi la etiqueta de un `case` no se pliega (hallazgo #503).
            if (codigo == RemoteCodes.RESULT_SUCCESS) {
                return;
            }
            if (codigo == RemoteCodes.RESULT_NOT_IMPLEMENTED) {
                throw new NotImplementedException(in.readUTF());
            }
            if (codigo == RemoteCodes.RESULT_USER_EXCEPTION) {
                throw leerExcepcionDeUsuario();
            }
            if (codigo == RemoteCodes.RESULT_CORRALLED) {
                throw leerExcepcionDeResolucion();
            }
            if (codigo == RemoteCodes.RESULT_USER_EXCEPTION_CHAINED) {
                throw leerCadena();
            }
            if (codigo == RemoteCodes.RESULT_INTERNAL_PROBLEM) {
                throw new InternalException(in.readUTF());
            }
            if (codigo == RemoteCodes.RESULT_STOPPED) {
                throw new StoppedException();
            }
            if (codigo == RemoteCodes.RESULT_TERMINATED) {
                throw new EngineTerminationException(in.readUTF());
            }
            throw new EngineTerminationException("Bad remote result code: " + codigo);
        } catch (EOFException e) {
            throw new EngineTerminationException(String.valueOf(e));
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        } catch (ClassNotFoundException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    private RunException leerCadena() throws IOException, ClassNotFoundException,
            EngineTerminationException {
        in.readInt();
        final RunException primera = leerExcepcionDeUsuario();
        RunException ultima = primera;
        while (true) {
            final int c = in.readInt();
            final RunException causa;
            if (c == RemoteCodes.RESULT_USER_EXCEPTION) {
                causa = leerExcepcionDeUsuario();
            } else if (c == RemoteCodes.RESULT_CORRALLED) {
                causa = leerExcepcionDeResolucion();
            } else if (c == RemoteCodes.RESULT_SUCCESS) {
                return primera;
            } else {
                throw new EngineTerminationException("Bad chained remote result code: " + c);
            }
            ultima.initCause(causa);
            ultima = causa;
        }
    }

    private UserException leerExcepcionDeUsuario() throws IOException, ClassNotFoundException {
        final String mensaje = in.readUTF();
        final String clase = in.readUTF();
        return new UserException(mensaje, clase, (StackTraceElement[]) in.readObject());
    }

    private ResolutionException leerExcepcionDeResolucion()
            throws IOException, ClassNotFoundException {
        final int id = in.readInt();
        return new ResolutionException(id, (StackTraceElement[]) in.readObject());
    }
}
