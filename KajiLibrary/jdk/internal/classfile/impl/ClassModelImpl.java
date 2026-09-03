package jdk.internal.classfile.impl;

import java.lang.classfile.AccessFlags;
import java.lang.classfile.Attribute;
import java.lang.classfile.AttributeMapper;
import java.lang.classfile.AttributedElement;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassFileVersion;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.FieldElement;
import java.lang.classfile.FieldModel;
import java.lang.classfile.Instruction;
import java.lang.classfile.Interfaces;
import java.lang.classfile.Label;
import java.lang.classfile.MethodElement;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.Superclass;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.ConstantPoolException;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.classfile.instruction.ExceptionCatch;
import java.lang.reflect.AccessFlag.Location;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

// El modelo de una clase leída. El archivo se recorre entero en el constructor: encabezado,
// interfaces, campos, métodos y atributos. Nada queda "para después" salvo el cuerpo de los métodos,
// que se decodifica la primera vez que se pide.
public final class ClassModelImpl implements ClassModel {

    private final ClassReaderImpl lector;
    private final int mayor;
    private final int menor;
    private final AccessFlags banderas;
    private final ClassEntry estaClase;
    private final ClassEntry superClase;
    private final List<ClassEntry> interfaces;
    private final List<FieldModel> campos;
    private final List<MethodModel> metodos;
    private final List<Attribute<?>> atributos;

    public ClassModelImpl(ClassReaderImpl lector) {
        this.lector = lector;
        this.menor = lector.readU2(4);
        this.mayor = lector.readU2(6);
        this.banderas = new AccessFlagsImpl(lector.flags(), Location.CLASS);
        this.estaClase = lector.thisClassEntry();
        this.superClase = lector.superclassEntry().orElse(null);

        int p = lector.offsetCabecera + 6;
        int nInterfaces = lector.readU2(p);
        p += 2;
        List<ClassEntry> ifs = new ArrayList<ClassEntry>();
        for (int i = 0; i < nInterfaces; i++) {
            ClassEntry ce = lector.readEntry(p, ClassEntry.class);
            ifs.add(ce);
            p += 2;
        }
        this.interfaces = Collections.unmodifiableList(ifs);

        int nCampos = lector.readU2(p);
        p += 2;
        List<FieldModel> cs = new ArrayList<FieldModel>();
        for (int i = 0; i < nCampos; i++) {
            FieldModelImpl f = new FieldModelImpl(this, lector, p);
            cs.add(f);
            p = f.fin();
        }
        this.campos = Collections.unmodifiableList(cs);

        int nMetodos = lector.readU2(p);
        p += 2;
        List<MethodModel> ms = new ArrayList<MethodModel>();
        for (int i = 0; i < nMetodos; i++) {
            MethodModelImpl m = new MethodModelImpl(this, lector, p);
            ms.add(m);
            p = m.fin();
        }
        this.metodos = Collections.unmodifiableList(ms);

        Atributos as = new Atributos(lector, p, this);
        this.atributos = Collections.unmodifiableList(as.lista);
        if (as.fin != lector.classfileLength()) {
            throw new IllegalArgumentException("sobran " + (lector.classfileLength() - as.fin)
                    + " bytes después del último atributo de la clase");
        }
        // La tabla de arranque tiene que quedar armada antes de que alguien resuelva una entrada
        // dinámica del pool, y este es el primer momento en que se sabe dónde está.
        int bsm = as.offsetDe(Attributes.NAME_BOOTSTRAP_METHODS);
        if (bsm >= 0) {
            lector.tablaDeArranque(bsm);
        }
    }

    public ConstantPool constantPool() {
        return this.lector;
    }

    public AccessFlags flags() {
        return this.banderas;
    }

    public ClassEntry thisClass() {
        return this.estaClase;
    }

    public int majorVersion() {
        return this.mayor;
    }

    public int minorVersion() {
        return this.menor;
    }

    public List<FieldModel> fields() {
        return this.campos;
    }

    public List<MethodModel> methods() {
        return this.metodos;
    }

    public Optional<ClassEntry> superclass() {
        return Optional.ofNullable(this.superClase);
    }

    public List<ClassEntry> interfaces() {
        return this.interfaces;
    }

    public boolean isModuleInfo() {
        return (this.banderas.flagsMask() & ClassFile.ACC_MODULE) != 0
                && this.estaClase.asInternalName().equals("module-info");
    }

    public List<Attribute<?>> attributes() {
        return this.atributos;
    }

    // El orden es el del archivo leído de arriba abajo: versión, banderas, superclase, interfaces,
    // campos, métodos y atributos.
    public void forEach(Consumer<? super ClassElement> consumer) {
        consumer.accept(ClassFileVersion.of(this.mayor, this.menor));
        consumer.accept(this.banderas);
        if (this.superClase != null) {
            consumer.accept(Superclass.of(this.superClase));
        }
        consumer.accept(Interfaces.of(this.interfaces));
        for (int i = 0; i < this.campos.size(); i++) {
            consumer.accept((ClassElement) this.campos.get(i));
        }
        for (int i = 0; i < this.metodos.size(); i++) {
            consumer.accept((ClassElement) this.metodos.get(i));
        }
        for (int i = 0; i < this.atributos.size(); i++) {
            consumer.accept((ClassElement) this.atributos.get(i));
        }
    }

    public String toString() {
        return "ClassModel[" + this.estaClase.asInternalName() + "]";
    }
}

// Los atributos de un lugar del archivo, leídos de corrido. Guarda además el offset del cuerpo de
// cada uno: el modelo lo necesita para volver a entrar en `Code` y en `BootstrapMethods`, que son los
// dos atributos cuya estructura este lector sí interpreta.
final class Atributos {

    final List<Attribute<?>> lista = new ArrayList<Attribute<?>>();
    final List<String> nombres = new ArrayList<String>();
    final int[] offsets;
    final int fin;

    Atributos(ClassReaderImpl lector, int p, AttributedElement duenio) {
        int n = lector.readU2(p);
        p += 2;
        this.offsets = new int[n];
        for (int i = 0; i < n; i++) {
            Utf8Entry name = lector.readEntry(p, Utf8Entry.class);
            int largo = lector.readInt(p + 2);
            if (largo < 0 || p + 6 + largo > lector.classfileLength()) {
                throw new IllegalArgumentException("el atributo " + name.stringValue()
                        + " dice medir " + largo + " bytes y no entra en el archivo");
            }
            AttributeMapper<RawAttribute> mapper = Mappers.forName(name.stringValue());
            this.lista.add(mapper.readAttribute(duenio, lector, p + 6));
            this.nombres.add(name.stringValue());
            this.offsets[i] = p + 6;
            p += 6 + largo;
        }
        this.fin = p;
    }

    int offsetDe(String name) {
        for (int i = 0; i < this.nombres.size(); i++) {
            if (this.nombres.get(i).equals(name)) {
                return this.offsets[i];
            }
        }
        return -1;
    }
}

// Un campo leído.
final class FieldModelImpl implements FieldModel {

    private final ClassModel duenio;
    private final AccessFlags banderas;
    private final Utf8Entry name;
    private final Utf8Entry descriptor;
    private final List<Attribute<?>> atributos;
    private final int fin;

    FieldModelImpl(ClassModel duenio, ClassReaderImpl lector, int p) {
        this.duenio = duenio;
        this.banderas = new AccessFlagsImpl(lector.readU2(p), Location.FIELD);
        this.name = lector.readEntry(p + 2, Utf8Entry.class);
        this.descriptor = lector.readEntry(p + 4, Utf8Entry.class);
        Atributos as = new Atributos(lector, p + 6, this);
        this.atributos = Collections.unmodifiableList(as.lista);
        this.fin = as.fin;
    }

    int fin() {
        return this.fin;
    }

    public AccessFlags flags() {
        return this.banderas;
    }

    public Optional<ClassModel> parent() {
        return Optional.of(this.duenio);
    }

    public Utf8Entry fieldName() {
        return this.name;
    }

    public Utf8Entry fieldType() {
        return this.descriptor;
    }

    public List<Attribute<?>> attributes() {
        return this.atributos;
    }

    public void forEach(Consumer<? super FieldElement> consumer) {
        consumer.accept(this.banderas);
        for (int i = 0; i < this.atributos.size(); i++) {
            consumer.accept((FieldElement) this.atributos.get(i));
        }
    }

    public String toString() {
        return "FieldModel[" + this.name.stringValue() + " " + this.descriptor.stringValue() + "]";
    }
}

// Un método leído. El cuerpo se decodifica la primera vez que se pide.
final class MethodModelImpl implements MethodModel {

    private final ClassModel duenio;
    private final ClassReaderImpl lector;
    private final AccessFlags banderas;
    private final Utf8Entry name;
    private final Utf8Entry descriptor;
    private final List<Attribute<?>> atributos;
    private final int offsetDelCodigo;
    private final int fin;
    private CodeModel cuerpo;

    MethodModelImpl(ClassModel duenio, ClassReaderImpl lector, int p) {
        this.duenio = duenio;
        this.lector = lector;
        this.banderas = new AccessFlagsImpl(lector.readU2(p), Location.METHOD);
        this.name = lector.readEntry(p + 2, Utf8Entry.class);
        this.descriptor = lector.readEntry(p + 4, Utf8Entry.class);
        Atributos as = new Atributos(lector, p + 6, this);
        this.atributos = Collections.unmodifiableList(as.lista);
        this.offsetDelCodigo = as.offsetDe(Attributes.NAME_CODE);
        this.fin = as.fin;
    }

    int fin() {
        return this.fin;
    }

    public AccessFlags flags() {
        return this.banderas;
    }

    public Optional<ClassModel> parent() {
        return Optional.of(this.duenio);
    }

    public Utf8Entry methodName() {
        return this.name;
    }

    public Utf8Entry methodType() {
        return this.descriptor;
    }

    public List<Attribute<?>> attributes() {
        return this.atributos;
    }

    public Optional<CodeModel> code() {
        if (this.offsetDelCodigo < 0) {
            return Optional.empty();
        }
        if (this.cuerpo == null) {
            this.cuerpo = new CodeModelImpl(this, this.lector, this.offsetDelCodigo);
        }
        return Optional.of(this.cuerpo);
    }

    // Banderas, cuerpo y atributos. El atributo `Code` NO se emite como atributo: se emite el
    // `CodeModel`, que es lo que el JDK hace y lo que evita que el cuerpo aparezca dos veces.
    public void forEach(Consumer<? super MethodElement> consumer) {
        consumer.accept(this.banderas);
        Optional<CodeModel> c = code();
        if (c.isPresent()) {
            consumer.accept(c.get());
        }
        for (int i = 0; i < this.atributos.size(); i++) {
            Attribute<?> a = this.atributos.get(i);
            if (!a.attributeName().equalsString(Attributes.NAME_CODE)) {
                consumer.accept((MethodElement) a);
            }
        }
    }

    public String toString() {
        return "MethodModel[" + this.name.stringValue() + this.descriptor.stringValue() + "]";
    }
}

// El cuerpo de un método: el atributo `Code` (§4.7.3) decodificado.
final class CodeModelImpl implements CodeModel {

    private final MethodModel duenio;
    private final List<ExceptionCatch> manejadores;
    private final List<Attribute<?>> atributos;
    private final List<Instruction> instrucciones;
    private final int maxStack;
    private final int maxLocals;

    CodeModelImpl(MethodModel duenio, ClassReaderImpl lector, int p) {
        this.duenio = duenio;
        this.maxStack = lector.readU2(p);
        this.maxLocals = lector.readU2(p + 2);
        int largoDelCodigo = lector.readInt(p + 4);
        if (largoDelCodigo <= 0 || p + 8 + largoDelCodigo > lector.classfileLength()) {
            throw new IllegalArgumentException("code_length inválido: " + largoDelCodigo);
        }
        int inicio = p + 8;
        this.instrucciones = decode(lector, inicio, largoDelCodigo);

        int q = inicio + largoDelCodigo;
        int nManejadores = lector.readU2(q);
        q += 2;
        List<ExceptionCatch> hs = new ArrayList<ExceptionCatch>();
        for (int i = 0; i < nManejadores; i++) {
            int desde = lector.readU2(q);
            int hasta = lector.readU2(q + 2);
            int manejador = lector.readU2(q + 4);
            ClassEntry type = lector.readEntryOrNull(q + 6, ClassEntry.class);
            if (desde > largoDelCodigo || hasta > largoDelCodigo || manejador >= largoDelCodigo) {
                throw new IllegalArgumentException(
                        "un manejador de excepción apunta fuera del arreglo code");
            }
            hs.add(new ExceptionCatchImpl(new LabelImpl(manejador), new LabelImpl(desde),
                    new LabelImpl(hasta), Optional.ofNullable(type)));
            q += 8;
        }
        this.manejadores = Collections.unmodifiableList(hs);
        Atributos as = new Atributos(lector, q, this);
        this.atributos = Collections.unmodifiableList(as.lista);
    }

    // El recorrido del arreglo `code`. Los dos casos que no son "sumá `sizeIfFixed()`" son los que
    // rompen a un lector ingenuo: `wide`, que cambia el espacio de opcodes, y los dos switches, cuyo
    // largo depende del relleno hasta el próximo múltiplo de 4 *desde el inicio del método*.
    private static List<Instruction> decode(ClassReaderImpl lector, int inicio, int largo) {
        List<Instruction> salida = new ArrayList<Instruction>();
        int bci = 0;
        while (bci < largo) {
            int b = lector.readU1(inicio + bci);
            Opcode op;
            int tam;
            if (b == OpcodeTable.WIDE) {
                if (bci + 1 >= largo) {
                    throw new IllegalArgumentException("un wide al final del arreglo code");
                }
                op = OpcodeTable.wide(lector.readU1(inicio + bci + 1));
                if (op == null) {
                    throw new IllegalArgumentException(
                            "wide seguido de un opcode que no lo admite, en el bci " + bci);
                }
                tam = op.sizeIfFixed();
            } else {
                op = OpcodeTable.simple(b);
                if (op == null) {
                    throw new IllegalArgumentException(
                            "opcode desconocido 0x" + Integer.toHexString(b) + " en el bci " + bci);
                }
                tam = op.sizeIfFixed();
                if (tam < 0) {
                    tam = largoDeSwitch(lector, inicio, bci, op, largo);
                }
            }
            if (bci + tam > largo) {
                throw new IllegalArgumentException(
                        "la instrucción del bci " + bci + " se sale del arreglo code");
            }
            salida.add(Instructions.decode(lector, inicio, bci, op, tam));
            bci += tam;
        }
        return Collections.unmodifiableList(salida);
    }

    private static int largoDeSwitch(ClassReaderImpl lector, int inicio, int bci, Opcode op,
            int largo) {
        int p = bci + 1;
        while ((p & 3) != 0) {
            p++;
        }
        if (op == Opcode.TABLESWITCH) {
            if (inicio + p + 12 > lector.classfileLength()) {
                throw new IllegalArgumentException("un tableswitch truncado en el bci " + bci);
            }
            int bajo = lector.readInt(inicio + p + 4);
            int alto = lector.readInt(inicio + p + 8);
            if (alto < bajo) {
                throw new IllegalArgumentException("tableswitch con high < low en el bci " + bci);
            }
            long n = (long) alto - (long) bajo + 1L;
            long tam = (long) p + 12L + n * 4L - bci;
            if (tam > largo) {
                throw new IllegalArgumentException("un tableswitch que no entra en el arreglo code");
            }
            return (int) tam;
        }
        if (inicio + p + 8 > lector.classfileLength()) {
            throw new IllegalArgumentException("un lookupswitch truncado en el bci " + bci);
        }
        int n = lector.readInt(inicio + p + 4);
        if (n < 0) {
            throw new IllegalArgumentException("lookupswitch con npairs negativo en el bci " + bci);
        }
        long tam = (long) p + 8L + (long) n * 8L - bci;
        if (tam > largo) {
            throw new IllegalArgumentException("un lookupswitch que no entra en el arreglo code");
        }
        return (int) tam;
    }

    public Optional<MethodModel> parent() {
        return Optional.of(this.duenio);
    }

    public List<ExceptionCatch> exceptionHandlers() {
        return this.manejadores;
    }

    public List<Attribute<?>> attributes() {
        return this.atributos;
    }

    /** El `max_stack` del atributo. No es API del JDK; está porque el lector ya lo tiene. */
    public int maxStack() {
        return this.maxStack;
    }

    /** El `max_locals` del atributo. */
    public int maxLocals() {
        return this.maxLocals;
    }

    // Manejadores, instrucciones y atributos del código.
    //
    // Lo que NO se emite: las pseudoinstrucciones de depuración —`LabelTarget`, `LineNumber`,
    // `LocalVariable`, `LocalVariableType`, `CharacterRange`— intercaladas entre las instrucciones.
    // Los datos están: siguen ahí como los atributos `LineNumberTable` y compañía, que este
    // recorrido sí emite. Es el mismo cuerpo que produce el JDK con `DebugElementsOption.DROP_DEBUG`
    // y `DeadLabelsOption`, no una pérdida de información.
    public void forEach(Consumer<? super CodeElement> consumer) {
        for (int i = 0; i < this.manejadores.size(); i++) {
            consumer.accept(this.manejadores.get(i));
        }
        for (int i = 0; i < this.instrucciones.size(); i++) {
            consumer.accept(this.instrucciones.get(i));
        }
        for (int i = 0; i < this.atributos.size(); i++) {
            consumer.accept((CodeElement) this.atributos.get(i));
        }
    }

    public String toString() {
        return "CodeModel[" + this.instrucciones.size() + " instrucciones]";
    }
}
