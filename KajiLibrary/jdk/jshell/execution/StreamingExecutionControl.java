package jdk.jshell.execution;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import jdk.jshell.spi.ExecutionControl;

/**
 * The engine that runs nothing: it hands every operation to another one over a pair of streams.
 *
 * <h2>What it is</h2>
 *
 * <p>JShell's end of the protocol. Each method writes the command's name and its arguments, flushes
 * the stream, and reads the answer. On the other side there is an {@link ExecutionControlForwarder}
 * doing the reverse and ending up calling a real engine --usually a {@link RemoteExecutionControl}
 * in another process.
 *
 * <h2>Why every answer starts with a mark</h2>
 *
 * <p>Because what the user's program prints travels over the same stream. Without
 * {@code COMMAND_PREFIX} in front, a {@code System.out.println("CMD_LOAD")} of the user's would be
 * indistinguishable from an answer. See {@link RemoteCodes}.
 *
 * <h2>The {@code null} that travels as text</h2>
 *
 * <p>{@code writeUTF} cannot write {@code null}, and the user's values may be one. The protocol uses
 * a sentinel string with control characters, which no reasonable {@code toString} produces. It is a
 * known compromise and it is written this way in the JDK; it is reproduced all the same because the
 * other side may be the JDK's agent.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>It works. The protocol is reading and writing over streams, and it needs nothing else. The test
 * {@code java/JSH2.java} runs it end to end against a {@link DirectExecutionControl} in the same
 * process, with two pipes in between.
 *
 * @since 9
 */
public class StreamingExecutionControl implements ExecutionControl {

    private final ObjectOutput out;
    private final ObjectInput in;

    /**
     * An engine over that pair of streams.
     *
     * @param out where the commands are sent
     * @param in where the answers arrive
     */
    public StreamingExecutionControl(ObjectOutput out, ObjectInput in) {
        this.out = out;
        this.in = in;
    }

    /**
     * Installs those classes on the other side.
     *
     * @param cbcs the name and the bytecode of each one
     * @throws ClassInstallException if any of them could not be installed
     * @throws NotImplementedException if the other engine does not know how to install
     * @throws EngineTerminationException if the connection was cut
     */
    @Override
    public void load(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        try {
            writeCommand(RemoteCodes.CMD_LOAD);
            out.writeObject(cbcs);
            out.flush();
            readInstallResult();
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    /**
     * Replaces the code of those classes on the other side.
     *
     * @param cbcs the classes and their new bytecode
     * @throws ClassInstallException if they could not be replaced
     * @throws NotImplementedException if the other engine does not know how to redefine
     * @throws EngineTerminationException if the connection was cut
     */
    @Override
    public void redefine(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        try {
            writeCommand(RemoteCodes.CMD_REDEFINE);
            out.writeObject(cbcs);
            out.flush();
            readInstallResult();
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    /**
     * Calls that method on the other side.
     *
     * @param className the class
     * @param methodName the method
     * @return the representation of the result
     * @throws RunException if the user's code failed
     * @throws EngineTerminationException if the connection was cut
     * @throws InternalException if the other engine failed
     */
    @Override
    public String invoke(String className, String methodName)
            throws RunException, EngineTerminationException, InternalException {
        try {
            writeCommand(RemoteCodes.CMD_INVOKE);
            out.writeUTF(className);
            out.writeUTF(methodName);
            out.flush();
            readRunResult();
            return in.readUTF();
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    /**
     * Reads a variable's value on the other side.
     *
     * @param className the class
     * @param varName the variable
     * @return the representation of the value, or {@code null}
     * @throws RunException if the user's code failed
     * @throws EngineTerminationException if the connection was cut
     * @throws InternalException if the other engine failed
     */
    @Override
    public String varValue(String className, String varName)
            throws RunException, EngineTerminationException, InternalException {
        try {
            writeCommand(RemoteCodes.CMD_VAR_VALUE);
            out.writeUTF(className);
            out.writeUTF(varName);
            out.flush();
            readRunResult();
            return readTextOrNull();
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    /**
     * Adds an entry to the other side's search path.
     *
     * @param path the entry
     * @throws EngineTerminationException if the connection was cut
     * @throws InternalException if it could not be added
     */
    @Override
    public void addToClasspath(String path) throws EngineTerminationException, InternalException {
        try {
            writeCommand(RemoteCodes.CMD_ADD_CLASSPATH);
            out.writeUTF(path);
            out.flush();
            readSimpleResult();
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    /**
     * Asks the other side to cut short whatever it is running.
     *
     * <p>This command is sent while the other side is busy with an {@code invoke}, so it expects no
     * answer: the answer that will arrive is the one from the {@code invoke} that was cut short.
     *
     * @throws EngineTerminationException if the connection was cut
     * @throws InternalException if it could not be sent
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
     * An operation of the other engine's own.
     *
     * @param command the operation's name
     * @param arg its argument
     * @return whatever it returns
     * @throws RunException if the user's code failed
     * @throws EngineTerminationException if the connection was cut
     * @throws InternalException if the other engine failed
     */
    @Override
    public Object extensionCommand(String command, Object arg)
            throws RunException, EngineTerminationException, InternalException {
        try {
            writeCommand(command);
            out.writeObject(arg);
            out.flush();
            readRunResult();
            return in.readObject();
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        } catch (ClassNotFoundException e) {
            throw new InternalException(String.valueOf(e));
        }
    }

    /**
     * Closes the connection.
     *
     * <p>It sends the close command and does not look at whether it arrived: if the other side is
     * already dead, there is nothing to do with that news, and {@code close} cannot fail.
     */
    @Override
    public void close() {
        try {
            writeCommand(RemoteCodes.CMD_CLOSE);
            out.flush();
        } catch (IOException e) {
            // The other side is already gone. That is exactly what was wanted.
        }
    }

    private void writeCommand(String cmd) throws IOException {
        out.writeInt(RemoteCodes.COMMAND_PREFIX);
        out.writeUTF(cmd);
    }

    /** A text that may be {@code null}; see the class note. */
    private String readTextOrNull() throws IOException {
        final String s = in.readUTF();
        return RemoteCodes.NULL_SENTINEL.equals(s) ? null : s;
    }

    /** The answer to an operation that returns nothing. */
    private void readSimpleResult() throws EngineTerminationException, InternalException {
        try {
            final int code = in.readInt();
            // A chain of `if` and not a `switch`: the constants come from another compiled file,
            // and there a `case` label does not fold (finding #503).
            if (code == RemoteCodes.RESULT_SUCCESS) {
                return;
            }
            if (code == RemoteCodes.RESULT_INTERNAL_PROBLEM) {
                throw new InternalException(in.readUTF());
            }
            if (code == RemoteCodes.RESULT_TERMINATED) {
                throw new EngineTerminationException(in.readUTF());
            }
            throw new EngineTerminationException("Bad remote result code: " + code);
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    /** The answer to {@code load} and {@code redefine}. */
    private void readInstallResult()
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        try {
            final int code = in.readInt();
            // A chain of `if` and not a `switch`: the constants come from another compiled file,
            // and there a `case` label does not fold (finding #503).
            if (code == RemoteCodes.RESULT_SUCCESS) {
                return;
            }
            if (code == RemoteCodes.RESULT_NOT_IMPLEMENTED) {
                throw new NotImplementedException(in.readUTF());
            }
            if (code == RemoteCodes.RESULT_CLASS_INSTALL_EXCEPTION) {
                throw new ClassInstallException(in.readUTF(), (boolean[]) in.readObject());
            }
            if (code == RemoteCodes.RESULT_TERMINATED) {
                throw new EngineTerminationException(in.readUTF());
            }
            throw new EngineTerminationException("Bad remote result code: " + code);
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        } catch (ClassNotFoundException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    /**
     * The answer to an operation that runs the user's code.
     *
     * <p>The chained case --{@code RESULT_USER_EXCEPTION_CHAINED}-- brings the exception and then,
     * one after another, its causes, up to a {@code RESULT_SUCCESS} acting as a terminator. They are
     * linked with {@code initCause} in the order they arrive; without that, the other side would see
     * the topmost exception with nothing underneath, which is exactly what is no use for
     * understanding what happened.
     */
    private void readRunResult()
            throws RunException, EngineTerminationException, InternalException {
        try {
            final int code = in.readInt();
            // A chain of `if` and not a `switch`: the constants come from another compiled file,
            // and there a `case` label does not fold (finding #503).
            if (code == RemoteCodes.RESULT_SUCCESS) {
                return;
            }
            if (code == RemoteCodes.RESULT_NOT_IMPLEMENTED) {
                throw new NotImplementedException(in.readUTF());
            }
            if (code == RemoteCodes.RESULT_USER_EXCEPTION) {
                throw readUserException();
            }
            if (code == RemoteCodes.RESULT_CORRALLED) {
                throw readResolutionException();
            }
            if (code == RemoteCodes.RESULT_USER_EXCEPTION_CHAINED) {
                throw readChain();
            }
            if (code == RemoteCodes.RESULT_INTERNAL_PROBLEM) {
                throw new InternalException(in.readUTF());
            }
            if (code == RemoteCodes.RESULT_STOPPED) {
                throw new StoppedException();
            }
            if (code == RemoteCodes.RESULT_TERMINATED) {
                throw new EngineTerminationException(in.readUTF());
            }
            throw new EngineTerminationException("Bad remote result code: " + code);
        } catch (EOFException e) {
            throw new EngineTerminationException(String.valueOf(e));
        } catch (IOException e) {
            throw new EngineTerminationException(String.valueOf(e));
        } catch (ClassNotFoundException e) {
            throw new EngineTerminationException(String.valueOf(e));
        }
    }

    private RunException readChain() throws IOException, ClassNotFoundException,
            EngineTerminationException {
        in.readInt();
        final RunException first = readUserException();
        RunException last = first;
        while (true) {
            final int c = in.readInt();
            final RunException cause;
            if (c == RemoteCodes.RESULT_USER_EXCEPTION) {
                cause = readUserException();
            } else if (c == RemoteCodes.RESULT_CORRALLED) {
                cause = readResolutionException();
            } else if (c == RemoteCodes.RESULT_SUCCESS) {
                return first;
            } else {
                throw new EngineTerminationException("Bad chained remote result code: " + c);
            }
            last.initCause(cause);
            last = cause;
        }
    }

    private UserException readUserException() throws IOException, ClassNotFoundException {
        final String message = in.readUTF();
        final String type = in.readUTF();
        return new UserException(message, type, (StackTraceElement[]) in.readObject());
    }

    private ResolutionException readResolutionException()
            throws IOException, ClassNotFoundException {
        final int id = in.readInt();
        return new ResolutionException(id, (StackTraceElement[]) in.readObject());
    }
}
