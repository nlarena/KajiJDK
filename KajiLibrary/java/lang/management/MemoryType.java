package java.lang.management;

/**
 * KajiLibrary's java.lang.management.MemoryType -- heap or non-heap.
 *
 * <p>Two values, and the division is the one the virtual machine specification makes:
 *
 * <ul>
 *   <li>{@link #HEAP} is where the objects live. It is what the collector walks;
 *   <li>{@link #NON_HEAP} is everything else the virtual machine reserves: the method area, the
 *       loaded classes' data, the compiled code.
 * </ul>
 *
 * <p>A program running out of memory looks first at which of the two filled up: they are different
 * problems with different solutions.
 *
 * <p>{@link #toString} returns {@code "Heap memory"} and {@code "Non-heap memory"}, not the
 * constant's name. {@code name()} is the one for the name.
 */
public enum MemoryType {

    /** Where the objects live. */
    HEAP("Heap memory"),

    /** What the virtual machine reserves for itself. */
    NON_HEAP("Non-heap memory");

    /** The text to display. */
    private final String description;

    MemoryType(String s) {
        this.description = s;
    }

    /** The text to display, not the name. See the class's note. */
    @Override
    public String toString() {
        return this.description;
    }
}
