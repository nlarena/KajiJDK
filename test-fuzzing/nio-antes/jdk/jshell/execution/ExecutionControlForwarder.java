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
 * The far end of {@link StreamingExecutionControl}: it reads commands and runs them.
 *
 * <h2>What it does</h2>
 *
 * <p>A loop: read the mark, read the command's name, read its arguments, ask the real engine for it,
 * and write the answer. It is what runs inside the process that executes the snippets.
 *
 * <h2>Why exceptions travel taken apart</h2>
 *
 * <p>A user's exception is not serialized: it could be of a class the other side does not know
 * --the user has just written it-- and deserializing it over there would fail. What is sent is the
 * message, the class's name and the stack trace, which is what JShell needs in order to show it.
 *
 * <p>The causes travel chained one after another, terminated by a {@code RESULT_SUCCESS}. Without
 * that, the other side would see the topmost exception with nothing underneath, which is exactly
 * what is no use.
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
     * Serves commands until the close one arrives or the stream is cut.
     *
     * <p>It propagates nothing: it is the main loop of the process that executes, and there is
     * nobody to tell about a problem. What it can do is stop serving, which is what it does.
     */
    void commandLoop() {
        try {
            while (true) {
                if (in.readInt() != RemoteCodes.COMMAND_PREFIX) {
                    // The stream lost sync: reading on would give invented commands.
                    return;
                }
                final String cmd = in.readUTF();
                if (RemoteCodes.CMD_CLOSE.equals(cmd)) {
                    ec.close();
                    return;
                }
                if (!serve(cmd)) {
                    return;
                }
            }
        } catch (IOException e) {
            // The other side left. There is nothing to answer and nobody to answer to.
        }
    }

    /** Serves one command; returns false when serving has to stop. */
    private boolean serve(String cmd) throws IOException {
        try {
            if (RemoteCodes.CMD_LOAD.equals(cmd)) {
                ec.load((ClassBytecodes[]) in.readObject());
                success();
            } else if (RemoteCodes.CMD_REDEFINE.equals(cmd)) {
                ec.redefine((ClassBytecodes[]) in.readObject());
                success();
            } else if (RemoteCodes.CMD_INVOKE.equals(cmd)) {
                final String type = in.readUTF();
                final String method = in.readUTF();
                final String value = ec.invoke(type, method);
                out.writeInt(RemoteCodes.RESULT_SUCCESS);
                out.writeUTF(value == null ? "" : value);
                out.flush();
            } else if (RemoteCodes.CMD_VAR_VALUE.equals(cmd)) {
                final String type = in.readUTF();
                final String variable = in.readUTF();
                final String value = ec.varValue(type, variable);
                out.writeInt(RemoteCodes.RESULT_SUCCESS);
                out.writeUTF(value == null ? RemoteCodes.NULL_SENTINEL : value);
                out.flush();
            } else if (RemoteCodes.CMD_ADD_CLASSPATH.equals(cmd)) {
                ec.addToClasspath(in.readUTF());
                success();
            } else if (RemoteCodes.CMD_STOP.equals(cmd)) {
                // It arrives while another thread is serving an invoke; it carries no answer of its
                // own.
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
            write(RemoteCodes.RESULT_INTERNAL_PROBLEM, String.valueOf(e));
            return true;
        } catch (NotImplementedException e) {
            write(RemoteCodes.RESULT_NOT_IMPLEMENTED, String.valueOf(e.getMessage()));
            return true;
        } catch (ClassInstallException e) {
            out.writeInt(RemoteCodes.RESULT_CLASS_INSTALL_EXCEPTION);
            out.writeUTF(String.valueOf(e.getMessage()));
            out.writeObject(e.installed());
            out.flush();
            return true;
        } catch (EngineTerminationException e) {
            write(RemoteCodes.RESULT_TERMINATED, String.valueOf(e.getMessage()));
            return false;
        } catch (InternalException e) {
            write(RemoteCodes.RESULT_INTERNAL_PROBLEM, String.valueOf(e.getMessage()));
            return true;
        } catch (StoppedException e) {
            out.writeInt(RemoteCodes.RESULT_STOPPED);
            out.flush();
            return true;
        } catch (RunException e) {
            writeUserException(e);
            return true;
        }
    }

    private void success() throws IOException {
        out.writeInt(RemoteCodes.RESULT_SUCCESS);
        out.flush();
    }

    private void write(int code, String message) throws IOException {
        out.writeInt(code);
        out.writeUTF(message);
        out.flush();
    }

    /** Sends the user's exception and, if it has one, its chain of causes. */
    private void writeUserException(RunException e) throws IOException {
        final Throwable cause = e.getCause();
        if (cause == null) {
            out.writeInt(codeOf(e));
            bodyOf(e);
            out.flush();
            return;
        }
        out.writeInt(RemoteCodes.RESULT_USER_EXCEPTION_CHAINED);
        out.writeInt(0);
        out.writeInt(codeOf(e));
        bodyOf(e);
        Throwable t = cause;
        while (t instanceof RunException) {
            final RunException r = (RunException) t;
            out.writeInt(codeOf(r));
            bodyOf(r);
            t = r.getCause();
        }
        // The chain's terminator; without it, the reader goes on waiting for another cause.
        out.writeInt(RemoteCodes.RESULT_SUCCESS);
        out.flush();
    }

    private static int codeOf(RunException e) {
        return e instanceof ResolutionException
                ? RemoteCodes.RESULT_CORRALLED : RemoteCodes.RESULT_USER_EXCEPTION;
    }

    private void bodyOf(RunException e) throws IOException {
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
