package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.JcmdVThreadCommands -- the registration of the `jcmd` commands
 * about virtual threads.
 *
 * <p>**It has no public member at all, and that is the whole class.** In the JDK its entire job is
 * done by the static initialiser: it registers the `Thread.vthread_scheduler` and
 * `Thread.vthread_dump` commands that the `jcmd` tool then invokes from outside the process. Nobody
 * calls it from Java.
 *
 * <p>Here the initialiser registers nothing, because there is no diagnostic channel to register
 * with nor a scheduler of virtual threads to report on. The type exists with the shape the JDK
 * declares.
 */
public class JcmdVThreadCommands {

    private JcmdVThreadCommands() {
    }
}
