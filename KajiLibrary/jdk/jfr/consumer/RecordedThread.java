package jdk.jfr.consumer;

import java.util.List;

import jdk.jfr.ValueDescriptor;

/**
 * A thread, as it was recorded.
 *
 * <h2>Why there are two names and two identifiers</h2>
 *
 * <p>A Java thread is two things at a time: a {@code Thread} object and a thread of the operating
 * system. {@link #getJavaName} and {@link #getJavaThreadId} are of the first one; {@link
 * #getOSName} and {@link #getOSThreadId} of the second.
 *
 * <p>They do not coincide and both are needed. The identifier of the system is the one that allows
 * one to cross the recording with what a tool from outside saw --a native profiler, {@code top}--;
 * the Java one is the one that appears in a thread dump.
 *
 * <p>A virtual thread has no system thread of its own: {@link #isVirtual} says so, and there the
 * fields of the operating system mean nothing.
 *
 * @since 9
 */
public final class RecordedThread extends RecordedObject {

    RecordedThread(List<ValueDescriptor> descriptors, Object[] values) {
        super(descriptors, values);
    }

    /**
     * The name of the thread in the operating system.
     *
     * @return the name, or {@code null}
     */
    public String getOSName() {
        return getString("osName");
    }

    /**
     * The identifier of the thread in the operating system.
     *
     * @return the identifier, or {@code -1} if it is a virtual thread
     */
    public long getOSThreadId() {
        return getLong("osThreadId");
    }

    /**
     * The group it belongs to.
     *
     * @return the group, or {@code null}
     */
    public RecordedThreadGroup getThreadGroup() {
        return getValue("group");
    }

    /**
     * The name of the {@code Thread} object.
     *
     * @return the name, or {@code null}
     */
    public String getJavaName() {
        return getString("javaName");
    }

    /**
     * The identifier of the {@code Thread} object.
     *
     * @return the identifier, or {@code 0} if the thread is not of Java
     */
    public long getJavaThreadId() {
        return getLong("javaThreadId");
    }

    /**
     * The identifier the VM that recorded gave this thread.
     *
     * @return the identifier
     */
    public long getId() {
        return getLong("javaThreadId");
    }

    /**
     * Whether it is a virtual thread.
     *
     * @return whether it is
     */
    public boolean isVirtual() {
        return hasField("virtual") && getBoolean("virtual");
    }
}
