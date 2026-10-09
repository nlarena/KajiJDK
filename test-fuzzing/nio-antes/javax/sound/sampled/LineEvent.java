package javax.sound.sampled;

import java.util.EventObject;

/**
 * KajiLibrary's javax.sound.sampled.LineEvent -- a line opened, closed, started or stopped.
 *
 * <p>Four types, in two pairs that get confused:
 *
 * <ul>
 *   <li>{@link Type#OPEN} and {@link Type#CLOSE} are about the <b>resource</b>: the line reserved
 *       or released the device;
 *   <li>{@link Type#START} and {@link Type#STOP} are about the <b>flow</b>: there is or there is no
 *       audio going through.
 * </ul>
 *
 * <p>A line can open and close without ever sounding, and it can start and stop many times while it
 * is open. A clip that ends emits {@code STOP}, not {@code CLOSE}.
 *
 * <p>{@link #getFramePosition} says at which frame it happened; for {@code OPEN} and {@code CLOSE}
 * it can be {@link AudioSystem#NOT_SPECIFIED}.
 */
public class LineEvent extends EventObject {

    private static final long serialVersionUID = -1274246333383880410L;

    /** Which of the four. */
    private final Type type;

    /** At which frame. */
    private final long position;

    /**
     * @param line of which line
     * @param type which of the four
     * @param position at which frame, or {@link AudioSystem#NOT_SPECIFIED}
     * @throws IllegalArgumentException if the line is null
     */
    public LineEvent(Line line, Type type, long position) {
        super(line);
        this.type = type;
        this.position = position;
    }

    /** Of which line. */
    public final Line getLine() {
        return (Line) getSource();
    }

    /** Which of the four. */
    public final Type getType() {
        return this.type;
    }

    /** At which frame. */
    public final long getFramePosition() {
        return this.position;
    }

    /** The type, the word {@code event}, and the line. */
    @Override
    public String toString() {
        String s = "";
        if (this.type != null) {
            s = this.type.toString() + " ";
        }
        s = s + "event from line " + getLine();
        return s;
    }

    /**
     * Which of the four.
     *
     * <p>It is not an enum for the same reason as the rest of the package: the API is from 1999 and
     * enums from 2004. Here equality is by <b>identity</b>, as in {@link Control.Type}.
     */
    public static class Type {

        /** The line reserved the device. */
        public static final Type OPEN = new Type("Open");

        /** It released it. */
        public static final Type CLOSE = new Type("Close");

        /** Audio started going through. */
        public static final Type START = new Type("Start");

        /** It stopped going through. */
        public static final Type STOP = new Type("Stop");

        /** The name, for display. */
        private final String name;

        /** Protected: the types are defined by the platform. */
        protected Type(String name) {
            this.name = name;
        }

        /** By identity. See the class note. */
        @Override
        public final boolean equals(Object obj) {
            return super.equals(obj);
        }

        /** The identity one. */
        @Override
        public final int hashCode() {
            return super.hashCode();
        }

        /** The name. */
        @Override
        public String toString() {
            return this.name;
        }
    }
}
