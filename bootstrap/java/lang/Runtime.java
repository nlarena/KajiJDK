package java.lang;

// Minimal java.lang.Runtime: the program's handle on the VM it is running in. A singleton
// — there is exactly one VM per process, so `getRuntime()` always hands back the same
// object, allocated by the static initializer below.
//
// Out of scope: **shutdown hooks** (`addShutdownHook`/`removeShutdownHook`). A hook is a
// `Thread` the VM must *run* while terminating, i.e. execute Java bytecode on the one path
// that has to be able to stop executing it (`System.exit` cuts every frame — see
// `Exec::vm_exit`). Modelling them needs a termination phase that can still schedule
// threads, which this VM does not have yet.
public class Runtime {
    private static final Runtime current;

    static {
        current = new Runtime();
    }

    // Not instantiable from outside: the VM's single instance is the one above.
    private Runtime() {
    }

    public static Runtime getRuntime() {
        return current;
    }

    // Terminates the VM — the instance form of System.exit(int), which is where the real
    // work happens (the VM intercepts that call).
    public void exit(int status) {
        System.exit(status);
    }

    // How many processors the VM can use. Native: only the host OS knows.
    public native int availableProcessors();
}
