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

// Un pool de constantes al que se le pueden agregar entradas. Es la mitad de escritura de
// {@link ConstantPool}: cada `xxxEntry(...)` devuelve la entrada que representa ese valor,
// creándola si no estaba y reutilizándola si sí — el pool no tiene duplicados.
//
// Los `abstract` de acá son las formas primitivas, las que de verdad tocan el pool. Todo lo demás
// son `default` que traducen un descriptor nominal (`ClassDesc`, `MethodTypeDesc`, …) a esas formas;
// esa división es la misma del JDK y es la que hace que una implementación tenga que escribir veinte
// métodos y no cuarenta.
//
// Diferencia con el JDK, y es de comportamiento: `canWriteDirect` acá sólo es cierto cuando el pool
// que se le pasa es **este mismo**. El JDK responde que sí también cuando el pool destino es un
// superconjunto por construcción (el caso de `of(ClassModel)`), lo que le permite copiar bytes de
// atributos sin reescribir índices. Contestar que no de más es seguro — obliga a reconstruir, nunca
// a escribir un índice equivocado.
public interface ConstantPoolBuilder extends ConstantPool {

    /** Un pool nuevo y vacío. */
    public static ConstantPoolBuilder of() {
        return new jdk.internal.classfile.impl.ConstantPoolBuilderImpl(null);
    }

    /** Un pool que empieza con todas las entradas de `classModel`, en sus mismos índices. */
    public static ConstantPoolBuilder of(ClassModel classModel) {
        return new jdk.internal.classfile.impl.ConstantPoolBuilderImpl(classModel);
    }

    /** Si los índices de `constantPool` se pueden escribir tal cual contra este pool. */
    boolean canWriteDirect(ConstantPool constantPool);

    /** La entrada `CONSTANT_Utf8` con este contenido. */
    Utf8Entry utf8Entry(String s);

    /** El descriptor de campo de `desc`, como `Utf8`. */
    default Utf8Entry utf8Entry(ClassDesc desc) {
        return utf8Entry(desc.descriptorString());
    }

    /** El descriptor de método de `desc`, como `Utf8`. */
    default Utf8Entry utf8Entry(MethodTypeDesc desc) {
        return utf8Entry(desc.descriptorString());
    }

    /** La entrada `CONSTANT_Class` con este nombre interno. */
    ClassEntry classEntry(Utf8Entry ne);

    /**
     * La entrada `CONSTANT_Class` de `classDesc`. Para un arreglo el `Utf8` lleva el descriptor
     * (`[[I`); para una clase o interfaz, el nombre interno (`java/lang/String`). Un primitivo no
     * tiene entrada posible y es un error.
     */
    default ClassEntry classEntry(ClassDesc classDesc) {
        return classEntry(utf8Entry(nombreInterno(classDesc)));
    }

    /** La entrada `CONSTANT_Package` con este nombre interno. */
    PackageEntry packageEntry(Utf8Entry nameEntry);

    /** La entrada `CONSTANT_Package` de `packageDesc`. */
    default PackageEntry packageEntry(PackageDesc packageDesc) {
        return packageEntry(utf8Entry(packageDesc.internalName()));
    }

    /** La entrada `CONSTANT_Module` con este nombre. */
    ModuleEntry moduleEntry(Utf8Entry moduleName);

    /** La entrada `CONSTANT_Module` de `moduleDesc`. */
    default ModuleEntry moduleEntry(ModuleDesc moduleDesc) {
        return moduleEntry(utf8Entry(moduleDesc.name()));
    }

    /** La entrada `CONSTANT_NameAndType` con este nombre y este descriptor. */
    NameAndTypeEntry nameAndTypeEntry(Utf8Entry nameEntry, Utf8Entry typeEntry);

    /** `CONSTANT_NameAndType` para un campo. */
    default NameAndTypeEntry nameAndTypeEntry(String name, ClassDesc type) {
        return nameAndTypeEntry(utf8Entry(name), utf8Entry(type));
    }

    /** `CONSTANT_NameAndType` para un método. */
    default NameAndTypeEntry nameAndTypeEntry(String name, MethodTypeDesc type) {
        return nameAndTypeEntry(utf8Entry(name), utf8Entry(type));
    }

    /** La entrada `CONSTANT_Fieldref`. */
    FieldRefEntry fieldRefEntry(ClassEntry owner, NameAndTypeEntry nameAndType);

    /** `CONSTANT_Fieldref` a partir de los descriptores nominales. */
    default FieldRefEntry fieldRefEntry(ClassDesc owner, String name, ClassDesc type) {
        return fieldRefEntry(classEntry(owner), nameAndTypeEntry(name, type));
    }

    /** La entrada `CONSTANT_Methodref`. */
    MethodRefEntry methodRefEntry(ClassEntry owner, NameAndTypeEntry nameAndType);

    /** `CONSTANT_Methodref` a partir de los descriptores nominales. */
    default MethodRefEntry methodRefEntry(ClassDesc owner, String name, MethodTypeDesc type) {
        return methodRefEntry(classEntry(owner), nameAndTypeEntry(name, type));
    }

    /** La entrada `CONSTANT_InterfaceMethodref`. */
    InterfaceMethodRefEntry interfaceMethodRefEntry(ClassEntry owner, NameAndTypeEntry nameAndType);

    /** `CONSTANT_InterfaceMethodref` a partir de los descriptores nominales. */
    default InterfaceMethodRefEntry interfaceMethodRefEntry(ClassDesc owner, String name,
            MethodTypeDesc type) {
        return interfaceMethodRefEntry(classEntry(owner), nameAndTypeEntry(name, type));
    }

    /** La entrada `CONSTANT_MethodType` de este descriptor. */
    MethodTypeEntry methodTypeEntry(MethodTypeDesc descriptor);

    /** La entrada `CONSTANT_MethodType` con este `Utf8` como descriptor. */
    MethodTypeEntry methodTypeEntry(Utf8Entry descriptor);

    /** La entrada `CONSTANT_MethodHandle` de `descriptor`. */
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

    /** La entrada `CONSTANT_MethodHandle` con este `reference_kind` y esta referencia. */
    MethodHandleEntry methodHandleEntry(int refKind, MemberRefEntry reference);

    /** La entrada `CONSTANT_InvokeDynamic` de `dcsd`. */
    default InvokeDynamicEntry invokeDynamicEntry(DynamicCallSiteDesc dcsd) {
        return invokeDynamicEntry(
                bsmEntry((DirectMethodHandleDesc) dcsd.bootstrapMethod(),
                        listaDe(dcsd.bootstrapArgs())),
                nameAndTypeEntry(dcsd.invocationName(), dcsd.invocationType()));
    }

    /** La entrada `CONSTANT_InvokeDynamic` con este método de arranque y este `NameAndType`. */
    InvokeDynamicEntry invokeDynamicEntry(BootstrapMethodEntry bootstrapMethodEntry,
            NameAndTypeEntry nameAndType);

    /** La entrada `CONSTANT_Dynamic` de `dcd`. */
    default ConstantDynamicEntry constantDynamicEntry(DynamicConstantDesc<?> dcd) {
        return constantDynamicEntry(
                bsmEntry(dcd.bootstrapMethod(), listaDe(dcd.bootstrapArgs())),
                nameAndTypeEntry(dcd.constantName(), dcd.constantType()));
    }

    /** La entrada `CONSTANT_Dynamic` con este método de arranque y este `NameAndType`. */
    ConstantDynamicEntry constantDynamicEntry(BootstrapMethodEntry bootstrapMethodEntry,
            NameAndTypeEntry nameAndType);

    /** La entrada `CONSTANT_Integer` con este valor. */
    IntegerEntry intEntry(int value);

    /** La entrada `CONSTANT_Float` con este valor. */
    FloatEntry floatEntry(float value);

    /** La entrada `CONSTANT_Long` con este valor. */
    LongEntry longEntry(long value);

    /** La entrada `CONSTANT_Double` con este valor. */
    DoubleEntry doubleEntry(double value);

    /** La entrada `CONSTANT_String` que apunta a este `Utf8`. */
    StringEntry stringEntry(Utf8Entry utf8);

    /** La entrada `CONSTANT_String` con este contenido. */
    default StringEntry stringEntry(String value) {
        return stringEntry(utf8Entry(value));
    }

    /** La entrada de valor constante que corresponde a `c`. */
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
        throw new IllegalArgumentException("no es un valor constante: " + c);
    }

    /** La entrada cargable con `ldc` que corresponde a `c`. */
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

    /** La entrada de `BootstrapMethods` para este handle y estos argumentos estáticos. */
    default BootstrapMethodEntry bsmEntry(DirectMethodHandleDesc methodReference,
            List<ConstantDesc> arguments) {
        List<LoadableConstantEntry> args = new ArrayList<LoadableConstantEntry>();
        for (int i = 0; i < arguments.size(); i++) {
            args.add(loadableConstantEntry(arguments.get(i)));
        }
        return bsmEntry(methodHandleEntry(methodReference), args);
    }

    /** La entrada de `BootstrapMethods` con este handle y estos argumentos ya en el pool. */
    BootstrapMethodEntry bsmEntry(MethodHandleEntry methodReference,
            List<LoadableConstantEntry> arguments);

    // --- Ayudantes de los `default` de arriba. No son API: son estáticos de paquete. ---

    /** El nombre que lleva el `Utf8` de una `CONSTANT_Class` para este descriptor. */
    private static String nombreInterno(ClassDesc classDesc) {
        String d = classDesc.descriptorString();
        if (d.charAt(0) == '[') {
            return d;
        }
        if (d.charAt(0) != 'L') {
            throw new IllegalArgumentException("un primitivo no tiene CONSTANT_Class: " + d);
        }
        return d.substring(1, d.length() - 1);
    }

    /** Un `List` a partir del arreglo de argumentos estáticos de un descriptor dinámico. */
    private static List<ConstantDesc> listaDe(ConstantDesc[] args) {
        List<ConstantDesc> lista = new ArrayList<ConstantDesc>();
        for (int i = 0; i < args.length; i++) {
            lista.add(args[i]);
        }
        return lista;
    }
}
