package java.lang.classfile.constantpool;

// The common shape of `CONSTANT_Fieldref`, `CONSTANT_Methodref` and `CONSTANT_InterfaceMethodref`
// (JVMS §4.4.2): an owner (`CONSTANT_Class`) and a `CONSTANT_NameAndType`. All three have the same
// structure and are told apart only by the tag, which is what decides which invocation instruction is
// legal on them.
public interface MemberRefEntry extends PoolEntry {

    /** The class that declares --or through which one looks up-- the member. */
    ClassEntry owner();

    /** The member's name/descriptor pair. */
    NameAndTypeEntry nameAndType();

    /** A shortcut to `nameAndType().name()`. */
    default Utf8Entry name() {
        return nameAndType().name();
    }

    /** A shortcut to `nameAndType().type()`. */
    default Utf8Entry type() {
        return nameAndType().type();
    }
}
