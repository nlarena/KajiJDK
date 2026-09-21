package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.PostVMInitHook -- what runs when the VM has finished starting up.
 *
 * <p>There are things that cannot be done during initialisation because they need a VM already up:
 * in the JDK, this hook is the one that sets up the management support when it was asked for on the
 * command line. The VM invokes it by name, once, after start-up and before `main`.
 *
 * <p>Here it **does nothing, and nothing is pending**: there are no management agents nor JMX to
 * initialise, so the list of things to do after start-up is empty. The class exists with the shape
 * the JDK declares, so that that point of invocation has somebody to call if some day there is
 * something.
 */
public class PostVMInitHook {

    public PostVMInitHook() {
    }

    /** The VM invokes it once, already started. */
    public static void run() {
    }
}
