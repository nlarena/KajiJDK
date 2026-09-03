package jdk.internal.classfile.impl;

import java.lang.classfile.BootstrapMethodEntry;
import java.lang.classfile.ClassModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantDynamicEntry;
import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.ConstantPoolException;
import java.lang.classfile.constantpool.DoubleEntry;
import java.lang.classfile.constantpool.FieldRefEntry;
import java.lang.classfile.constantpool.FloatEntry;
import java.lang.classfile.constantpool.IntegerEntry;
import java.lang.classfile.constantpool.InterfaceMethodRefEntry;
import java.lang.classfile.constantpool.InvokeDynamicEntry;
import java.lang.classfile.constantpool.LoadableConstantEntry;
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
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// El pool de escritura. Guarda las entradas en una lista indexada igual que el archivo —la ranura 0
// no existe, y un `long` o un `double` ocupa dos— y un mapa de deduplicación de clave textual, que
// es lo que hace que pedir dos veces el mismo `Utf8` devuelva la misma entrada y el mismo índice.
//
// Dos decisiones que vale la pena nombrar:
//
//   1. `of(ClassModel)` COPIA las entradas del modelo en vez de compartirlas, y las pone en sus
//      mismos índices. Compartirlas sería más barato, pero entonces `entry.constantPool()` de una
//      entrada de este pool devolvería el pool del lector, que no es este: una entrada que miente
//      sobre a qué pool pertenece rompe cualquier código que use esa respuesta para decidir si
//      puede escribir el índice tal cual.
//   2. Toda entrada que llega de afuera se ADOPTA: si su `constantPool()` no es este pool, se
//      reconstruye acá una equivalente. Aceptarla como está guardaría un índice del pool ajeno.
//
// La validación es la misma del lector y por la misma razón: un pool que acepta un
// `reference_kind` fuera de 1..9, o un handle de campo que apunta a un método, produce un `.class`
// que la JVM rechaza al cargarlo, y el error aparece lejísimos de donde se cometió.
public final class ConstantPoolBuilderImpl implements ConstantPoolBuilder {

    // Copias locales de las etiquetas de PoolEntry: el generador de bytecode no pliega una constante
    // de otra unidad de compilación en una etiqueta `case`. Los valores son los del JVMS §4.4.
    private static final int TAG_UTF8 = 1;
    private static final int TAG_INTEGER = 3;
    private static final int TAG_FLOAT = 4;
    private static final int TAG_LONG = 5;
    private static final int TAG_DOUBLE = 6;
    private static final int TAG_CLASS = 7;
    private static final int TAG_STRING = 8;
    private static final int TAG_FIELDREF = 9;
    private static final int TAG_METHODREF = 10;
    private static final int TAG_INTERFACE_METHODREF = 11;
    private static final int TAG_NAME_AND_TYPE = 12;
    private static final int TAG_METHOD_HANDLE = 15;
    private static final int TAG_METHOD_TYPE = 16;
    private static final int TAG_DYNAMIC = 17;
    private static final int TAG_INVOKE_DYNAMIC = 18;
    private static final int TAG_MODULE = 19;
    private static final int TAG_PACKAGE = 20;

    // El índice más alto que un `u2` del formato puede nombrar.
    private static final int INDICE_MAXIMO = 65535;

    // Indexada por índice de pool. La posición 0 y la segunda ranura de un long/double quedan null.
    private final List<PoolEntry> entradas = new ArrayList<PoolEntry>();
    private final Map<String, PoolEntry> porClave = new HashMap<String, PoolEntry>();
    private final List<BootstrapMethodEntry> bsms = new ArrayList<BootstrapMethodEntry>();
    private final Map<String, BootstrapMethodEntry> bsmPorClave =
            new HashMap<String, BootstrapMethodEntry>();

    public ConstantPoolBuilderImpl(ClassModel modelo) {
        this.entradas.add(null);
        if (modelo != null) {
            importar(modelo.constantPool());
        }
    }

    // --- ConstantPool ---

    public int size() {
        return this.entradas.size();
    }

    public PoolEntry entryByIndex(int index) {
        if (index < 1 || index >= this.entradas.size()) {
            throw new ConstantPoolException("el índice " + index + " no está en el pool");
        }
        PoolEntry e = this.entradas.get(index);
        if (e == null) {
            throw new ConstantPoolException("el índice " + index + " no es una entrada del pool");
        }
        return e;
    }

    public <T extends PoolEntry> T entryByIndex(int index, Class<T> cls) {
        PoolEntry e = entryByIndex(index);
        if (!cls.isInstance(e)) {
            throw new ConstantPoolException("el índice " + index + " es "
                    + e.getClass().getSimpleName() + " y no " + cls.getSimpleName());
        }
        return (T) e;
    }

    public BootstrapMethodEntry bootstrapMethodEntry(int index) {
        if (index < 0 || index >= this.bsms.size()) {
            throw new ConstantPoolException(
                    "el índice " + index + " no está en la tabla de BootstrapMethods");
        }
        return this.bsms.get(index);
    }

    public int bootstrapMethodCount() {
        return this.bsms.size();
    }

    // --- ConstantPoolBuilder: las formas primitivas ---

    // Ver la nota de alcance en `ConstantPoolBuilder`: sólo este mismo pool.
    public boolean canWriteDirect(ConstantPool constantPool) {
        return constantPool == this;
    }

    public Utf8Entry utf8Entry(String s) {
        if (s == null) {
            throw new NullPointerException("utf8Entry(null)");
        }
        String clave = "u:" + s;
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (Utf8Entry) ya;
        }
        Utf8EntryImpl e = new Utf8EntryImpl(this, proximoIndice(), s);
        return (Utf8Entry) agregar(clave, e, 1);
    }

    public ClassEntry classEntry(Utf8Entry ne) {
        Utf8Entry n = adoptar(ne);
        String clave = "c:" + n.index();
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (ClassEntry) ya;
        }
        ClassEntryImpl e = new ClassEntryImpl(this, proximoIndice(), n);
        return (ClassEntry) agregar(clave, e, 1);
    }

    public PackageEntry packageEntry(Utf8Entry nameEntry) {
        Utf8Entry n = adoptar(nameEntry);
        String clave = "p:" + n.index();
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (PackageEntry) ya;
        }
        PackageEntryImpl e = new PackageEntryImpl(this, proximoIndice(), n);
        return (PackageEntry) agregar(clave, e, 1);
    }

    public ModuleEntry moduleEntry(Utf8Entry moduleName) {
        Utf8Entry n = adoptar(moduleName);
        String clave = "m:" + n.index();
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (ModuleEntry) ya;
        }
        ModuleEntryImpl e = new ModuleEntryImpl(this, proximoIndice(), n);
        return (ModuleEntry) agregar(clave, e, 1);
    }

    public NameAndTypeEntry nameAndTypeEntry(Utf8Entry nameEntry, Utf8Entry typeEntry) {
        Utf8Entry n = adoptar(nameEntry);
        Utf8Entry t = adoptar(typeEntry);
        String clave = "n:" + n.index() + ":" + t.index();
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (NameAndTypeEntry) ya;
        }
        NameAndTypeEntryImpl e = new NameAndTypeEntryImpl(this, proximoIndice(), n, t);
        return (NameAndTypeEntry) agregar(clave, e, 1);
    }

    public FieldRefEntry fieldRefEntry(ClassEntry owner, NameAndTypeEntry nameAndType) {
        ClassEntry o = adoptar(owner);
        NameAndTypeEntry nt = adoptar(nameAndType);
        String clave = "F:" + o.index() + ":" + nt.index();
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (FieldRefEntry) ya;
        }
        FieldRefEntryImpl e = new FieldRefEntryImpl(this, proximoIndice(), o, nt);
        return (FieldRefEntry) agregar(clave, e, 1);
    }

    public MethodRefEntry methodRefEntry(ClassEntry owner, NameAndTypeEntry nameAndType) {
        ClassEntry o = adoptar(owner);
        NameAndTypeEntry nt = adoptar(nameAndType);
        String clave = "M:" + o.index() + ":" + nt.index();
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (MethodRefEntry) ya;
        }
        MethodRefEntryImpl e = new MethodRefEntryImpl(this, proximoIndice(), o, nt);
        return (MethodRefEntry) agregar(clave, e, 1);
    }

    public InterfaceMethodRefEntry interfaceMethodRefEntry(ClassEntry owner,
            NameAndTypeEntry nameAndType) {
        ClassEntry o = adoptar(owner);
        NameAndTypeEntry nt = adoptar(nameAndType);
        String clave = "I:" + o.index() + ":" + nt.index();
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (InterfaceMethodRefEntry) ya;
        }
        InterfaceMethodRefEntryImpl e =
                new InterfaceMethodRefEntryImpl(this, proximoIndice(), o, nt);
        return (InterfaceMethodRefEntry) agregar(clave, e, 1);
    }

    public MethodTypeEntry methodTypeEntry(MethodTypeDesc descriptor) {
        return methodTypeEntry(utf8Entry(descriptor.descriptorString()));
    }

    public MethodTypeEntry methodTypeEntry(Utf8Entry descriptor) {
        Utf8Entry d = adoptar(descriptor);
        String clave = "t:" + d.index();
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (MethodTypeEntry) ya;
        }
        MethodTypeEntryImpl e = new MethodTypeEntryImpl(this, proximoIndice(), d);
        return (MethodTypeEntry) agregar(clave, e, 1);
    }

    public MethodHandleEntry methodHandleEntry(int refKind, MemberRefEntry reference) {
        if (refKind < 1 || refKind > 9) {
            throw new IllegalArgumentException("reference_kind " + refKind + " fuera de 1..9");
        }
        MemberRefEntry r = adoptar(reference);
        exigirCoherenciaDeHandle(refKind, r);
        String clave = "h:" + refKind + ":" + r.index();
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (MethodHandleEntry) ya;
        }
        MethodHandleEntryImpl e = new MethodHandleEntryImpl(this, proximoIndice(), refKind, r);
        return (MethodHandleEntry) agregar(clave, e, 1);
    }

    public InvokeDynamicEntry invokeDynamicEntry(BootstrapMethodEntry bootstrapMethodEntry,
            NameAndTypeEntry nameAndType) {
        int bsm = adoptar(bootstrapMethodEntry).bsmIndex();
        NameAndTypeEntry nt = adoptar(nameAndType);
        String clave = "y:" + bsm + ":" + nt.index();
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (InvokeDynamicEntry) ya;
        }
        InvokeDynamicEntryImpl e = new InvokeDynamicEntryImpl(this, proximoIndice(), bsm, nt);
        return (InvokeDynamicEntry) agregar(clave, e, 1);
    }

    public ConstantDynamicEntry constantDynamicEntry(BootstrapMethodEntry bootstrapMethodEntry,
            NameAndTypeEntry nameAndType) {
        int bsm = adoptar(bootstrapMethodEntry).bsmIndex();
        NameAndTypeEntry nt = adoptar(nameAndType);
        String clave = "D:" + bsm + ":" + nt.index();
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (ConstantDynamicEntry) ya;
        }
        ConstantDynamicEntryImpl e = new ConstantDynamicEntryImpl(this, proximoIndice(), bsm, nt);
        return (ConstantDynamicEntry) agregar(clave, e, 1);
    }

    public IntegerEntry intEntry(int value) {
        String clave = "i:" + value;
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (IntegerEntry) ya;
        }
        IntegerEntryImpl e = new IntegerEntryImpl(this, proximoIndice(), value);
        return (IntegerEntry) agregar(clave, e, 1);
    }

    // La clave va por bits y no por valor: `0.0f` y `-0.0f` son entradas distintas del pool, y dos
    // `NaN` con la misma representación son la misma. `==` sobre float diría lo contrario en ambos.
    public FloatEntry floatEntry(float value) {
        String clave = "f:" + Float.floatToRawIntBits(value);
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (FloatEntry) ya;
        }
        FloatEntryImpl e = new FloatEntryImpl(this, proximoIndice(), value);
        return (FloatEntry) agregar(clave, e, 1);
    }

    public LongEntry longEntry(long value) {
        String clave = "l:" + value;
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (LongEntry) ya;
        }
        LongEntryImpl e = new LongEntryImpl(this, proximoIndice(), value);
        return (LongEntry) agregar(clave, e, 2);
    }

    public DoubleEntry doubleEntry(double value) {
        String clave = "d:" + Double.doubleToRawLongBits(value);
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (DoubleEntry) ya;
        }
        DoubleEntryImpl e = new DoubleEntryImpl(this, proximoIndice(), value);
        return (DoubleEntry) agregar(clave, e, 2);
    }

    public StringEntry stringEntry(Utf8Entry utf8) {
        Utf8Entry u = adoptar(utf8);
        String clave = "s:" + u.index();
        PoolEntry ya = this.porClave.get(clave);
        if (ya != null) {
            return (StringEntry) ya;
        }
        StringEntryImpl e = new StringEntryImpl(this, proximoIndice(), u);
        return (StringEntry) agregar(clave, e, 1);
    }

    public BootstrapMethodEntry bsmEntry(MethodHandleEntry methodReference,
            List<LoadableConstantEntry> arguments) {
        MethodHandleEntry h = adoptar(methodReference);
        List<LoadableConstantEntry> args = new ArrayList<LoadableConstantEntry>();
        StringBuilder clave = new StringBuilder("B:").append(h.index());
        for (int i = 0; i < arguments.size(); i++) {
            LoadableConstantEntry a = adoptarCargable(arguments.get(i));
            args.add(a);
            clave.append(':').append(a.index());
        }
        String k = clave.toString();
        BootstrapMethodEntry ya = this.bsmPorClave.get(k);
        if (ya != null) {
            return ya;
        }
        BootstrapMethodEntryImpl e =
                new BootstrapMethodEntryImpl(this, this.bsms.size(), h, args);
        this.bsms.add(e);
        this.bsmPorClave.put(k, e);
        return e;
    }

    // --- Interno ---

    private int proximoIndice() {
        int i = this.entradas.size();
        if (i > INDICE_MAXIMO) {
            throw new ConstantPoolException(
                    "el pool pasó de " + INDICE_MAXIMO + " entradas y ya no cabe en un u2");
        }
        return i;
    }

    private PoolEntry agregar(String clave, PoolEntry e, int ancho) {
        this.entradas.add(e);
        if (ancho == 2) {
            // La segunda ranura de un long/double es inutilizable (JVMS §4.4.5).
            this.entradas.add(null);
        }
        this.porClave.put(clave, e);
        return e;
    }

    // §4.4.8: los kinds 1..4 nombran un campo; 5..9, un método, y sólo el 9 puede ser de interfaz.
    private static void exigirCoherenciaDeHandle(int refKind, MemberRefEntry ref) {
        boolean esCampo = ref.tag() == TAG_FIELDREF;
        boolean esInterfaz = ref.tag() == TAG_INTERFACE_METHODREF;
        if (refKind <= 4) {
            if (!esCampo) {
                throw new IllegalArgumentException(
                        "el reference_kind " + refKind + " exige un CONSTANT_Fieldref");
            }
            return;
        }
        if (esCampo) {
            throw new IllegalArgumentException(
                    "el reference_kind " + refKind + " no admite un CONSTANT_Fieldref");
        }
        if (refKind == 9 && !esInterfaz) {
            throw new IllegalArgumentException(
                    "el reference_kind 9 exige un CONSTANT_InterfaceMethodref");
        }
        if (refKind != 9 && esInterfaz && refKind != 6 && refKind != 7) {
            throw new IllegalArgumentException("el reference_kind " + refKind
                    + " no admite un CONSTANT_InterfaceMethodref");
        }
    }

    // --- Adopción: una entrada de otro pool se reconstruye acá ---

    private Utf8Entry adoptar(Utf8Entry e) {
        return e.constantPool() == this ? e : utf8Entry(e.stringValue());
    }

    private ClassEntry adoptar(ClassEntry e) {
        return e.constantPool() == this ? e : classEntry(utf8Entry(e.asInternalName()));
    }

    private NameAndTypeEntry adoptar(NameAndTypeEntry e) {
        return e.constantPool() == this
                ? e
                : nameAndTypeEntry(utf8Entry(e.name().stringValue()),
                        utf8Entry(e.type().stringValue()));
    }

    private MemberRefEntry adoptar(MemberRefEntry e) {
        if (e.constantPool() == this) {
            return e;
        }
        ClassEntry o = adoptar(e.owner());
        NameAndTypeEntry nt = adoptar(e.nameAndType());
        int tag = e.tag();
        if (tag == TAG_FIELDREF) {
            return fieldRefEntry(o, nt);
        }
        if (tag == TAG_INTERFACE_METHODREF) {
            return interfaceMethodRefEntry(o, nt);
        }
        return methodRefEntry(o, nt);
    }

    private MethodHandleEntry adoptar(MethodHandleEntry e) {
        return e.constantPool() == this ? e : methodHandleEntry(e.kind(), e.reference());
    }

    private BootstrapMethodEntry adoptar(BootstrapMethodEntry e) {
        return e.constantPool() == this ? e : bsmEntry(e.bootstrapMethod(), e.arguments());
    }

    private LoadableConstantEntry adoptarCargable(LoadableConstantEntry e) {
        if (e.constantPool() == this) {
            return e;
        }
        int tag = e.tag();
        switch (tag) {
            case TAG_UTF8:
                // Un `Utf8` no es cargable con `ldc`; si llega acá el argumento estaba mal.
                throw new IllegalArgumentException("un CONSTANT_Utf8 no es un argumento cargable");
            case TAG_INTEGER:
                return intEntry(((IntegerEntry) e).intValue());
            case TAG_FLOAT:
                return floatEntry(((FloatEntry) e).floatValue());
            case TAG_LONG:
                return longEntry(((LongEntry) e).longValue());
            case TAG_DOUBLE:
                return doubleEntry(((DoubleEntry) e).doubleValue());
            case TAG_CLASS:
                return classEntry(utf8Entry(((ClassEntry) e).asInternalName()));
            case TAG_STRING:
                return stringEntry(utf8Entry(((StringEntry) e).stringValue()));
            case TAG_METHOD_TYPE:
                return methodTypeEntry(
                        utf8Entry(((MethodTypeEntry) e).descriptor().stringValue()));
            case TAG_METHOD_HANDLE:
                return adoptar((MethodHandleEntry) e);
            case TAG_DYNAMIC: {
                ConstantDynamicEntry d = (ConstantDynamicEntry) e;
                BootstrapMethodEntry b = adoptar(d.bootstrap());
                return constantDynamicEntry(b, adoptar(d.nameAndType()));
            }
            default:
                throw new IllegalArgumentException(
                        "la etiqueta " + tag + " no es una constante cargable");
        }
    }

    // --- Importación de un pool completo, conservando los índices ---

    private void importar(ConstantPool fuente) {
        int n = fuente.size();
        while (this.entradas.size() < n) {
            this.entradas.add(null);
        }
        for (int i = 1; i < n; i++) {
            copiar(fuente, i);
        }
        for (int i = 0; i < fuente.bootstrapMethodCount(); i++) {
            copiarBsm(fuente.bootstrapMethodEntry(i));
        }
    }

    // Recursiva a propósito: una `CONSTANT_Class` del índice 3 puede apuntar al `Utf8` del 40, que
    // todavía no se copió. Al volver, la copia del 40 ya está en su lugar y con su índice original.
    private PoolEntry copiar(ConstantPool fuente, int i) {
        PoolEntry ya = this.entradas.get(i);
        if (ya != null) {
            return ya;
        }
        PoolEntry o;
        try {
            o = fuente.entryByIndex(i);
        } catch (ConstantPoolException noEsEntrada) {
            // La ranura muerta que sigue a un long/double: se deja null, igual que en el archivo.
            return null;
        }
        PoolEntry nueva = construirCopia(fuente, o, i);
        this.entradas.set(i, nueva);
        this.porClave.put(claveDe(nueva), nueva);
        return nueva;
    }

    private PoolEntry construirCopia(ConstantPool fuente, PoolEntry o, int i) {
        switch (o.tag()) {
            case TAG_UTF8:
                return new Utf8EntryImpl(this, i, ((Utf8Entry) o).stringValue());
            case TAG_INTEGER:
                return new IntegerEntryImpl(this, i, ((IntegerEntry) o).intValue());
            case TAG_FLOAT:
                return new FloatEntryImpl(this, i, ((FloatEntry) o).floatValue());
            case TAG_LONG:
                return new LongEntryImpl(this, i, ((LongEntry) o).longValue());
            case TAG_DOUBLE:
                return new DoubleEntryImpl(this, i, ((DoubleEntry) o).doubleValue());
            case TAG_CLASS:
                return new ClassEntryImpl(this, i, utf8Copiado(fuente, ((ClassEntry) o).name()));
            case TAG_STRING:
                return new StringEntryImpl(this, i, utf8Copiado(fuente, ((StringEntry) o).utf8()));
            case TAG_METHOD_TYPE:
                return new MethodTypeEntryImpl(this, i,
                        utf8Copiado(fuente, ((MethodTypeEntry) o).descriptor()));
            case TAG_MODULE:
                return new ModuleEntryImpl(this, i, utf8Copiado(fuente, ((ModuleEntry) o).name()));
            case TAG_PACKAGE:
                return new PackageEntryImpl(this, i,
                        utf8Copiado(fuente, ((PackageEntry) o).name()));
            case TAG_NAME_AND_TYPE: {
                NameAndTypeEntry nt = (NameAndTypeEntry) o;
                return new NameAndTypeEntryImpl(this, i, utf8Copiado(fuente, nt.name()),
                        utf8Copiado(fuente, nt.type()));
            }
            case TAG_FIELDREF: {
                MemberRefEntry m = (MemberRefEntry) o;
                return new FieldRefEntryImpl(this, i, claseCopiada(fuente, m.owner()),
                        natCopiado(fuente, m.nameAndType()));
            }
            case TAG_METHODREF: {
                MemberRefEntry m = (MemberRefEntry) o;
                return new MethodRefEntryImpl(this, i, claseCopiada(fuente, m.owner()),
                        natCopiado(fuente, m.nameAndType()));
            }
            case TAG_INTERFACE_METHODREF: {
                MemberRefEntry m = (MemberRefEntry) o;
                return new InterfaceMethodRefEntryImpl(this, i, claseCopiada(fuente, m.owner()),
                        natCopiado(fuente, m.nameAndType()));
            }
            case TAG_METHOD_HANDLE: {
                MethodHandleEntry h = (MethodHandleEntry) o;
                PoolEntry r = copiar(fuente, h.reference().index());
                return new MethodHandleEntryImpl(this, i, h.kind(), (MemberRefEntry) r);
            }
            case TAG_DYNAMIC: {
                ConstantDynamicEntry d = (ConstantDynamicEntry) o;
                return new ConstantDynamicEntryImpl(this, i, d.bootstrapMethodIndex(),
                        natCopiado(fuente, d.nameAndType()));
            }
            case TAG_INVOKE_DYNAMIC: {
                InvokeDynamicEntry d = (InvokeDynamicEntry) o;
                return new InvokeDynamicEntryImpl(this, i, d.bootstrapMethodIndex(),
                        natCopiado(fuente, d.nameAndType()));
            }
            default:
                throw new ConstantPoolException(
                        "etiqueta " + o.tag() + " desconocida en el índice " + i);
        }
    }

    private Utf8Entry utf8Copiado(ConstantPool fuente, Utf8Entry o) {
        PoolEntry e = copiar(fuente, o.index());
        return (Utf8Entry) e;
    }

    private ClassEntry claseCopiada(ConstantPool fuente, ClassEntry o) {
        PoolEntry e = copiar(fuente, o.index());
        return (ClassEntry) e;
    }

    private NameAndTypeEntry natCopiado(ConstantPool fuente, NameAndTypeEntry o) {
        PoolEntry e = copiar(fuente, o.index());
        return (NameAndTypeEntry) e;
    }

    private void copiarBsm(BootstrapMethodEntry o) {
        MethodHandleEntry h = (MethodHandleEntry) this.entradas.get(o.bootstrapMethod().index());
        List<LoadableConstantEntry> args = new ArrayList<LoadableConstantEntry>();
        List<LoadableConstantEntry> orig = o.arguments();
        StringBuilder clave = new StringBuilder("B:").append(h.index());
        for (int i = 0; i < orig.size(); i++) {
            LoadableConstantEntry a =
                    (LoadableConstantEntry) this.entradas.get(orig.get(i).index());
            args.add(a);
            clave.append(':').append(a.index());
        }
        BootstrapMethodEntryImpl e =
                new BootstrapMethodEntryImpl(this, this.bsms.size(), h, args);
        this.bsms.add(e);
        this.bsmPorClave.put(clave.toString(), e);
    }

    // La misma clave que arman los `xxxEntry`, para que una entrada importada se reutilice en vez de
    // duplicarse cuando alguien la vuelve a pedir por valor.
    private static String claveDe(PoolEntry e) {
        switch (e.tag()) {
            case TAG_UTF8:
                return "u:" + ((Utf8Entry) e).stringValue();
            case TAG_INTEGER:
                return "i:" + ((IntegerEntry) e).intValue();
            case TAG_FLOAT:
                return "f:" + Float.floatToRawIntBits(((FloatEntry) e).floatValue());
            case TAG_LONG:
                return "l:" + ((LongEntry) e).longValue();
            case TAG_DOUBLE:
                return "d:" + Double.doubleToRawLongBits(((DoubleEntry) e).doubleValue());
            case TAG_CLASS:
                return "c:" + ((ClassEntry) e).name().index();
            case TAG_STRING:
                return "s:" + ((StringEntry) e).utf8().index();
            case TAG_METHOD_TYPE:
                return "t:" + ((MethodTypeEntry) e).descriptor().index();
            case TAG_MODULE:
                return "m:" + ((ModuleEntry) e).name().index();
            case TAG_PACKAGE:
                return "p:" + ((PackageEntry) e).name().index();
            case TAG_NAME_AND_TYPE: {
                NameAndTypeEntry nt = (NameAndTypeEntry) e;
                return "n:" + nt.name().index() + ":" + nt.type().index();
            }
            case TAG_FIELDREF: {
                MemberRefEntry m = (MemberRefEntry) e;
                return "F:" + m.owner().index() + ":" + m.nameAndType().index();
            }
            case TAG_METHODREF: {
                MemberRefEntry m = (MemberRefEntry) e;
                return "M:" + m.owner().index() + ":" + m.nameAndType().index();
            }
            case TAG_INTERFACE_METHODREF: {
                MemberRefEntry m = (MemberRefEntry) e;
                return "I:" + m.owner().index() + ":" + m.nameAndType().index();
            }
            case TAG_METHOD_HANDLE: {
                MethodHandleEntry h = (MethodHandleEntry) e;
                return "h:" + h.kind() + ":" + h.reference().index();
            }
            case TAG_DYNAMIC: {
                ConstantDynamicEntry d = (ConstantDynamicEntry) e;
                return "D:" + d.bootstrapMethodIndex() + ":" + d.nameAndType().index();
            }
            default: {
                InvokeDynamicEntry d = (InvokeDynamicEntry) e;
                return "y:" + d.bootstrapMethodIndex() + ":" + d.nameAndType().index();
            }
        }
    }

    public String toString() {
        return "ConstantPoolBuilder[" + (this.entradas.size() - 1) + " ranuras, "
                + this.bsms.size() + " bsm]";
    }
}
