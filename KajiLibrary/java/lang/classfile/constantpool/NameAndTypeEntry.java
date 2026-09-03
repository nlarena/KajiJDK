package java.lang.classfile.constantpool;

// `CONSTANT_NameAndType_info` (JVMS §4.4.6): el par (nombre simple, descriptor) que una referencia a
// miembro o una constante dinámica usa para nombrar lo que busca, sin decir dónde está.
public interface NameAndTypeEntry extends PoolEntry {

    /** El nombre simple del miembro. */
    Utf8Entry name();

    /** El descriptor de campo o de método. */
    Utf8Entry type();
}
