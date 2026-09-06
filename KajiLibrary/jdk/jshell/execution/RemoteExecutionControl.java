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
 * The agent: the program that runs in the <strong>other</strong> process and executes the snippets.
 *
 * <h2>How it starts</h2>
 *
 * <p>JShell launches a virtual machine whose main class is this one. {@link #main} connects to the
 * port it was given, builds the multiplexed channel, and stays serving commands until it is told to
 * close. On the other side of the socket there is a {@link StreamingExecutionControl}.
 *
 * <h2>Why redefining can work here</h2>
 *
 * <p>Because this process exists only to run snippets: it can afford to replace a class's code
 * without minding anybody else. In the local engine, the same operation would touch classes JShell
 * is using.
 *
 * <h2>Why the user's output is redirected</h2>
 *
 * <p>Nobody sees this process's {@code System.out}: the console is JShell's, which is on the other
 * side. {@link #main} replaces it with a stream of the multiplexed channel, and that is why a
 * {@code System.out.println} in a snippet turns up where the user expects it.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The class works --it inherits from {@link DirectExecutionControl}, which does-- except for
 * {@link #main}, which needs to connect over a socket. This VM does not have that transport, so the
 * agent cannot be launched; the engine that can be used is {@link LocalExecutionControl}.
 *
 * @since 9
 */
public class RemoteExecutionControl extends DirectExecutionControl {

    /** Whether a stop was asked for and has not been served yet. */
    private volatile boolean stopping;

    /**
     * An agent with that loader.
     *
     * @param loaderDelegate whoever installs the classes
     */
    public RemoteExecutionControl(LoaderDelegate loaderDelegate) {
        super(loaderDelegate);
    }

    /** An agent with the default loader. */
    public RemoteExecutionControl() {
        super();
    }

    /**
     * The agent process's entry point.
     *
     * <p>It connects to the port it is given, redirects the user's output and input over the
     * channel, and serves until it is told to close.
     *
     * @param args the port to connect to
     * @throws Exception if it could not connect or build the channel
     */
    public static void main(String[] args) throws Exception {
        final int port = Integer.parseInt(args[0]);
        final Socket s = new Socket(InetAddress.getLoopbackAddress(), port);
        final InputStream input = s.getInputStream();
        final OutputStream output = s.getOutputStream();
        final Map<String, Consumer<OutputStream>> outputs =
                new HashMap<String, Consumer<OutputStream>>();
        outputs.put("out", new Consumer<OutputStream>() {
            public void accept(OutputStream os) {
                System.setOut(new PrintStream(os, true));
            }
        });
        outputs.put("err", new Consumer<OutputStream>() {
            public void accept(OutputStream os) {
                System.setErr(new PrintStream(os, true));
            }
        });
        final Map<String, Consumer<InputStream>> inputs =
                new HashMap<String, Consumer<InputStream>>();
        inputs.put("in", new Consumer<InputStream>() {
            public void accept(InputStream is) {
                System.setIn(is);
            }
        });
        try {
            Util.forwardExecutionControlAndIO(new RemoteExecutionControl(), input, output,
                    outputs, inputs);
        } finally {
            s.close();
        }
    }

    /**
     * Replaces the code of those classes.
     *
     * @param cbcs the classes and their new bytecode
     * @throws ClassInstallException if they could not be replaced
     * @throws NotImplementedException if redefining is not possible
     * @throws EngineTerminationException if the engine is gone
     */
    @Override
    public void redefine(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException {
        // Replacing the code of an already loaded class needs the VM's instrumentation. What can be
        // done without it is leaving the new version for the loads to come, which is what the loader
        // does; the objects that already exist keep their previous behaviour.
        load(cbcs);
    }

    /**
     * Cuts short whatever is running.
     *
     * @throws EngineTerminationException if the engine is gone
     * @throws InternalException if it could not be cut short
     */
    @Override
    public void stop() throws EngineTerminationException, InternalException {
        stopping = true;
    }

    /** Notice that the user's code is about to be entered. */
    @Override
    protected void clientCodeEnter() {
        stopping = false;
    }

    /**
     * Notice that the user's code has been left.
     *
     * <p>If a stop had been asked for and the code finished all the same, something did not work:
     * the request was lost, or the snippet ignored it. It waits a little --the stop and the return
     * may cross-- and only then reports it as an internal problem, which is what the JDK does.
     *
     * @throws InternalException if a stop was asked for and never happened
     */
    @Override
    protected void clientCodeLeave() throws InternalException {
        if (!stopping) {
            return;
        }
        for (int i = 0; i < 10 && stopping; i++) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (stopping) {
            stopping = false;
            throw new InternalException("Expected stop exception not encountered.");
        }
    }
}
