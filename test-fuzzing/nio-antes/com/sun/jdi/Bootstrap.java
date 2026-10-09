package com.sun.jdi;

/**
 * Where the {@link VirtualMachineManager} is got from: the whole start-up of JDI.
 *
 * @since 1.3
 */
public class Bootstrap {

    /** For whoever instantiates it; the class has no state. */
    public Bootstrap() {
    }

    /**
     * The virtual machine manager, where every connector comes from.
     *
     * @return the manager
     * @throws UnsupportedOperationException in this library: JDI needs an implementation of the
     * debugging protocol (JDWP) and of the transport, which are dozens of internal classes and are
     * not part of this API
     */
    public static synchronized VirtualMachineManager virtualMachineManager() {
        throw new UnsupportedOperationException(
                "JDI needs an implementation of JDWP and of its transport, which this library "
                + "does not bring; what is here is the whole API, against which a debugger "
                + "compiles");
    }
}
