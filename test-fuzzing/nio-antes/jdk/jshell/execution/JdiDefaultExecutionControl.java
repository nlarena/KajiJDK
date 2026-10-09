package jdk.jshell.execution;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

import com.sun.jdi.VirtualMachine;
import jdk.jshell.spi.ExecutionEnv;

/**
 * The default engine of the {@code jshell} tool: the snippets run in another process.
 *
 * <h2>Why another process</h2>
 *
 * <p>Because the code written in an interactive session is not to be trusted --not even by whoever
 * writes it. A {@code System.exit(0)} typed without thinking takes the session with it if it runs in
 * the same process; in another, it takes a process JShell brings back up.
 *
 * <p>It also leaves JShell's machine clean: the snippets do not dirty its heap, leave it live threads
 * or load classes into it.
 *
 * <h2>How it is put together</h2>
 *
 * <p>Two channels to the same process. The orders go over the stream one, inherited from
 * {@link StreamingExecutionControl}; what can only be done from outside, which is redefining
 * classes, goes over JDI. See {@link JdiExecutionControl}.
 *
 * <h2>{@link JdiStarter}</h2>
 *
 * <p>It is the point where how the other process turns up is decided. Separating it allows the other
 * machine to be launched in a particular way --another Java version, a container, another user--
 * without touching anything in the engine.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>It cannot work: a {@link VirtualMachine} connected to another process is needed, and connecting
 * is JDI's transport, which this VM does not have. The engine that does work is
 * {@link LocalExecutionControl}.
 *
 * @since 9
 */
public class JdiDefaultExecutionControl extends JdiExecutionControl {

    private final VirtualMachine machine;
    private final Process process;

    JdiDefaultExecutionControl(ObjectOutput out, ObjectInput in, VirtualMachine machine,
            Process process) {
        super(out, in);
        this.machine = machine;
        this.process = process;
    }

    /**
     * Calls that method in the other process.
     *
     * @param className the class
     * @param methodName the method
     * @return the representation of the result
     * @throws RunException if the user's code failed
     * @throws EngineTerminationException if the other process is gone
     * @throws InternalException if the engine failed
     */
    @Override
    public String invoke(String className, String methodName)
            throws RunException, EngineTerminationException, InternalException {
        return super.invoke(className, methodName);
    }

    /**
     * Cuts short whatever is running in the other process.
     *
     * @throws EngineTerminationException if the other process is gone
     * @throws InternalException if it could not be cut short
     */
    @Override
    public void stop() throws EngineTerminationException, InternalException {
        super.stop();
    }

    /**
     * Closes the connection and ends the other process.
     *
     * <p>Killing the process as well as closing the channel is not excess: an agent that stopped
     * serving but is still alive is an orphan process, and a long session opening and closing
     * engines would leave one behind each time.
     */
    @Override
    public void close() {
        super.close();
        if (process != null) {
            process.destroy();
        }
    }

    /**
     * The virtual machine the agent runs on.
     *
     * @return the machine
     * @throws EngineTerminationException if it is gone
     */
    @Override
    protected synchronized VirtualMachine vm() throws EngineTerminationException {
        if (machine == null) {
            throw new EngineTerminationException("VM closed");
        }
        return machine;
    }

    /**
     * How the other virtual machine is started.
     *
     * @since 15
     */
    public interface JdiStarter {

        /**
         * Starts the other machine and returns what to talk to it with.
         *
         * @param env the session's environment
         * @param parameters the provider's parameters
         * @param port the port they will meet over
         * @return the machine and the process
         */
        TargetDescription start(ExecutionEnv env, Map<String, String> parameters, int port);

        /**
         * The virtual machine that was started and the process holding it.
         *
         * <p>They travel together and not apart because whoever receives them needs both: the
         * machine to talk over JDI, and the process to be able to end it. Having one without the
         * other leaves either a channel with no way to close it, or a process with no way to use it.
         *
         * @param vm the virtual machine
         * @param process the process holding it
         * @since 15
         */
        final class TargetDescription {

            private final VirtualMachine vm;
            private final Process process;

            /**
             * With that machine and that process.
             *
             * @param vm the virtual machine
             * @param process the process
             */
            public TargetDescription(VirtualMachine vm, Process process) {
                this.vm = vm;
                this.process = process;
            }

            /**
             * The virtual machine.
             *
             * @return the machine
             */
            public VirtualMachine vm() {
                return vm;
            }

            /**
             * The process.
             *
             * @return the process
             */
            public Process process() {
                return process;
            }

            @Override
            public String toString() {
                return "TargetDescription[vm=" + vm + ", process=" + process + "]";
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (!(obj instanceof TargetDescription)) {
                    return false;
                }
                final TargetDescription o = (TargetDescription) obj;
                return (vm == null ? o.vm == null : vm.equals(o.vm))
                        && (process == null ? o.process == null : process.equals(o.process));
            }

            @Override
            public int hashCode() {
                return (vm == null ? 0 : vm.hashCode()) * 31
                        + (process == null ? 0 : process.hashCode());
            }
        }
    }
}
