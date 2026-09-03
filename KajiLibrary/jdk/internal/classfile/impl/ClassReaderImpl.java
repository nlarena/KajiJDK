package jdk.internal.classfile.impl;

import java.lang.classfile.AttributeMapper;
import java.lang.classfile.BootstrapMethodEntry;
import java.lang.classfile.BufWriter;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassReader;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPoolException;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.MethodHandleEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

// El lector de bajo nivel: los bytes del archivo, el pool ya construido y validado, y el acceso por
// offset que usan los mapeadores de atributos.
//
// La validación es estricta a propósito, y ese es el punto de diseño del archivo. Al construirse:
//
//   1. exige el magic, un `constant_pool_count` &ge; 1 y que el pool entre en el archivo;
//   2. recorre las entradas comprobando que cada etiqueta sea una de las diecisiete y que el cuerpo
//      de cada una entre en lo que queda;
//   3. materializa las diecisiete formas, y al hacerlo comprueba que cada índice referenciado exista
//      y tenga LA ETIQUETA que corresponde — un `CONSTANT_Class` que apunte a un `CONSTANT_Integer`
//      no pasa;
//   4. exige que la ranura que sigue a un `long` o a un `double` no se use.
//
// Un archivo que no cumple todo eso tira `ConstantPoolException` acá, al abrirse, y no más tarde
// desde alguna llamada suelta. Aceptar un `.class` mal formado y devolver un modelo a medio armar
// sería peor que no tener lector.
public final class ClassReaderImpl implements ClassReader {

    // Copias locales de las etiquetas de PoolEntry. No son un duplicado por gusto: el generador
    // de bytecode no pliega una constante de otra unidad de compilación en una etiqueta `case`
    // (ver el informe), así que un `case TAG_UTF8:` no compila. Los valores son los
    // del JVMS §4.4 y hay una prueba que los compara contra PoolEntry.
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

    private final byte[] bytes;
    private final int poolCount;
    // Por índice de pool: la etiqueta y el offset del primer byte del `info`. Índice 0 y las ranuras
    // muertas quedan con etiqueta 0.
    private final int[] etiquetas;
    private final int[] offsets;
    private final PoolEntry[] entradas;
    private final Function<Utf8Entry, AttributeMapper<?>> aMedida;

    // El offset justo después del pool, donde arranca el `access_flags`.
    final int offsetCabecera;
    private final int accessFlags;
    private final ClassEntry estaClase;
    private final ClassEntry superClase;

    // La tabla de `BootstrapMethods`, que no está en el pool sino en un atributo de la clase. Se
    // llena cuando el `ClassModel` termina de leer los atributos, porque hasta entonces no se sabe
    // dónde está.
    private List<BootstrapMethodEntry> bsms;

    public ClassReaderImpl(byte[] bytes, Function<Utf8Entry, AttributeMapper<?>> aMedida) {
        this.bytes = bytes;
        this.aMedida = aMedida;
        if (bytes.length < 10) {
            throw new IllegalArgumentException(
                    "el archivo tiene " + bytes.length + " bytes: no llega ni al encabezado");
        }
        if (leerInt(0) != ClassFile.MAGIC_NUMBER) {
            throw new IllegalArgumentException("no empieza con 0xCAFEBABE");
        }
        this.poolCount = leerU2(8);
        if (this.poolCount < 1) {
            throw new ConstantPoolException("constant_pool_count = 0");
        }
        this.etiquetas = new int[this.poolCount];
        this.offsets = new int[this.poolCount];
        this.entradas = new PoolEntry[this.poolCount];
        this.offsetCabecera = recorrerPool();
        // Materializar todo obliga a que cada referencia interna se valide ahora y no después.
        for (int i = 1; i < this.poolCount; i++) {
            if (this.etiquetas[i] != 0) {
                materializar(i);
            }
        }
        exigir(this.offsetCabecera + 6 <= bytes.length, "el archivo se corta antes de this_class");
        this.accessFlags = leerU2(this.offsetCabecera);
        this.estaClase = entryByIndex(leerU2(this.offsetCabecera + 2), ClassEntry.class);
        int idxSuper = leerU2(this.offsetCabecera + 4);
        this.superClase = idxSuper == 0 ? null : entryByIndex(idxSuper, ClassEntry.class);
    }

    // --- Paso 1: el recorrido del pool. Devuelve el offset del `access_flags`. ---

    private int recorrerPool() {
        int p = 10;
        int i = 1;
        while (i < this.poolCount) {
            exigir(p < this.bytes.length, "el pool se sale del archivo en el índice " + i);
            int tag = this.bytes[p] & 0xFF;
            int cuerpo = largoDeCuerpo(tag, p, i);
            exigir(p + 1 + cuerpo <= this.bytes.length,
                    "la entrada " + i + " (tag " + tag + ") se sale del archivo");
            this.etiquetas[i] = tag;
            this.offsets[i] = p + 1;
            p += 1 + cuerpo;
            if (tag == PoolEntry.TAG_LONG || tag == PoolEntry.TAG_DOUBLE) {
                // La ranura siguiente es inutilizable (JVMS §4.4.5) y queda con etiqueta 0.
                exigir(i + 1 < this.poolCount,
                        "un long/double en el índice " + i + " no deja lugar para su segunda ranura");
                i += 2;
            } else {
                i += 1;
            }
        }
        return p;
    }

    private int largoDeCuerpo(int tag, int p, int i) {
        switch (tag) {
            case TAG_UTF8:
                exigir(p + 3 <= this.bytes.length, "un CONSTANT_Utf8 truncado en el índice " + i);
                return 2 + leerU2(p + 1);
            case TAG_INTEGER:
            case TAG_FLOAT:
                return 4;
            case TAG_LONG:
            case TAG_DOUBLE:
                return 8;
            case TAG_CLASS:
            case TAG_STRING:
            case TAG_METHOD_TYPE:
            case TAG_MODULE:
            case TAG_PACKAGE:
                return 2;
            case TAG_FIELDREF:
            case TAG_METHODREF:
            case TAG_INTERFACE_METHODREF:
            case TAG_NAME_AND_TYPE:
            case TAG_DYNAMIC:
            case TAG_INVOKE_DYNAMIC:
                return 4;
            case TAG_METHOD_HANDLE:
                return 3;
            default:
                throw new ConstantPoolException(
                        "etiqueta desconocida " + tag + " en el índice " + i);
        }
    }

    // --- Paso 2: materializar una entrada, validando lo que referencia. ---

    private PoolEntry materializar(int i) {
        PoolEntry ya = this.entradas[i];
        if (ya != null) {
            return ya;
        }
        int tag = this.etiquetas[i];
        int p = this.offsets[i];
        PoolEntry e;
        switch (tag) {
            case TAG_UTF8:
                e = new Utf8EntryImpl(this, i, decodificarUtf8(p + 2, leerU2(p)));
                break;
            case TAG_INTEGER:
                e = new IntegerEntryImpl(this, i, leerInt(p));
                break;
            case TAG_FLOAT:
                e = new FloatEntryImpl(this, i, Float.intBitsToFloat(leerInt(p)));
                break;
            case TAG_LONG:
                e = new LongEntryImpl(this, i, leerLong(p));
                break;
            case TAG_DOUBLE:
                e = new DoubleEntryImpl(this, i, Double.longBitsToDouble(leerLong(p)));
                break;
            case TAG_CLASS:
                e = new ClassEntryImpl(this, i, utf8En(leerU2(p), i));
                break;
            case TAG_STRING:
                e = new StringEntryImpl(this, i, utf8En(leerU2(p), i));
                break;
            case TAG_METHOD_TYPE:
                e = new MethodTypeEntryImpl(this, i, utf8En(leerU2(p), i));
                break;
            case TAG_MODULE:
                e = new ModuleEntryImpl(this, i, utf8En(leerU2(p), i));
                break;
            case TAG_PACKAGE:
                e = new PackageEntryImpl(this, i, utf8En(leerU2(p), i));
                break;
            case TAG_NAME_AND_TYPE:
                e = new NameAndTypeEntryImpl(this, i,
                        utf8En(leerU2(p), i),
                        utf8En(leerU2(p + 2), i));
                break;
            case TAG_FIELDREF:
                e = new FieldRefEntryImpl(this, i,
                        claseEn(leerU2(p), i),
                        natEn(leerU2(p + 2), i));
                break;
            case TAG_METHODREF:
                e = new MethodRefEntryImpl(this, i,
                        claseEn(leerU2(p), i),
                        natEn(leerU2(p + 2), i));
                break;
            case TAG_INTERFACE_METHODREF:
                e = new InterfaceMethodRefEntryImpl(this, i,
                        claseEn(leerU2(p), i),
                        natEn(leerU2(p + 2), i));
                break;
            case TAG_METHOD_HANDLE: {
                int refKind = this.bytes[p] & 0xFF;
                if (refKind < 1 || refKind > 9) {
                    throw new ConstantPoolException(
                            "reference_kind " + refKind + " fuera de 1..9 en el índice " + i);
                }
                MemberRefEntry ref = miembroEn(leerU2(p + 1), i);
                exigirCoherenciaDeHandle(refKind, ref, i);
                e = new MethodHandleEntryImpl(this, i, refKind, ref);
                break;
            }
            case TAG_DYNAMIC:
                e = new ConstantDynamicEntryImpl(this, i, leerU2(p),
                        natEn(leerU2(p + 2), i));
                break;
            case TAG_INVOKE_DYNAMIC:
                e = new InvokeDynamicEntryImpl(this, i, leerU2(p),
                        natEn(leerU2(p + 2), i));
                break;
            default:
                throw new ConstantPoolException("etiqueta " + tag + " en el índice " + i);
        }
        this.entradas[i] = e;
        return e;
    }

    // Lo que §4.4.8 exige de la combinación kind/referencia. Es la única regla del pool que no se
    // deduce de las etiquetas, y dejarla afuera dejaría pasar handles imposibles.
    private void exigirCoherenciaDeHandle(int refKind, MemberRefEntry ref, int i) {
        boolean esCampo = ref.tag() == PoolEntry.TAG_FIELDREF;
        if (refKind <= 4) {
            if (!esCampo) {
                throw new ConstantPoolException("el reference_kind " + refKind
                        + " del índice " + i + " exige un CONSTANT_Fieldref");
            }
        } else {
            if (esCampo) {
                throw new ConstantPoolException("el reference_kind " + refKind
                        + " del índice " + i + " no admite un CONSTANT_Fieldref");
            }
            boolean esInit = ref.name().equalsString("<init>");
            if (refKind == 8 && !esInit) {
                throw new ConstantPoolException(
                        "un REF_newInvokeSpecial (índice " + i + ") tiene que apuntar a <init>");
            }
            if (refKind != 8 && esInit) {
                throw new ConstantPoolException("el reference_kind " + refKind
                        + " del índice " + i + " no puede apuntar a <init>");
            }
        }
    }

    // El UTF-8 *modificado* de §4.4.7: el `NUL` viaja en dos bytes y los caracteres suplementarios en
    // seis (dos sustitutos de tres bytes cada uno). No es UTF-8 y no se puede delegar en un decoder
    // estándar, que rechazaría lo primero y colapsaría lo segundo.
    private String decodificarUtf8(int desde, int largo) {
        exigir(desde + largo <= this.bytes.length, "un CONSTANT_Utf8 se sale del archivo");
        StringBuilder sb = new StringBuilder(largo);
        int p = desde;
        int fin = desde + largo;
        while (p < fin) {
            int b1 = this.bytes[p] & 0xFF;
            if (b1 < 0x80) {
                if (b1 == 0) {
                    throw new ConstantPoolException("un 0x00 crudo dentro de un CONSTANT_Utf8");
                }
                sb.append((char) b1);
                p += 1;
            } else if ((b1 & 0xE0) == 0xC0) {
                exigir(p + 1 < fin, "un CONSTANT_Utf8 se corta en medio de una secuencia");
                int b2 = this.bytes[p + 1] & 0xFF;
                exigir((b2 & 0xC0) == 0x80, "byte de continuación inválido en un CONSTANT_Utf8");
                sb.append((char) (((b1 & 0x1F) << 6) | (b2 & 0x3F)));
                p += 2;
            } else if ((b1 & 0xF0) == 0xE0) {
                exigir(p + 2 < fin, "un CONSTANT_Utf8 se corta en medio de una secuencia");
                int b2 = this.bytes[p + 1] & 0xFF;
                int b3 = this.bytes[p + 2] & 0xFF;
                exigir((b2 & 0xC0) == 0x80 && (b3 & 0xC0) == 0x80,
                        "byte de continuación inválido en un CONSTANT_Utf8");
                sb.append((char) (((b1 & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F)));
                p += 3;
            } else {
                throw new ConstantPoolException(
                        "byte 0x" + Integer.toHexString(b1) + " inválido en un CONSTANT_Utf8");
            }
        }
        return sb.toString();
    }

    private void exigir(boolean cond, String mensaje) {
        if (!cond) {
            throw new ConstantPoolException(mensaje);
        }
    }

    // Envolturas sin genéricos de `exigirTipo`. Existen porque el compilador borra el parámetro de
    // tipo a su cota cuando la llamada genérica va directo como argumento de otra llamada, y
    // entonces no encuentra el constructor (ver el informe). Con estas la inferencia no hace falta.
    private Utf8Entry utf8En(int indice, int desde) {
        return exigirTipo(indice, Utf8Entry.class, desde);
    }

    private ClassEntry claseEn(int indice, int desde) {
        return exigirTipo(indice, ClassEntry.class, desde);
    }

    private NameAndTypeEntry natEn(int indice, int desde) {
        return exigirTipo(indice, NameAndTypeEntry.class, desde);
    }

    private MemberRefEntry miembroEn(int indice, int desde) {
        return exigirTipo(indice, MemberRefEntry.class, desde);
    }

    private <T extends PoolEntry> T exigirTipo(int indice, Class<T> cls, int desde) {
        if (indice < 1 || indice >= this.poolCount || this.etiquetas[indice] == 0) {
            throw new ConstantPoolException("el índice " + desde + " referencia el índice "
                    + indice + ", que no es una entrada del pool");
        }
        PoolEntry e = materializar(indice);
        if (!cls.isInstance(e)) {
            throw new ConstantPoolException("el índice " + desde + " referencia el índice " + indice
                    + ", que es " + e.getClass().getSimpleName() + " y no " + cls.getSimpleName());
        }
        return (T) e;
    }

    // --- ConstantPool ---

    public PoolEntry entryByIndex(int index) {
        if (index < 1 || index >= this.poolCount || this.etiquetas[index] == 0) {
            throw new ConstantPoolException("índice de pool inválido: " + index);
        }
        return materializar(index);
    }

    public int size() {
        return this.poolCount;
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
        List<BootstrapMethodEntry> tabla = this.bsms;
        if (tabla == null) {
            throw new ConstantPoolException(
                    "la clase no tiene atributo BootstrapMethods, y el índice " + index
                    + " lo necesita");
        }
        if (index < 0 || index >= tabla.size()) {
            throw new ConstantPoolException("índice de BootstrapMethods fuera de rango: " + index);
        }
        return tabla.get(index);
    }

    public int bootstrapMethodCount() {
        return this.bsms == null ? 0 : this.bsms.size();
    }

    // Lo llama `ClassModelImpl` cuando encuentra el atributo, que es el único momento en que se
    // puede saber dónde está la tabla.
    void tablaDeArranque(int offsetCuerpo) {
        int n = leerU2(offsetCuerpo);
        List<BootstrapMethodEntry> tabla = new ArrayList<BootstrapMethodEntry>();
        int p = offsetCuerpo + 2;
        for (int i = 0; i < n; i++) {
            exigir(p + 4 <= this.bytes.length, "BootstrapMethods truncado");
            MethodHandleEntry handle = entryByIndex(leerU2(p), MethodHandleEntry.class);
            int nargs = leerU2(p + 2);
            p += 4;
            List<LoadableConstantEntry> args = new ArrayList<LoadableConstantEntry>();
            for (int j = 0; j < nargs; j++) {
                exigir(p + 2 <= this.bytes.length, "BootstrapMethods truncado");
                LoadableConstantEntry arg =
                        entryByIndex(leerU2(p), LoadableConstantEntry.class);
                args.add(arg);
                p += 2;
            }
            tabla.add(new BootstrapMethodEntryImpl(this, i, handle, args));
        }
        this.bsms = tabla;
    }

    // --- ClassReader ---

    public Function<Utf8Entry, AttributeMapper<?>> customAttributes() {
        return this.aMedida;
    }

    public int flags() {
        return this.accessFlags;
    }

    public ClassEntry thisClassEntry() {
        return this.estaClase;
    }

    public Optional<ClassEntry> superclassEntry() {
        return Optional.ofNullable(this.superClase);
    }

    public int classfileLength() {
        return this.bytes.length;
    }

    public PoolEntry readEntry(int offset) {
        return entryByIndex(readU2(offset));
    }

    public <T extends PoolEntry> T readEntry(int offset, Class<T> cls) {
        return entryByIndex(readU2(offset), cls);
    }

    public PoolEntry readEntryOrNull(int offset) {
        int i = readU2(offset);
        return i == 0 ? null : entryByIndex(i);
    }

    public <T extends PoolEntry> T readEntryOrNull(int offset, Class<T> cls) {
        int i = readU2(offset);
        return i == 0 ? null : entryByIndex(i, cls);
    }

    public int readU1(int offset) {
        rango(offset, 1);
        return this.bytes[offset] & 0xFF;
    }

    public int readU2(int offset) {
        rango(offset, 2);
        return leerU2(offset);
    }

    public int readS1(int offset) {
        rango(offset, 1);
        return this.bytes[offset];
    }

    public int readS2(int offset) {
        rango(offset, 2);
        return (short) leerU2(offset);
    }

    public int readInt(int offset) {
        rango(offset, 4);
        return leerInt(offset);
    }

    public long readLong(int offset) {
        rango(offset, 8);
        return leerLong(offset);
    }

    public float readFloat(int offset) {
        return Float.intBitsToFloat(readInt(offset));
    }

    public double readDouble(int offset) {
        return Double.longBitsToDouble(readLong(offset));
    }

    public byte[] readBytes(int offset, int len) {
        rango(offset, len);
        byte[] r = new byte[len];
        System.arraycopy(this.bytes, offset, r, 0, len);
        return r;
    }

    public void copyBytesTo(BufWriter buf, int offset, int len) {
        rango(offset, len);
        buf.writeBytes(this.bytes, offset, len);
    }

    private void rango(int offset, int len) {
        if (offset < 0 || len < 0 || offset + len > this.bytes.length) {
            throw new ConstantPoolException(
                    "lectura fuera del archivo: offset " + offset + ", " + len + " bytes, archivo de "
                    + this.bytes.length);
        }
    }

    // Lecturas sin chequeo, para uso interno donde el rango ya se validó.
    int leerU2(int p) {
        return ((this.bytes[p] & 0xFF) << 8) | (this.bytes[p + 1] & 0xFF);
    }

    int leerInt(int p) {
        return ((this.bytes[p] & 0xFF) << 24) | ((this.bytes[p + 1] & 0xFF) << 16)
                | ((this.bytes[p + 2] & 0xFF) << 8) | (this.bytes[p + 3] & 0xFF);
    }

    long leerLong(int p) {
        return ((long) leerInt(p) << 32) | (leerInt(p + 4) & 0xFFFFFFFFL);
    }

    int largoDelArchivo() {
        return this.bytes.length;
    }
}
