package jdk.jshell.execution;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;

/**
 * The remote engine that can also look at the other virtual machine with JDI.
 *
 * <h2>Why both are needed</h2>
 *
 * <p>The stream protocol is enough for asking the agent to load and to run. It is not enough for
 * <strong>redefining</strong>: replacing the code of an already loaded class is an operation of the
 * virtual machine, not of the program running inside it. That is done from outside, with JDI.
 *
 * <p>Hence this class inherits the protocol from {@link StreamingExecutionControl} and adds a
 * {@link #vm()}: the orders go over the stream, and the surgery over JDI.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>{@link #redefine} and {@link #referenceType} need a {@link VirtualMachine} connected to another
 * process, and connecting is JDI's transport, which this VM does not have. The
 * {@code com.sun.jdi} API is complete and these calls are the right ones; what is missing is
 * underneath, not here.
 *
 * @since 9
 */
public abstract class JdiExecutionControl extends StreamingExecutionControl {

    /**
     * An engine over that pair of streams.
     *
     * @param out where the commands are sent
     * @param in where the answers arrive
     */
    protected JdiExecutionControl(ObjectOutput out, ObjectInput in) {
        super(out, in);
    }

    /**
     * The virtual machine the agent runs on.
     *
     * @return the machine
     * @throws EngineTerminationException if it is gone
     */
    protected abstract VirtualMachine vm() throws EngineTerminationException;

    /**
     * Replaces the code of those classes on the other virtual machine.
     *
     * <p>It is done with {@code VirtualMachine.redefineClasses}, which is the only way to change an
     * already loaded class. It has the known limit: a method's body can be changed and the class's
     * shape cannot, and the frames already on the stack go on with the old code.
     *
     * @param cbcs the classes and their new bytecode
     * @throws ClassInstallException if any of them could not be replaced
     * @throws EngineTerminationException if the other machine is gone
     */
    @Override
    public void redefine(ClassBytecodes[] cbcs)
            throws ClassInstallException, EngineTerminationException {
        final VirtualMachine machine = vm();
        final java.util.Map<ReferenceType, byte[]> map =
                new java.util.HashMap<ReferenceType, byte[]>();
        final boolean[] installed = new boolean[cbcs.length];
        for (int i = 0; i < cbcs.length; i++) {
            final ReferenceType rt = referenceType(machine, cbcs[i].name());
            if (rt == null) {
                throw new ClassInstallException("redefine: not loaded " + cbcs[i].name(),
                        installed);
            }
            map.put(rt, cbcs[i].bytecodes());
            installed[i] = true;
        }
        machine.redefineClasses(map);
    }

    /**
     * That class's reflection on the other virtual machine.
     *
     * <p>It returns {@code null} when there is none by that name, and also when there is more than
     * one: two different loaders may have loaded classes of the same name, and there is no choosing
     * between them without knowing which the asker meant.
     *
     * @param vm the machine
     * @param name the class's full name
     * @return the reflection, or {@code null}
     */
    protected ReferenceType referenceType(VirtualMachine vm, String name) {
        final List<ReferenceType> rts = vm.classesByName(name);
        return rts.size() == 1 ? rts.get(0) : null;
    }
}
