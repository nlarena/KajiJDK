package com.sun.jdi;

import java.util.List;
import java.util.Map;

import com.sun.jdi.event.EventQueue;
import com.sun.jdi.request.EventRequestManager;

/**
 * The virtual machine that is being debugged, seen from the debugger.
 *
 * <p>It is JDI's root object: a connector from {@link com.sun.jdi.connect} returns it, and
 * everything else hangs from it -- the loaded classes, the threads, the event queue, the request
 * manager.
 *
 * <h2>The capability queries</h2>
 *
 * <p>Half this interface are {@code canXxx()} methods. It is not redundancy: JDWP is a
 * negotiated protocol, and a VM on the other side may not support -- or may have switched off --
 * almost any service. A serious debugger asks before offering the feature in its interface,
 * because the alternative is to find out with an exception in the middle of a session.
 *
 * <h2>The {@code mirrorOf} family</h2>
 *
 * <p>It makes a value <strong>in the debugged machine</strong> from one of this side's. It is
 * needed in order to pass arguments to {@code ObjectReference.invokeMethod}: an {@code int} of
 * this process does not serve, the equivalent has to be created on the other side.
 *
 * <p>{@link #mirrorOf(String)} is the case that surprises most: it creates a new {@code String}
 * object in the other VM, with what that implies -- it takes up memory there and the collector
 * may take it away --.
 *
 * <h2>About this interface in this library</h2>
 *
 * <p>It was half done with a note saying that the methods that return other mirrors would be
 * added when {@code com.sun.jdi}, {@code com.sun.jdi.event} and {@code com.sun.jdi.request}
 * existed. They exist now, so they are there.
 */
public interface VirtualMachine extends Mirror {

    /** Trace nothing of the JDWP traffic. */
    int TRACE_NONE = 0;

    /** Trace the packets that go out to the debugged VM. */
    int TRACE_SENDS = 0x01;

    /** Trace the packets that come from the debugged VM. */
    int TRACE_RECEIVES = 0x02;

    /** Trace the events that arrive. */
    int TRACE_EVENTS = 0x04;

    /** Trace the creation of type mirrors. */
    int TRACE_REFTYPES = 0x08;

    /** Trace the creation of object mirrors. */
    int TRACE_OBJREFS = 0x10;

    /** Trace everything. */
    int TRACE_ALL = 0x00ffffff;

    /**
     * It suspends every thread of the debugged VM.
     *
     * <p>The suspensions are **counted**: two `suspend()` ask for two `resume()`. It is what
     * allows two parts of the debugger to suspend without getting in each other's way.
     */
    void suspend();

    /** It lifts one suspension. See the counting in {@link #suspend}. */
    void resume();

    /**
     * The debugged VM's process, or `null` if the debugger did not launch it.
     *
     * <p>There is only a process when it was reached through a
     * {@link com.sun.jdi.connect.LaunchingConnector}: if the debugger **attached** to a VM that
     * was already running, it does not have its `Process`.
     */
    Process process();

    /**
     * It cuts the debugging session off and releases everything.
     *
     * <p>The debugged VM **goes on running**: its suspensions are lifted and its event requests
     * are removed. It is the opposite of {@link #exit}.
     */
    void dispose();

    /**
     * It ends the debugged VM with that exit code.
     *
     * <p>After this it cannot be asked anything else.
     */
    void exit(int exitCode);

    /** Whether events on modifying a field can be asked for. */
    boolean canWatchFieldModification();

    /** Whether events on reading a field can be asked for. */
    boolean canWatchFieldAccess();

    /** Whether a method's bytecode can be read. */
    boolean canGetBytecodes();

    /** Whether it can be known whether a member is synthetic. */
    boolean canGetSyntheticAttribute();

    /** Whether the monitors a thread holds can be listed. */
    boolean canGetOwnedMonitorInfo();

    /** Whether it can be known which monitor a thread is waiting for. */
    boolean canGetCurrentContendedMonitor();

    /** Whether it can be known which threads are waiting for a monitor. */
    boolean canGetMonitorInfo();

    /** Whether an event request can be filtered by instance. */
    boolean canUseInstanceFilters();

    /** Whether already loaded classes can be redefined. */
    boolean canRedefineClasses();

    /**
     * Whether a redefinition may add methods.
     *
     * @deprecated No VM has supported it for a long time, and the JDWP specification set it
     * aside. It always gives `false`.
     */
    @Deprecated
    boolean canAddMethod();

    /**
     * Whether a redefinition may change the class's shape without restrictions.
     *
     * @deprecated The same as {@link #canAddMethod}: it was left without support.
     */
    @Deprecated
    boolean canUnrestrictedlyRedefineClasses();

    /** Whether frames can be popped from a thread's stack. */
    boolean canPopFrames();

    /** Whether a class's `SourceDebugExtension` attribute can be read. */
    boolean canGetSourceDebugExtension();

    /** Whether the VM death event can be asked for. */
    boolean canRequestVMDeathEvent();

    /** Whether a method exit event may bring the returned value. */
    boolean canGetMethodReturnValues();

    /** Whether a type's instances can be counted and listed. */
    boolean canGetInstanceInfo();

    /** Whether an event request can be filtered by source file name. */
    boolean canUseSourceNameFilters();

    /** Whether an early return from a method can be forced. */
    boolean canForceEarlyReturn();

    /**
     * Whether the debugged VM can be modified.
     *
     * <p>With `false` the session is read-only: one may look, not touch. It is the case of a
     * memory dump opened as though it were a VM.
     */
    boolean canBeModified();

    /** Whether monitor events can be asked for. */
    boolean canRequestMonitorEvents();

    /** Whether a monitor event may say in which frame it happened. */
    boolean canGetMonitorFrameInfo();

    /** Whether a class file format's version can be read. */
    boolean canGetClassFileVersion();

    /** Whether a class's constant pool can be read. */
    boolean canGetConstantPool();

    /**
     * Whether module information can be consulted.
     *
     * <p>It is `default` -- and not abstract -- because it arrived with Java 9, and a JDI
     * implementation written earlier has to go on compiling. By default it says no, which is the
     * right answer for any of those.
     */
    default boolean canGetModuleInfo() {
        return false;
    }

    /**
     * It fixes the default stratum for code with several source languages.
     *
     * <p>A `.class` generated from JSP carries line maps for two "strata" -- the bytecode and the
     * JSP -- and this chooses which to use when nobody asks for one.
     *
     * @param stratum the stratum's name, or `null` for the one the class declares as its own
     */
    void setDefaultStratum(String stratum);

    /** The default stratum, or `null` if it is the one each class declares. */
    String getDefaultStratum();

    /** A readable description of the debugged VM. */
    String description();

    /** The debugged VM's version, as it reports it. */
    String version();

    /** The debugged VM's name, as it reports it. */
    String name();

    /**
     * It fixes which JDWP traffic is traced.
     *
     * @param traceFlags an `or` combination of the `TRACE_*` constants
     */
    void setDebugTraceMode(int traceFlags);

    /**
     * Every class loaded in the debugged machine.
     *
     * <p>In a real program they are thousands, and each one is a trip. Almost always
     * {@link #classesByName} is wanted instead.
     *
     * @return the classes
     */
    List<ReferenceType> allClasses();

    /**
     * The classes with that name.
     *
     * <p>It returns a list and not a single one: two different loaders may have loaded classes of
     * the same name, and in an application server that is the normal thing, not the exception.
     *
     * @param className the full name
     * @return the classes with that name, or an empty list
     */
    List<ReferenceType> classesByName(String className);

    /**
     * Every module of the debugged machine.
     *
     * @return the modules
     * @since 9
     */
    default List<ModuleReference> allModules() {
        throw new UnsupportedOperationException();
    }

    /**
     * It replaces the code of some classes without restarting the VM.
     *
     * <p>It is what makes "hot reloading" possible. It has a hard limit: a method's body may be
     * changed and the class's <strong>shape</strong> may not -- adding a field, changing a
     * signature, touching the hierarchy --. The frames that were already on the stack go on
     * executing the old code, and that is why {@code Method.isObsolete} exists.
     *
     * @param classToBytes the classes and their new bytecode
     */
    void redefineClasses(Map<? extends ReferenceType, byte[]> classToBytes);

    /**
     * Every thread of the debugged machine.
     *
     * @return the threads
     */
    List<ThreadReference> allThreads();

    /**
     * The thread groups that have no parent.
     *
     * @return the root groups
     */
    List<ThreadGroupReference> topLevelThreadGroups();

    /**
     * The queue the events arrive through.
     *
     * @return the queue
     */
    EventQueue eventQueue();

    /**
     * The manager the events are asked for with.
     *
     * @return the manager
     */
    EventRequestManager eventRequestManager();

    /**
     * How many live instances there are of each of those types.
     *
     * <p>Counting them forces the other VM's heap to be walked, so it is expensive and may pause
     * it. The returned array corresponds position by position with the list that was passed.
     *
     * @param types the types to count
     * @return the number of instances of each one
     */
    long[] instanceCounts(List<? extends ReferenceType> types);

    /**
     * A {@code boolean} of the debugged machine with that value.
     *
     * @param value the value
     * @return the mirror
     */
    BooleanValue mirrorOf(boolean value);

    /**
     * A {@code byte} of the debugged machine with that value.
     *
     * @param value the value
     * @return the mirror
     */
    ByteValue mirrorOf(byte value);

    /**
     * A {@code char} of the debugged machine with that value.
     *
     * @param value the value
     * @return the mirror
     */
    CharValue mirrorOf(char value);

    /**
     * A {@code short} of the debugged machine with that value.
     *
     * @param value the value
     * @return the mirror
     */
    ShortValue mirrorOf(short value);

    /**
     * A {@code int} of the debugged machine with that value.
     *
     * @param value the value
     * @return the mirror
     */
    IntegerValue mirrorOf(int value);

    /**
     * A {@code long} of the debugged machine with that value.
     *
     * @param value the value
     * @return the mirror
     */
    LongValue mirrorOf(long value);

    /**
     * A {@code float} of the debugged machine with that value.
     *
     * @param value the value
     * @return the mirror
     */
    FloatValue mirrorOf(float value);

    /**
     * A {@code double} of the debugged machine with that value.
     *
     * @param value the value
     * @return the mirror
     */
    DoubleValue mirrorOf(double value);

    /**
     * A <strong>new</strong> {@code String} in the debugged machine.
     *
     * <p>It creates an object there, not a value: it takes up memory in the other VM and its
     * collector may take it away while it is being used. That is what
     * {@code ObjectReference.disableCollection} is for.
     *
     * @param value the text
     * @return the mirror
     */
    StringReference mirrorOf(String value);

    /**
     * The debugged machine's {@code void} value.
     *
     * <p>It exists because a method that returns nothing still has to be able to report
     * <strong>something</strong> as the result of {@code invokeMethod}, and that something is
     * this.
     *
     * @return void's mirror
     */
    VoidValue mirrorOfVoid();
}
