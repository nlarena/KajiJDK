package jdk.jfr.consumer;

import java.util.List;

import jdk.jfr.ValueDescriptor;

/**
 * A frame of a recorded stack of calls.
 *
 * <h2>Why there is a bytecode index and a line number</h2>
 *
 * <p>The bytecode index is always there; the line number only if the class was compiled with the
 * table of lines, which is optional. In code compiled without it, {@link #getLineNumber} returns
 * {@code -1} and the bytecode index is the only thing that locates the exact point.
 *
 * <p>They also differ in precision: inside one line there may be several calls, and the bytecode
 * index tells which of them it is.
 *
 * <h2>{@link #getType} is not the type of the method</h2>
 *
 * <p>It is the type <strong>of the frame</strong>: whether the code was interpreted, compiled by
 * the JIT or native. That datum is what explains a stack where the same method appears twice with
 * completely different costs.
 *
 * @since 9
 */
public final class RecordedFrame extends RecordedObject {

    RecordedFrame(List<ValueDescriptor> descriptors, Object[] values) {
        super(descriptors, values);
    }

    /**
     * Whether the frame is of Java code and not native.
     *
     * @return whether it is of Java
     */
    public boolean isJavaFrame() {
        return hasField("method") && getValue("method") != null;
    }

    /**
     * The index of the bytecode inside the method.
     *
     * @return the index, or {@code -1} if it is not known
     */
    public int getBytecodeIndex() {
        return getInt("bytecodeIndex");
    }

    /**
     * The line number of the source.
     *
     * @return the line, or {@code -1} if the class does not bring the table of lines
     */
    public int getLineNumber() {
        return getInt("lineNumber");
    }

    /**
     * How the code was running: interpreted, compiled or native.
     *
     * @return the type of frame
     */
    public String getType() {
        return getString("type");
    }

    /**
     * The method of the frame.
     *
     * @return the method, or {@code null} if the frame is not of Java
     */
    public RecordedMethod getMethod() {
        return getValue("method");
    }
}
