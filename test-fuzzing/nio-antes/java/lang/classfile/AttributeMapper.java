package java.lang.classfile;

// What knows how to read and write an attribute with a given name. A reader finds a name in the file,
// looks the mapper up, and asks it to interpret the body; a writer goes the other way round. The
// mappers of the attributes the JVMS defines are in {@link Attributes}.
public interface AttributeMapper<A extends Attribute<A>> {

    /** The attribute's name, just as it appears in the `Utf8`. */
    String name();

    /**
     * It reads the attribute. `pos` is the offset of the **body**'s first byte, that is, after the
     * `attribute_name_index` and the `attribute_length`.
     */
    A readAttribute(AttributedElement enclosing, ClassReader cf, int pos);

    /** It writes the whole attribute --name, length and body-- into `buf`. */
    void writeAttribute(BufWriter buf, A attr);

    /** Whether the attribute may appear more than once at the same place. By default, no. */
    default boolean allowMultiple() {
        return false;
    }

    /** What happens to this attribute when the class is transformed. */
    AttributeStability stability();

    /**
     * How much of an attribute survives a transformation of the class containing it. The constants go
     * from the most stable to the least, and that is the criterion by which a transformation decides
     * whether it can copy the attribute as it stands or has to drop it.
     */
    public enum AttributeStability {

        /** It depends neither on the pool nor on the code's positions: it is always copied. */
        STATELESS,
        /** It depends on the constant pool, but not on the code. */
        CP_REFS,
        /** It depends on the positions inside the `code` array. */
        LABELS,
        /** The format is not known; it is copied byte by byte and may end up wrong. */
        UNKNOWN,
        /** It cannot be carried over: a transformation drops it. */
        UNSTABLE;
    }
}
