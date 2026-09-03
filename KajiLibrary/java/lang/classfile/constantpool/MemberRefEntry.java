package java.lang.classfile.constantpool;

// La forma común de `CONSTANT_Fieldref`, `CONSTANT_Methodref` y `CONSTANT_InterfaceMethodref`
// (JVMS §4.4.2): un dueño (`CONSTANT_Class`) y un `CONSTANT_NameAndType`. Las tres tienen la misma
// estructura y se distinguen sólo por la etiqueta, que es lo que decide qué instrucción de
// invocación es legal sobre ellas.
public interface MemberRefEntry extends PoolEntry {

    /** La clase que declara —o a través de la cual se busca— el miembro. */
    ClassEntry owner();

    /** El par nombre/descriptor del miembro. */
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
