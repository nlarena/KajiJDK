package jdk.jshell.execution;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControl.ClassBytecodes;
import jdk.jshell.spi.ExecutionControl.ClassInstallException;
import jdk.jshell.spi.ExecutionControl.EngineTerminationException;
import jdk.jshell.spi.ExecutionControl.InternalException;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;
import jdk.jshell.spi.ExecutionControl.ResolutionException;
import jdk.jshell.spi.ExecutionControl.RunException;
import jdk.jshell.spi.ExecutionControl.StoppedException;
import jdk.jshell.spi.ExecutionControl.UserException;

/**
 * El otro extremo de {@link StreamingExecutionControl}: lee comandos y los ejecuta.
 *
 * <h2>Que hace</h2>
 *
 * <p>Un bucle: lee la marca, lee el nombre del comando, lee sus argumentos, se lo pide al motor de
 * verdad, y escribe la respuesta. Es el que corre dentro del proceso que ejecuta los fragmentos.
 *
 * <h2>Por que las excepciones viajan desarmadas</h2>
 *
 * <p>Una excepcion del usuario no se serializa: podria ser de una clase que el otro lado no conoce
 * --la acaba de escribir el usuario-- y deserializarla alla fallaria. Se manda el mensaje, el nombre
 * de la clase y la traza, que es lo que JShell necesita para mostrarla.
 *
 * <p>Las causas van encadenadas una tras otra, terminadas por un {@code RESULT_SUCCESS}. Sin eso,
 * del otro lado se veria la excepcion de arriba sin nada abajo, que es justo lo que no sirve.
 */
final class ExecutionControlForwarder {

    private final ExecutionControl ec;
    private final ObjectInput in;
    private final ObjectOutput out;

    ExecutionControlForwarder(ExecutionControl ec, ObjectInput in, ObjectOutput out) {
        this.ec = ec;
        this.in = in;
        this.out = out;
    }

    /**
     * Atiende comandos hasta que llegue el de cierre o se corte el flujo.
     *
     * <p>No propaga nada: es el bucle principal del proceso que ejecuta, y no hay a quien contarle
     * un problema. Lo que puede es dejar de atender, que es lo que hace.
     */
    void commandLoop() {
        try {
            while (true) {
                if (in.readInt() != RemoteCodes.COMMAND_PREFIX) {
                    // El flujo se desincronizo: seguir leyendo daria comandos inventados.
                    return;
                }
                final String cmd = in.readUTF();
                if (RemoteCodes.CMD_CLOSE.equals(cmd)) {
                    ec.close();
                    return;
                }
                if (!atender(cmd)) {
                    return;
                }
            }
        } catch (IOException e) {
            // El otro lado se fue. No hay nada que responder ni a quien.
        }
    }

    /** Atiende un comando; devuelve falso si hay que dejar de atender. */
    private boolean atender(String cmd) throws IOException {
        try {
            if (RemoteCodes.CMD_LOAD.equals(cmd)) {
                ec.load((ClassBytecodes[]) in.readObject());
                exito();
            } else if (RemoteCodes.CMD_REDEFINE.equals(cmd)) {
                ec.redefine((ClassBytecodes[]) in.readObject());
                exito();
            } else if (RemoteCodes.CMD_INVOKE.equals(cmd)) {
                final String clase = in.readUTF();
                final String metodo = in.readUTF();
                final String valor = ec.invoke(clase, metodo);
                out.writeInt(RemoteCodes.RESULT_SUCCESS);
                out.writeUTF(valor == null ? "" : valor);
                out.flush();
            } else if (RemoteCodes.CMD_VAR_VALUE.equals(cmd)) {
                final String clase = in.readUTF();
                final String variable = in.readUTF();
                final String valor = ec.varValue(clase, variable);
                out.writeInt(RemoteCodes.RESULT_SUCCESS);
                out.writeUTF(valor == null ? RemoteCodes.NULO : valor);
                out.flush();
            } else if (RemoteCodes.CMD_ADD_CLASSPATH.equals(cmd)) {
                ec.addToClasspath(in.readUTF());
                exito();
            } else if (RemoteCodes.CMD_STOP.equals(cmd)) {
                // Llega mientras otro hilo esta atendiendo un invoke; no lleva respuesta propia.
                ec.stop();
            } else {
                final Object arg = in.readObject();
                final Object r = ec.extensionCommand(cmd, arg);
                out.writeInt(RemoteCodes.RESULT_SUCCESS);
                out.writeObject(r);
                out.flush();
            }
            return true;
        } catch (ClassNotFoundException e) {
            escribir(RemoteCodes.RESULT_INTERNAL_PROBLEM, String.valueOf(e));
            return true;
        } catch (NotImplementedException e) {
            escribir(RemoteCodes.RESULT_NOT_IMPLEMENTED, String.valueOf(e.getMessage()));
            return true;
        } catch (ClassInstallException e) {
            out.writeInt(RemoteCodes.RESULT_CLASS_INSTALL_EXCEPTION);
            out.writeUTF(String.valueOf(e.getMessage()));
            out.writeObject(e.installed());
            out.flush();
            return true;
        } catch (EngineTerminationException e) {
            escribir(RemoteCodes.RESULT_TERMINATED, String.valueOf(e.getMessage()));
            return false;
        } catch (InternalException e) {
            escribir(RemoteCodes.RESULT_INTERNAL_PROBLEM, String.valueOf(e.getMessage()));
            return true;
        } catch (StoppedException e) {
            out.writeInt(RemoteCodes.RESULT_STOPPED);
            out.flush();
            return true;
        } catch (RunException e) {
            escribirDeUsuario(e);
            return true;
        }
    }

    private void exito() throws IOException {
        out.writeInt(RemoteCodes.RESULT_SUCCESS);
        out.flush();
    }

    private void escribir(int codigo, String mensaje) throws IOException {
        out.writeInt(codigo);
        out.writeUTF(mensaje);
        out.flush();
    }

    /** Manda la excepcion del usuario y, si tiene, su cadena de causas. */
    private void escribirDeUsuario(RunException e) throws IOException {
        final Throwable causa = e.getCause();
        if (causa == null) {
            out.writeInt(codigoDe(e));
            cuerpoDe(e);
            out.flush();
            return;
        }
        out.writeInt(RemoteCodes.RESULT_USER_EXCEPTION_CHAINED);
        out.writeInt(0);
        out.writeInt(codigoDe(e));
        cuerpoDe(e);
        Throwable t = causa;
        while (t instanceof RunException) {
            final RunException r = (RunException) t;
            out.writeInt(codigoDe(r));
            cuerpoDe(r);
            t = r.getCause();
        }
        // El terminador de la cadena; sin el, el que lee sigue esperando otra causa.
        out.writeInt(RemoteCodes.RESULT_SUCCESS);
        out.flush();
    }

    private static int codigoDe(RunException e) {
        return e instanceof ResolutionException
                ? RemoteCodes.RESULT_CORRALLED : RemoteCodes.RESULT_USER_EXCEPTION;
    }

    private void cuerpoDe(RunException e) throws IOException {
        if (e instanceof ResolutionException) {
            out.writeInt(((ResolutionException) e).id());
            out.writeObject(e.getStackTrace());
            return;
        }
        final UserException u = (UserException) e;
        out.writeUTF(String.valueOf(u.getMessage()));
        out.writeUTF(u.causeExceptionClass());
        out.writeObject(u.getStackTrace());
    }
}
