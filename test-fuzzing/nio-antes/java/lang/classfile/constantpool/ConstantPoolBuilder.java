package java.lang.classfile.constantpool;

import java.lang.classfile.BootstrapMethodEntry;
import java.lang.classfile.ClassModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.constant.ModuleDesc;
import java.lang.constant.PackageDesc;
import java.util.ArrayList;
import java.util.List;

// A constant pool entries can be added to. It is {@link ConstantPool}'s writing half: each
// `xxxEntry(...)` returns the entry representing that value, creating it if it was not there and
// reusing it if it was -- the pool has no duplicates.
//
// The `abstract` ones here are the primitive forms, the ones that really touch the pool. Everything
// else is `default` methods translating a nominal descriptor (`ClassDesc`, `MethodTypeDesc`, ...)
// into those forms; that division is the JDK's own and it is what makes an implementation have to
// write twenty methods and not forty.
//
// A difference from the JDK, and one of behaviour: `canWriteDirect` here is only true when the pool
// handed to it is **this very one**. The JDK also answers yes when the destination pool is a superset
// by construction (the `of(ClassModel)` case), which lets it copy attribute bytes without rewriting
// indices. Answering no too often is safe -- it forces rebuilding, never writing a wrong index.
public interface ConstantPoolBuilder extends ConstantPool {

    /** A new, empty pool. */
    public static ConstantPoolBuilder of() {
        return new jdk.internal.classfile.impl.ConstantPoolBuilderImpl(null);
    }

    /** A pool starting with every entry of `classModel`, at their same indices. */
    public static ConstantPoolBuilder of(ClassModel classModel) {
        return new jdk.internal.classfile.impl.ConstantPoolBuilderImpl(classModel);
    }

    /** Whether `constantPool`'s indices can be written as they stand against this pool. */
    boolean canWriteDirect(ConstantPool constantPool);

    /** The `CONSTANT_Utf8` entry with these contents. */
    Utf8Entry utf8Entry(String s);

    /** `desc`'s field descriptor, as a `Utf8`. */
    default Utf8Entry utf8Entry(ClassDesc desc) {
        return utf8Entry(desc.descriptorString());
    }

    /** `desc`'s method descriptor, as a `Utf8`. */
    default Utf8Entry utf8Entry(MethodTypeDesc desc) {
        return utf8Entry(desc.descriptorString());
    }

    /** The `CONSTANT_Class` entry with this internal name. */
    ClassEntry classEntry(Utf8Entry ne);

    /**
     * `classDesc`'s `CONSTANT_Class` entry. For an array the `Utf8` carries the descriptor (`[[I`);
     * for a class or interface, the internal name (`java/lang/String`). A primitive has no possible
     * entry and is an error.
     */
    default ClassEntry classEntry(ClassDesc classDesc) {
        return classEntry(utf8Entry(internalNameOf(classDesc)));
    }

    /** The `CONSTANT_Package` entry with this internal name. */
    PackageEntry packageEntry(Utf8Entry nameEntry);

    /** `packageDesc`'s `CONSTANT_Package` entry. */
    default PackageEntry packageEntry(PackageDesc packageDesc) {
        return packageEntry(utf8Entry(packageDesc.internalName()));
    }

    /** The `CONSTANT_Module` entry with this name. */
    ModuleEntry moduleEntry(Utf8Entry moduleName);

    /** `moduleDesc`'s `CONSTANT_Module` entry. */
    default ModuleEntry moduleEntry(ModuleDesc moduleDesc) {
        return moduleEntry(utf8Entry(moduleDesc.name()));
    }

    /** The `CONSTANT_NameAndType` entry with this name and this descriptor. */
    NameAndTypeEntry nameAndTypeEntry(Utf8Entry nameEntry, Utf8Entry typeEntry);

    /** `CONSTANT_NameAndType` for a field. */
    default NameAndTypeEntry nameAndTypeEntry(String name, ClassDesc type) {
        return nameAndTypeEntry(utf8Entry(name), utf8Entry(type));
    }

    /** `CONSTANT_NameAndType` for a method. */
    default NameAndTypeEntry nameAndTypeEntry(String name, MethodTypeDesc type) {
        return nameAndTypeEntry(utf8Entry(name), utf8Entry(type));
    }

    /** The `CONSTANT_Fieldref` entry. */
    FieldRefEntry fieldRefEntry(ClassEntry owner, NameAndTypeEntry nameAndType);

    /** `CONSTANT_Fieldref` out of the nominal descriptors. */
    default FieldRefEntry fieldRefEntry(ClassDesc owner, String name, ClassDesc type) {
        return fieldRefEntry(classEntry(owner), nameAndTypeEntry(name, type));
    }

    /** The `CONSTANT_Methodref` entry. */
    MethodRefEntry methodRefEntry(ClassEntry owner, NameAndTypeEntry nameAndType);

    /** `CONSTANT_Methodref` out of the nominal descriptors. */
    default MethodRefEntry methodRefEntry(ClassDesc owner, String name, MethodTypeDesc type) {
        return methodRefEntry(classEntry(owner), nameAndTypeEntry(name, type));
    }

    /** The `CONSTANT_InterfaceMethodref` entry. */
    InterfaceMethodRefEntry interfaceMethodRefEntry(ClassEntry owner, NameAndTypeEntry nameAndType);

    /** `CONSTANT_InterfaceMethodref` out of the nominal descriptors. */
    default InterfaceMethodRefEntry interfaceMethodRefEntry(ClassDesc owner, String name,
            MethodTypeDesc type) {
        return interfaceMethodRefEntry(classEntry(owner), nameAndTypeEntry(name, type));
    }

    /** This descriptor's `CONSTANT_MethodType` entry. */
    MethodTypeEntry methodTypeEntry(MethodTypeDesc descriptor);

    /** The `CONSTANT_MethodType` entry with this `Utf8` as its descriptor. */
    MethodTypeEntry methodTypeEntry(Utf8Entry descriptor);

    /** `descriptor`'s `CONSTANT_MethodHandle` entry. */
    default MethodHandleEntry methodHandleEntry(DirectMethodHandleDesc descriptor) {
        int refKind = descriptor.refKind();
        MemberRefEntry ref;
        if (refKind <= 4) {
            ref = fieldRefEntry(descriptor.owner(), descriptor.methodName(),
                    ClassDesc.ofDescriptor(descriptor.lookupDescriptor()));
        } else if (descriptor.isOwnerInterface()) {
            ref = interfaceMethodRefEntry(descriptor.owner(), descriptor.methodName(),
                    MethodTypeDesc.ofDescriptor(descriptor.lookupDescriptor()));
        } else {
            ref = methodRefEntry(descriptor.owner(), descriptor.methodName(),
                    MethodTypeDesc.ofDescriptor(descriptor.lookupDescriptor()));
        }
        return methodHandleEntry(refKind, ref);
    }

    /** The `CONSTANT_MethodHandle` entry with this `reference_kind` and this reference. */
    MethodHandleEntry methodHandleEntry(int refKind, MemberRefEntry reference);

    /** `dcsd`'s `CONSTANT_InvokeDynamic` entry. */
    default InvokeDynamicEntry invokeDynamicEntry(DynamicCallSiteDesc dcsd) {
        return invokeDynamicEntry(
                bsmEntry((DirectMethodHandleDesc) dcsd.bootstrapMethod(),
                        listOf(dcsd.bootstrapArgs())),
                nameAndTypeEntry(dcsd.invocationName(), dcsd.invocationType()));
    }

    /** The `CONSTANT_InvokeDynamic` entry with this bootstrap method and this
     * `NameAndType`. */
    InvokeDynamicEntry invokeDynamicEntry(BootstrapMethodEntry bootstrapMethodEntry,
            NameAndTypeEntry nameAndType);

    /** `dcd`'s `CONSTANT_Dynamic` entry. */
    default ConstantDynamicEntry constantDynamicEntry(DynamicConstantDesc<?> dcd) {
        return constantDynamicEntry(
                bsmEntry(dcd.bootstrapMethod(), listOf(dcd.bootstrapArgs())),
                nameAndTypeEntry(dcd.constantName(), dcd.constantType()));
    }

    /** The `CONSTANT_Dynamic` entry with this bootstrap method and this `NameAndType`. */
    ConstantDynamicEntry constantDynamicEntry(BootstrapMethodEntry bootstrapMethodEntry,
            NameAndTypeEntry nameAndType);

    /** The `CONSTANT_Integer` entry with this value. */
    IntegerEntry intEntry(int value);

    /** The `CONSTANT_Float` entry with this value. */
    FloatEntry floatEntry(float value);

    /** The `CONSTANT_Long` entry with this value. */
    LongEntry longEntry(long value);

    /** The `CONSTANT_Double` entry with this value. */
    DoubleEntry doubleEntry(double value);

    /** The `CONSTANT_String` entry pointing at this `Utf8`. */
    StringEntry stringEntry(Utf8Entry utf8);

    /** The `CONSTANT_String` entry with these contents. */
    default StringEntry stringEntry(String value) {
        return stringEntry(utf8Entry(value));
    }

    /** The value-constant entry corresponding to `c`. */
    default ConstantValueEntry constantValueEntry(ConstantDesc c) {
        if (c instanceof Integer) {
            return intEntry(((Integer) c).intValue());
        }
        if (c instanceof String) {
            return stringEntry((String) c);
        }
        if (c instanceof Long) {
            return longEntry(((Long) c).longValue());
        }
        if (c instanceof Float) {
            return floatEntry(((Float) c).floatValue());
        }
        if (c instanceof Double) {
            return doubleEntry(((Double) c).doubleValue());
        }
        throw new IllegalArgumentException("not a constant value: " + c);
    }

    /** The `ldc`-loadable entry corresponding to `c`. */
    default LoadableConstantEntry loadableConstantEntry(ConstantDesc c) {
        if (c instanceof ClassDesc) {
            return classEntry((ClassDesc) c);
        }
        if (c instanceof MethodTypeDesc) {
            return methodTypeEntry((MethodTypeDesc) c);
        }
        if (c instanceof DirectMethodHandleDesc) {
            return methodHandleEntry((DirectMethodHandleDesc) c);
        }
        if (c instanceof DynamicConstantDesc) {
            return constantDynamicEntry((DynamicConstantDesc<?>) c);
        }
        return constantValueEntry(c);
    }

    /** The `BootstrapMethods` entry for this handle and these static arguments. */
    default BootstrapMethodEntry bsmEntry(DirectMethodHandleDesc methodReference,
            List<ConstantDesc> arguments) {
        List<LoadableConstantEntry> args = new ArrayList<LoadableConstantEntry>();
        for (int i = 0; i < arguments.size(); i++) {
            args.add(loadableConstantEntry(arguments.get(i)));
        }
        return bsmEntry(methodHandleEntry(methodReference), args);
    }

    /** The `BootstrapMethods` entry with this handle and these arguments already in the pool. */
    BootstrapMethodEntry bsmEntry(MethodHandleEntry methodReference,
            List<LoadableConstantEntry> arguments);

    // --- Helpers for the `default` methods above. They are not API: package-private statics. ---

    /** The name a `CONSTANT_Class`'s `Utf8` carries for this descriptor. */
    private static String internalNameOf(ClassDesc classDesc) {
        String d = classDesc.descriptorString();
        if (d.charAt(0) == '[') {
            return d;
        }
        if (d.charAt(0) != 'L') {
            throw new IllegalArgumentException("a primitive has no CONSTANT_Class: " + d);
        }
        return d.substring(1, d.length() - 1);
    }

    /** A `List` out of a dynamic descriptor's array of static arguments. */
    private static List<ConstantDesc> listOf(ConstantDesc[] args) {
        List<ConstantDesc> list = new ArrayList<ConstantDesc>();
        for (int i = 0; i < args.length; i++) {
            list.add(args[i]);
        }
        return list;
    }
}
