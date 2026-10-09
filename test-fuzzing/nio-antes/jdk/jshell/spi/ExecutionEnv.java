package jdk.jshell.spi;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import jdk.jshell.JShellConsole;

/**
 * What `jshell` lends an {@link ExecutionControl} so that it can do its job.
 *
 * <p>It is the SPI's other direction: {@link ExecutionControl} is what the engine offers `jshell`,
 * and this is what `jshell` offers the engine. And what it offers are the three things the engine
 * cannot make for itself:
 *
 * <ul>
 *   <li>the user's streams, which the engine has to **connect** to the VM it executes in;
 *   <li>the extra options to start that VM with;
 *   <li>a way to report that it has died, which is {@link #closeDown()}.
 * </ul>
 *
 * <p>It is implemented by `jshell`, not by the engine.
 *
 * @since 9
 */
public interface ExecutionEnv {

    /** Where the user's code reads from. */
    InputStream userIn();

    /** Where the user's code writes. */
    PrintStream userOut();

    /** Where the user's code writes its errors. */
    PrintStream userErr();

    /**
     * The extra options for the remote VM.
     *
     * <p>They come from the command line's `-R`. The engine adds them to its own if it launches a
     * separate VM, and ignores them if it executes in the same process.
     */
    List<String> extraRemoteVMOptions();

    /**
     * Reports that the engine has died.
     *
     * <p>It is called by the **engine**, not by `jshell`: it is how it tells `jshell` that it has run
     * out of VM on the other side and has to be rebuilt. `jshell` uses it to stop sending it
     * snippets.
     */
    void closeDown();

    /**
     * The console the user's code sees, if there is one.
     *
     * <p>Empty means there is no console, not that it is unknown: user code calling
     * `System.console()` has to receive `null`.
     *
     * <p>It is `default` because it arrived with Java 22, and an engine written earlier has to go on
     * compiling.
     */
    default Optional<JShellConsole> console() {
        return Optional.empty();
    }
}
