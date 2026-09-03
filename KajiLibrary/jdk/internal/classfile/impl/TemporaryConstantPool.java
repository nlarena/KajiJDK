package jdk.internal.classfile.impl;

import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.DoubleEntry;
import java.lang.classfile.constantpool.FieldRefEntry;
import java.lang.classfile.constantpool.FloatEntry;
import java.lang.classfile.constantpool.IntegerEntry;
import java.lang.classfile.constantpool.InterfaceMethodRefEntry;
import java.lang.classfile.constantpool.LongEntry;
import java.lang.classfile.constantpool.MethodRefEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;

// El pool de las entradas sueltas.
//
// Buena parte de las fábricas públicas de `java.lang.classfile` reciben un `String`, un `int` o un
// `ClassDesc` en vez de una entrada de pool —`AnnotationValue.ofString("hola")`,
// `TypeCheckInstruction.of(CHECKCAST, CD_String)`— y tienen que devolver un objeto que YA tenga su
// entrada. Esa entrada sale de acá. El JDK hace exactamente lo mismo con su `TemporaryConstantPool`.
//
// Lo que hay que tener presente, y es la única consecuencia visible: una entrada de este pool
// pertenece a este pool y no al de la clase que se esté escribiendo, así que `entry.constantPool()`
// no va a ser el pool destino y el escritor tiene que adoptarla. `ConstantPoolBuilderImpl` ya lo
// hace con toda entrada ajena, así que no hace falta nada más de parte de quien la usa.
//
// El pool dedup lica por valor, así que pedir mil veces `"hola"` no lo hace crecer; pedir un millón
// de textos distintos sí, y a los 65535 índices tira. Es el mismo techo que tiene cualquier `.class`
// y nadie llega ahí fabricando constantes sueltas, pero conviene que esté dicho.
public final class TemporaryConstantPool {

    private static final ConstantPoolBuilder POOL = ConstantPoolBuilder.of();

    private TemporaryConstantPool() {
    }

    /** El pool en sí, para quien necesite una entrada que no tenga atajo acá. */
    public static ConstantPoolBuilder pool() {
        return POOL;
    }

    /** Un `CONSTANT_Utf8` suelto. */
    public static Utf8Entry utf8(String s) {
        if (s == null) {
            throw new NullPointerException("utf8");
        }
        synchronized (POOL) {
            return POOL.utf8Entry(s);
        }
    }

    /** Un `CONSTANT_Class` suelto. */
    public static ClassEntry classEntry(ClassDesc d) {
        if (d == null) {
            throw new NullPointerException("clase");
        }
        synchronized (POOL) {
            return POOL.classEntry(d);
        }
    }

    /** Un `CONSTANT_Class` suelto con este nombre interno. */
    public static ClassEntry classEntry(Utf8Entry name) {
        synchronized (POOL) {
            return POOL.classEntry(name);
        }
    }

    /** Un `CONSTANT_NameAndType` suelto. */
    public static NameAndTypeEntry nameAndType(Utf8Entry name, Utf8Entry type) {
        synchronized (POOL) {
            return POOL.nameAndTypeEntry(name, type);
        }
    }

    /** Un `CONSTANT_Fieldref` suelto. */
    public static FieldRefEntry fieldRef(ClassEntry owner, NameAndTypeEntry nat) {
        synchronized (POOL) {
            return POOL.fieldRefEntry(owner, nat);
        }
    }

    /** Un `CONSTANT_Methodref` suelto. */
    public static MethodRefEntry methodRef(ClassEntry owner, NameAndTypeEntry nat) {
        synchronized (POOL) {
            return POOL.methodRefEntry(owner, nat);
        }
    }

    /** Un `CONSTANT_InterfaceMethodref` suelto. */
    public static InterfaceMethodRefEntry interfaceMethodRef(ClassEntry owner, NameAndTypeEntry nat) {
        synchronized (POOL) {
            return POOL.interfaceMethodRefEntry(owner, nat);
        }
    }

    /** Un `CONSTANT_Integer` suelto. */
    public static IntegerEntry intEntry(int value) {
        synchronized (POOL) {
            return POOL.intEntry(value);
        }
    }

    /** Un `CONSTANT_Long` suelto. */
    public static LongEntry longEntry(long value) {
        synchronized (POOL) {
            return POOL.longEntry(value);
        }
    }

    /** Un `CONSTANT_Float` suelto. */
    public static FloatEntry floatEntry(float value) {
        synchronized (POOL) {
            return POOL.floatEntry(value);
        }
    }

    /** Un `CONSTANT_Double` suelto. */
    public static DoubleEntry doubleEntry(double value) {
        synchronized (POOL) {
            return POOL.doubleEntry(value);
        }
    }
}
