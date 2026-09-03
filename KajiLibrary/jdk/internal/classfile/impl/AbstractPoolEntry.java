package jdk.internal.classfile.impl;

import java.lang.classfile.BootstrapMethodEntry;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.ConstantDynamicEntry;
import java.lang.classfile.constantpool.DoubleEntry;
import java.lang.classfile.constantpool.FieldRefEntry;
import java.lang.classfile.constantpool.FloatEntry;
import java.lang.classfile.constantpool.IntegerEntry;
import java.lang.classfile.constantpool.InterfaceMethodRefEntry;
import java.lang.classfile.constantpool.InvokeDynamicEntry;
import java.lang.classfile.constantpool.LongEntry;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.MethodHandleEntry;
import java.lang.classfile.constantpool.MethodRefEntry;
import java.lang.classfile.constantpool.MethodTypeEntry;
import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.PackageEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.StringEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DirectMethodHandleDesc.Kind;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.constant.ModuleDesc;
import java.lang.constant.PackageDesc;

// La base de todas las entradas del pool: el pool al que pertenecen y el índice que ocupan. Las
// implementaciones concretas están todas en este archivo, package-private, porque son diecisiete
// clases de tres campos cada una y separarlas en diecisiete archivos no aclararía nada.
//
// A diferencia del JDK, las entradas de acá se materializan **enteras** al leer el archivo: sus
// referencias a otras entradas ya están resueltas y validadas cuando el `ClassModel` existe. El JDK
// resuelve perezosamente, lo que es más rápido para leer un atributo suelto; esto es más simple y
// —lo que importa más— hace que un pool mal formado falle al abrirse y no diez llamadas después.
public abstract class AbstractPoolEntry implements PoolEntry {

    final ConstantPool pool;
    final int index;

    AbstractPoolEntry(ConstantPool pool, int index) {
        this.pool = pool;
        this.index = index;
    }

    public ConstantPool constantPool() {
        return this.pool;
    }

    public int index() {
        return this.index;
    }

    public int width() {
        return 1;
    }
}

// `CONSTANT_Utf8`. Guarda el `String` ya decodificado del UTF-8 modificado: el formato admite
// secuencias que `String` no produce (el `NUL` de dos bytes, los sustitutos de seis), así que la
// decodificación no se puede delegar al juego de caracteres estándar.
final class Utf8EntryImpl extends AbstractPoolEntry implements Utf8Entry {

    private final String valor;

    Utf8EntryImpl(ConstantPool pool, int index, String valor) {
        super(pool, index);
        this.valor = valor;
    }

    public int tag() {
        return PoolEntry.TAG_UTF8;
    }

    public String stringValue() {
        return this.valor;
    }

    public ConstantDesc constantValue() {
        return this.valor;
    }

    public boolean equalsString(String s) {
        return this.valor.equals(s);
    }

    public boolean isFieldType(ClassDesc desc) {
        return this.valor.equals(desc.descriptorString());
    }

    public boolean isMethodType(MethodTypeDesc desc) {
        return this.valor.equals(desc.descriptorString());
    }

    public int length() {
        return this.valor.length();
    }

    public char charAt(int i) {
        return this.valor.charAt(i);
    }

    public CharSequence subSequence(int desde, int hasta) {
        return this.valor.substring(desde, hasta);
    }

    public String toString() {
        return this.valor;
    }
}

// `CONSTANT_Integer`.
final class IntegerEntryImpl extends AbstractPoolEntry implements IntegerEntry {

    private final int valor;

    IntegerEntryImpl(ConstantPool pool, int index, int valor) {
        super(pool, index);
        this.valor = valor;
    }

    public int tag() {
        return PoolEntry.TAG_INTEGER;
    }

    public int intValue() {
        return this.valor;
    }

    public ConstantDesc constantValue() {
        return Integer.valueOf(this.valor);
    }

    public String toString() {
        return "int " + this.valor;
    }
}

// `CONSTANT_Float`.
final class FloatEntryImpl extends AbstractPoolEntry implements FloatEntry {

    private final float valor;

    FloatEntryImpl(ConstantPool pool, int index, float valor) {
        super(pool, index);
        this.valor = valor;
    }

    public int tag() {
        return PoolEntry.TAG_FLOAT;
    }

    public float floatValue() {
        return this.valor;
    }

    public ConstantDesc constantValue() {
        return Float.valueOf(this.valor);
    }

    public String toString() {
        return "float " + this.valor;
    }
}

// `CONSTANT_Long`. Ocupa dos ranuras.
final class LongEntryImpl extends AbstractPoolEntry implements LongEntry {

    private final long valor;

    LongEntryImpl(ConstantPool pool, int index, long valor) {
        super(pool, index);
        this.valor = valor;
    }

    public int tag() {
        return PoolEntry.TAG_LONG;
    }

    public int width() {
        return 2;
    }

    public long longValue() {
        return this.valor;
    }

    public ConstantDesc constantValue() {
        return Long.valueOf(this.valor);
    }

    public String toString() {
        return "long " + this.valor;
    }
}

// `CONSTANT_Double`. Ocupa dos ranuras.
final class DoubleEntryImpl extends AbstractPoolEntry implements DoubleEntry {

    private final double valor;

    DoubleEntryImpl(ConstantPool pool, int index, double valor) {
        super(pool, index);
        this.valor = valor;
    }

    public int tag() {
        return PoolEntry.TAG_DOUBLE;
    }

    public int width() {
        return 2;
    }

    public double doubleValue() {
        return this.valor;
    }

    public ConstantDesc constantValue() {
        return Double.valueOf(this.valor);
    }

    public String toString() {
        return "double " + this.valor;
    }
}

// `CONSTANT_Class`.
final class ClassEntryImpl extends AbstractPoolEntry implements ClassEntry {

    private final Utf8Entry name;
    private ClassDesc simbolo;

    ClassEntryImpl(ConstantPool pool, int index, Utf8Entry name) {
        super(pool, index);
        this.name = name;
    }

    public int tag() {
        return PoolEntry.TAG_CLASS;
    }

    public Utf8Entry name() {
        return this.name;
    }

    public String asInternalName() {
        return this.name.stringValue();
    }

    public ClassDesc asSymbol() {
        if (this.simbolo == null) {
            String n = this.name.stringValue();
            if (n.length() == 0) {
                throw new java.lang.classfile.constantpool.ConstantPoolException(
                        "CONSTANT_Class con nombre vacío en el índice " + this.index);
            }
            this.simbolo = n.charAt(0) == '['
                    ? ClassDesc.ofDescriptor(n)
                    : ClassDesc.ofInternalName(n);
        }
        return this.simbolo;
    }

    public boolean matches(ClassDesc desc) {
        return asSymbol().equals(desc);
    }

    public String toString() {
        return "class " + this.name.stringValue();
    }
}

// `CONSTANT_String`.
final class StringEntryImpl extends AbstractPoolEntry implements StringEntry {

    private final Utf8Entry utf8;

    StringEntryImpl(ConstantPool pool, int index, Utf8Entry utf8) {
        super(pool, index);
        this.utf8 = utf8;
    }

    public int tag() {
        return PoolEntry.TAG_STRING;
    }

    public Utf8Entry utf8() {
        return this.utf8;
    }

    public String stringValue() {
        return this.utf8.stringValue();
    }

    public boolean equalsString(String s) {
        return this.utf8.equalsString(s);
    }

    public ConstantDesc constantValue() {
        return this.utf8.stringValue();
    }

    public String toString() {
        return "String " + this.utf8.stringValue();
    }
}

// `CONSTANT_NameAndType`.
final class NameAndTypeEntryImpl extends AbstractPoolEntry implements NameAndTypeEntry {

    private final Utf8Entry name;
    private final Utf8Entry type;

    NameAndTypeEntryImpl(ConstantPool pool, int index, Utf8Entry name, Utf8Entry type) {
        super(pool, index);
        this.name = name;
        this.type = type;
    }

    public int tag() {
        return PoolEntry.TAG_NAME_AND_TYPE;
    }

    public Utf8Entry name() {
        return this.name;
    }

    public Utf8Entry type() {
        return this.type;
    }

    public String toString() {
        return this.name.stringValue() + ":" + this.type.stringValue();
    }
}

// `CONSTANT_Fieldref`.
final class FieldRefEntryImpl extends AbstractPoolEntry implements FieldRefEntry {

    private final ClassEntry duenio;
    private final NameAndTypeEntry nat;

    FieldRefEntryImpl(ConstantPool pool, int index, ClassEntry duenio, NameAndTypeEntry nat) {
        super(pool, index);
        this.duenio = duenio;
        this.nat = nat;
    }

    public int tag() {
        return PoolEntry.TAG_FIELDREF;
    }

    public ClassEntry owner() {
        return this.duenio;
    }

    public NameAndTypeEntry nameAndType() {
        return this.nat;
    }

    public String toString() {
        return "Field " + this.duenio.asInternalName() + "." + this.nat.toString();
    }
}

// `CONSTANT_Methodref`.
final class MethodRefEntryImpl extends AbstractPoolEntry implements MethodRefEntry {

    private final ClassEntry duenio;
    private final NameAndTypeEntry nat;

    MethodRefEntryImpl(ConstantPool pool, int index, ClassEntry duenio, NameAndTypeEntry nat) {
        super(pool, index);
        this.duenio = duenio;
        this.nat = nat;
    }

    public int tag() {
        return PoolEntry.TAG_METHODREF;
    }

    public ClassEntry owner() {
        return this.duenio;
    }

    public NameAndTypeEntry nameAndType() {
        return this.nat;
    }

    public String toString() {
        return "Method " + this.duenio.asInternalName() + "." + this.nat.toString();
    }
}

// `CONSTANT_InterfaceMethodref`.
final class InterfaceMethodRefEntryImpl extends AbstractPoolEntry implements InterfaceMethodRefEntry {

    private final ClassEntry duenio;
    private final NameAndTypeEntry nat;

    InterfaceMethodRefEntryImpl(ConstantPool pool, int index, ClassEntry duenio,
            NameAndTypeEntry nat) {
        super(pool, index);
        this.duenio = duenio;
        this.nat = nat;
    }

    public int tag() {
        return PoolEntry.TAG_INTERFACE_METHODREF;
    }

    public ClassEntry owner() {
        return this.duenio;
    }

    public NameAndTypeEntry nameAndType() {
        return this.nat;
    }

    public String toString() {
        return "InterfaceMethod " + this.duenio.asInternalName() + "." + this.nat.toString();
    }
}

// `CONSTANT_MethodType`.
final class MethodTypeEntryImpl extends AbstractPoolEntry implements MethodTypeEntry {

    private final Utf8Entry descriptor;
    private MethodTypeDesc simbolo;

    MethodTypeEntryImpl(ConstantPool pool, int index, Utf8Entry descriptor) {
        super(pool, index);
        this.descriptor = descriptor;
    }

    public int tag() {
        return PoolEntry.TAG_METHOD_TYPE;
    }

    public Utf8Entry descriptor() {
        return this.descriptor;
    }

    public MethodTypeDesc asSymbol() {
        if (this.simbolo == null) {
            this.simbolo = MethodTypeDesc.ofDescriptor(this.descriptor.stringValue());
        }
        return this.simbolo;
    }

    public boolean matches(MethodTypeDesc desc) {
        return this.descriptor.isMethodType(desc);
    }

    public String toString() {
        return "MethodType " + this.descriptor.stringValue();
    }
}

// `CONSTANT_MethodHandle`.
final class MethodHandleEntryImpl extends AbstractPoolEntry implements MethodHandleEntry {

    private final int refKind;
    private final MemberRefEntry referencia;

    MethodHandleEntryImpl(ConstantPool pool, int index, int refKind, MemberRefEntry referencia) {
        super(pool, index);
        this.refKind = refKind;
        this.referencia = referencia;
    }

    public int tag() {
        return PoolEntry.TAG_METHOD_HANDLE;
    }

    public int kind() {
        return this.refKind;
    }

    public MemberRefEntry reference() {
        return this.referencia;
    }

    public DirectMethodHandleDesc asSymbol() {
        boolean esInterfaz = this.referencia instanceof InterfaceMethodRefEntry;
        Kind k = Kind.valueOf(this.refKind, esInterfaz);
        return MethodHandleDesc.of(k, this.referencia.owner().asSymbol(),
                this.referencia.name().stringValue(), this.referencia.type().stringValue());
    }

    public String toString() {
        return "MethodHandle " + this.refKind + " " + this.referencia.toString();
    }
}

// `CONSTANT_Dynamic`.
final class ConstantDynamicEntryImpl extends AbstractPoolEntry implements ConstantDynamicEntry {

    private final int bsmIndex;
    private final NameAndTypeEntry nat;

    ConstantDynamicEntryImpl(ConstantPool pool, int index, int bsmIndex, NameAndTypeEntry nat) {
        super(pool, index);
        this.bsmIndex = bsmIndex;
        this.nat = nat;
    }

    public int tag() {
        return PoolEntry.TAG_DYNAMIC;
    }

    public int bootstrapMethodIndex() {
        return this.bsmIndex;
    }

    public BootstrapMethodEntry bootstrap() {
        return this.pool.bootstrapMethodEntry(this.bsmIndex);
    }

    public NameAndTypeEntry nameAndType() {
        return this.nat;
    }

    public String toString() {
        return "Dynamic #" + this.bsmIndex + " " + this.nat.toString();
    }
}

// `CONSTANT_InvokeDynamic`.
final class InvokeDynamicEntryImpl extends AbstractPoolEntry implements InvokeDynamicEntry {

    private final int bsmIndex;
    private final NameAndTypeEntry nat;

    InvokeDynamicEntryImpl(ConstantPool pool, int index, int bsmIndex, NameAndTypeEntry nat) {
        super(pool, index);
        this.bsmIndex = bsmIndex;
        this.nat = nat;
    }

    public int tag() {
        return PoolEntry.TAG_INVOKE_DYNAMIC;
    }

    public int bootstrapMethodIndex() {
        return this.bsmIndex;
    }

    public BootstrapMethodEntry bootstrap() {
        return this.pool.bootstrapMethodEntry(this.bsmIndex);
    }

    public NameAndTypeEntry nameAndType() {
        return this.nat;
    }

    public String toString() {
        return "InvokeDynamic #" + this.bsmIndex + " " + this.nat.toString();
    }
}

// `CONSTANT_Module`.
final class ModuleEntryImpl extends AbstractPoolEntry implements ModuleEntry {

    private final Utf8Entry name;

    ModuleEntryImpl(ConstantPool pool, int index, Utf8Entry name) {
        super(pool, index);
        this.name = name;
    }

    public int tag() {
        return PoolEntry.TAG_MODULE;
    }

    public Utf8Entry name() {
        return this.name;
    }

    public ModuleDesc asSymbol() {
        return ModuleDesc.of(this.name.stringValue());
    }

    public boolean matches(ModuleDesc desc) {
        return this.name.equalsString(desc.name());
    }

    public String toString() {
        return "module " + this.name.stringValue();
    }
}

// `CONSTANT_Package`.
final class PackageEntryImpl extends AbstractPoolEntry implements PackageEntry {

    private final Utf8Entry name;

    PackageEntryImpl(ConstantPool pool, int index, Utf8Entry name) {
        super(pool, index);
        this.name = name;
    }

    public int tag() {
        return PoolEntry.TAG_PACKAGE;
    }

    public Utf8Entry name() {
        return this.name;
    }

    public PackageDesc asSymbol() {
        return PackageDesc.ofInternalName(this.name.stringValue());
    }

    public boolean matches(PackageDesc desc) {
        return this.name.equalsString(desc.internalName());
    }

    public String toString() {
        return "package " + this.name.stringValue();
    }
}
