package java.lang.classfile.constantpool;

// `CONSTANT_NameAndType_info` (JVMS §4.4.6): the (simple name, descriptor) pair a member reference
// or a dynamic constant uses to name what it is after, without saying where it is.
public interface NameAndTypeEntry extends PoolEntry {

    /** The member's simple name. */
    Utf8Entry name();

    /** The field or method descriptor. */
    Utf8Entry type();
}
