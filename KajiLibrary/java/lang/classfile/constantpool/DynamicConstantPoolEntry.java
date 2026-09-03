package java.lang.classfile.constantpool;

import java.lang.classfile.BootstrapMethodEntry;

// La forma común de `CONSTANT_Dynamic` y `CONSTANT_InvokeDynamic` (JVMS §4.4.10): un índice al
// atributo `BootstrapMethods` de la clase más un `NameAndType`. Ese índice NO es un índice del pool;
// apunta a la tabla del atributo, y por eso estas dos entradas no se pueden resolver del todo sin
// haber leído antes ese atributo.
public interface DynamicConstantPoolEntry extends PoolEntry {

    /** El índice dentro de la tabla del atributo `BootstrapMethods`. */
    int bootstrapMethodIndex();

    /** La entrada de esa tabla. */
    BootstrapMethodEntry bootstrap();

    /** El par nombre/descriptor. */
    NameAndTypeEntry nameAndType();

    /** Atajo a `nameAndType().name()`. */
    default Utf8Entry name() {
        return nameAndType().name();
    }

    /** Atajo a `nameAndType().type()`. */
    default Utf8Entry type() {
        return nameAndType().type();
    }
}
