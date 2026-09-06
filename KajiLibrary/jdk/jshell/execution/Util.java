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
 * The pieces for building an execution engine that lives on the far side of a pair of streams.
 *
 * <h2>The two ends</h2>
 *
 * <p>{@link #forwardExecutionControl} is the side that <strong>executes</strong>: it stays serving
 * commands until it is told to close. {@link #remoteInputOutput} is <strong>JShell</strong>'s side:
 * it builds the pair of streams and returns the engine to talk to it with.
 *
 * <h2>Why multiplexing is necessary</h2>
 *
 * <p>Between the two ends there is a single connection and several streams have to travel through
 * it: the commands, what the user's program prints, its error output, and its input. Mixing them
 * unlabelled would make them impossible to separate; opening one connection per stream would
 * quadruple what has to get through a firewall. See {@link MultiplexingOutputStream}.
 *
 * <h2>The order the streams are opened in</h2>
 *
 * <p>{@link ObjectOutputStream} writes a header when built and {@link ObjectInputStream} reads it
 * when built. If both ends create the input first, both sit waiting for a header nobody wrote. That
 * is why the output is always created first and flushed.
 *
 * <p>It is the classic mistake in this kind of code and it gives no useful symptom: both processes
 * stay alive and still.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The first three methods work: they are streams and threads. {@link #detectJdiExitEvent} needs
 * the event queue of a debugged virtual machine, and this VM has no JDI transport; see
 * {@link JdiExecutionControl}.
 *
 * @since 9
 */
public class Util {

    /** The name of the stream the commands travel over. */
    private static final String COMMANDS = "$command";

    private Util() {
    }

    /**
     * Serves commands over that pair of streams until the close one arrives.
     *
     * <p>It does not return until then: it is the main loop of the side that executes.
     *
     * @param ec the engine that really executes
     * @param inStream where the commands arrive
     * @param outStream where they are answered
     */
    public static void forwardExecutionControl(ExecutionControl ec, ObjectInput inStream,
            ObjectOutput outStream) {
        new ExecutionControlForwarder(ec, inStream, outStream).commandLoop();
    }

    /**
     * Builds the executing side's streams and stays serving.
     *
     * <p>Before serving, it redirects the user program's output and input over the shared channel:
     * without that, what the snippet prints would be lost in the remote process.
     *
     * @param ec the engine that really executes
     * @param inStream the input connection
     * @param outStream the output connection
     * @param outputStreamMap what to do with each output stream to be published
     * @param inputStreamMap what to do with each input stream
     * @throws IOException if the streams could not be built
     */
    public static void forwardExecutionControlAndIO(ExecutionControl ec, InputStream inStream,
            OutputStream outStream, Map<String, Consumer<OutputStream>> outputStreamMap,
            Map<String, Consumer<InputStream>> inputStreamMap) throws IOException {
        for (final Map.Entry<String, Consumer<OutputStream>> e : outputStreamMap.entrySet()) {
            e.getValue().accept(multiplexed(e.getKey(), outStream));
        }
        // The output goes first and is flushed: ObjectOutputStream's header has to arrive before the
        // other side tries to read its own. See the class note.
        final ObjectOutputStream out = new ObjectOutputStream(multiplexed(COMMANDS, outStream));
        out.flush();
        final Map<String, OutputStream> targets = new HashMap<String, OutputStream>();
        final List<Closeable> toClose = new ArrayList<Closeable>();
        final PipedInputStream commandsIn = new PipedInputStream();
        final PipedOutputStream commandsOut = new PipedOutputStream(commandsIn);
        targets.put(COMMANDS, commandsOut);
        toClose.add(commandsOut);
        for (final Map.Entry<String, Consumer<InputStream>> e : inputStreamMap.entrySet()) {
            final PipedInputStream pin = new PipedInputStream();
            final PipedOutputStream pout = new PipedOutputStream(pin);
            targets.put(e.getKey(), pout);
            toClose.add(pout);
            e.getValue().accept(pin);
        }
        new DemultiplexInput(inStream, targets, toClose).start();
        final ObjectInputStream in = new ObjectInputStream(commandsIn);
        forwardExecutionControl(ec, in, out);
    }

    /**
     * Builds JShell's side's streams and returns the engine to talk to it with.
     *
     * @param inStream the input connection
     * @param outStream the output connection
     * @param outputStreamMap where to send each stream that arrives
     * @param inputStreamMap where to take each stream that is sent from
     * @param factory how to build the engine over the assembled pair of streams
     * @return the engine
     * @throws IOException if the streams could not be built
     */
    public static ExecutionControl remoteInputOutput(InputStream inStream, OutputStream outStream,
            Map<String, OutputStream> outputStreamMap, Map<String, InputStream> inputStreamMap,
            BiFunction<ObjectInput, ObjectOutput, ExecutionControl> factory) throws IOException {
        final Map<String, OutputStream> targets =
                new HashMap<String, OutputStream>(outputStreamMap);
        final List<Closeable> toClose = new ArrayList<Closeable>();
        final PipedInputStream commandsIn = new PipedInputStream();
        final PipedOutputStream commandsOut = new PipedOutputStream(commandsIn);
        targets.put(COMMANDS, commandsOut);
        toClose.add(commandsOut);
        for (final Map.Entry<String, InputStream> e : inputStreamMap.entrySet()) {
            copyInBackground(e.getValue(), multiplexed(e.getKey(), outStream));
        }
        final ObjectOutputStream out = new ObjectOutputStream(multiplexed(COMMANDS, outStream));
        out.flush();
        new DemultiplexInput(inStream, targets, toClose).start();
        final ObjectInputStream in = new ObjectInputStream(commandsIn);
        return factory.apply(in, out);
    }

    /**
     * Tells when the debugged virtual machine ends.
     *
     * <p>It stays listening to the event queue on a thread of its own and calls the reporter when a
     * {@link VMDeathEvent} or a {@link VMDisconnectEvent} arrives. Both matter and they are not the
     * same: the first is the other VM ending in an orderly way, the second is the connection falling
     * over. For JShell the consequence is the same --there is no engine any more-- and that is why
     * both report.
     *
     * <p>The thread is a daemon: if the only thing left alive is this watcher, there is nothing to
     * watch.
     *
     * <p>On this VM the queue never fills, because JDI's transport is what feeds it and there is
     * none. The thread starts all the same and what happens depends on the machine it is handed; see
     * {@link JdiExecutionControl}.
     *
     * @param vm the debugged machine
     * @param reporter whom to tell
     */
    public static void detectJdiExitEvent(VirtualMachine vm, Consumer<String> reporter) {
        final EventQueue queue = vm.eventQueue();
        final Thread t = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    final EventSet set;
                    try {
                        set = queue.remove();
                    } catch (InterruptedException e) {
                        return;
                    } catch (VMDisconnectedException e) {
                        reporter.accept("VM disconnected");
                        return;
                    }
                    for (final Event ev : set) {
                        if (ev instanceof VMDeathEvent) {
                            reporter.accept("VM died");
                            return;
                        }
                        if (ev instanceof VMDisconnectEvent) {
                            reporter.accept("VM disconnected");
                            return;
                        }
                    }
                    set.resume();
                }
            }
        }, "JDI exit watcher");
        t.setDaemon(true);
        t.start();
    }

    private static OutputStream multiplexed(String name, OutputStream target) {
        return new MultiplexingOutputStream(name, target);
    }

    /** Copies one stream into another, on a thread of its own, until it ends. */
    private static void copyInBackground(InputStream from, OutputStream to) {
        final Thread t = new Thread(new Runnable() {
            public void run() {
                final byte[] buf = new byte[1024];
                try {
                    while (true) {
                        final int n = from.read(buf);
                        if (n < 0) {
                            return;
                        }
                        to.write(buf, 0, n);
                        to.flush();
                    }
                } catch (IOException e) {
                    // The stream ended; there is nobody to tell and nothing to do.
                }
            }
        }, "input copier");
        t.setDaemon(true);
        t.start();
    }
}
