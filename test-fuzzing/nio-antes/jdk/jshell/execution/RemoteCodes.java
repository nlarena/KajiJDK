package jdk.jshell.execution;

/**
 * The vocabulary of the protocol between JShell and the engine that runs the code.
 *
 * <h2>Why there is a protocol and not calls</h2>
 *
 * <p>The engine may be on another virtual machine --that is what {@link JdiExecutionControl} is
 * about-- so {@link jdk.jshell.spi.ExecutionControl}'s operations travel as messages over a pair of
 * streams. Each message is a command name and its arguments; each answer is a code and whatever goes
 * with it.
 *
 * <p>The command names are the text of their own constant --{@code CMD_LOAD} is worth
 * {@code "CMD_LOAD"}-- because that way a dump of the stream reads without a translation table. In a
 * protocol whose job is to debug a debugger, that is worth more than the three bytes numbering them
 * would save.
 *
 * <h2>{@link #COMMAND_PREFIX}</h2>
 *
 * <p>It goes in front of every answer. The stream carries two things mixed together --what the
 * user's program prints and the engine's answers-- and this mark is what separates them: without it,
 * a {@code System.out.println} of the user's saying {@code "CMD_LOAD"} would be indistinguishable
 * from an answer.
 *
 * <p>The values are JDK 25's and cannot be chosen: an engine of this library has to be able to talk
 * to the JDK's agent and the other way round.
 */
final class RemoteCodes {

    /**
     * The mark preceding every answer from the engine.
     *
     * <p>In hexadecimal it is {@code 0xC03DC03D}, that is "code" twice over. That it is a pattern
     * recognizable by eye is not vanity: when the stream loses sync, it is the only thing that shows
     * where it caught up again.
     */
    static final int COMMAND_PREFIX = 0xC03DC03D;

    /**
     * The text a {@code null} travels as.
     *
     * <p>{@code writeUTF} cannot write {@code null} and the user's values may be one. This sentinel
     * carries control characters precisely so that no reasonable {@code toString} produces it by
     * accident. It is a known compromise; it is written this way in the JDK and copied because the
     * other side of the stream may be the JDK's agent.
     */
    static final String NULL_SENTINEL = "\u0002*?*NULL*?*\u0003";

    /** Close the engine. */
    static final String CMD_CLOSE = "CMD_CLOSE";

    /** Install new classes. */
    static final String CMD_LOAD = "CMD_LOAD";

    /** Replace the code of classes that were already there. */
    static final String CMD_REDEFINE = "CMD_REDEFINE";

    /** Call a method. */
    static final String CMD_INVOKE = "CMD_INVOKE";

    /** Read a variable's value. */
    static final String CMD_VAR_VALUE = "CMD_VAR_VALUE";

    /** Add an entry to the class path. */
    static final String CMD_ADD_CLASSPATH = "CMD_ADD_CLASSPATH";

    /** Cut short whatever is running. */
    static final String CMD_STOP = "CMD_STOP";

    /** It went well. */
    static final int RESULT_SUCCESS = 100;

    /** The engine ended and will serve nothing more. */
    static final int RESULT_TERMINATED = 101;

    /** That engine does not implement that operation. */
    static final int RESULT_NOT_IMPLEMENTED = 102;

    /** The engine failed, not the user's code. */
    static final int RESULT_INTERNAL_PROBLEM = 103;

    /** The user's code threw an exception. */
    static final int RESULT_USER_EXCEPTION = 104;

    /**
     * The user's code called something that is not defined yet.
     *
     * <p>"Corralled" is what JShell calls the filler method it puts in place of one the user
     * mentioned but has not written yet. Running it is not a mistake in the program: it is how
     * JShell says that definition is missing.
     */
    static final int RESULT_CORRALLED = 105;

    /** The classes could not be installed. */
    static final int RESULT_CLASS_INSTALL_EXCEPTION = 106;

    /** It was cut short by a {@link #CMD_STOP}. */
    static final int RESULT_STOPPED = 107;

    /** Like {@link #RESULT_USER_EXCEPTION}, and the chain of causes travels too. */
    static final int RESULT_USER_EXCEPTION_CHAINED = 108;

    private RemoteCodes() {
    }
}
