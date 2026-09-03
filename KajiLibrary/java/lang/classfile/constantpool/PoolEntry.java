package java.lang.classfile.constantpool;

// La raíz de las entradas del pool de constantes (JVMS §4.4). Una entrada es una tupla etiquetada
// que vive en un índice del pool de una clase concreta: `tag()` dice de qué estructura se trata,
// `index()` en qué posición está, y `constantPool()` a qué pool pertenece — dos entradas iguales de
// pools distintos NO son la misma entrada, y por eso la identidad del pool es parte del contrato.
//
// `width()` es 2 para `CONSTANT_Long` y `CONSTANT_Double` y 1 para todas las demás: es la rareza
// histórica de §4.4.5, donde esas dos ocupan dos ranuras del pool y la siguiente se declara
// inutilizable. Quien recorra el pool por índice tiene que sumar `width()`, no 1.
//
// En KajiJDK esta jerarquía es exactamente la del JDK, con una sola diferencia deliberada: las
// interfaces NO se declaran `sealed`. El JDK las sella hacia `jdk.internal.classfile.impl`; sellarlas
// acá obligaría a nombrar las implementaciones internas desde el paquete público, y un sellado que se
// relaja es un permiso de más, no un contrato que se incumple.
public interface PoolEntry {

    /** Etiqueta de `CONSTANT_Utf8_info` (JVMS §4.4.7). */
    public static final int TAG_UTF8 = 1;
    /** Etiqueta de `CONSTANT_Integer_info` (§4.4.4). */
    public static final int TAG_INTEGER = 3;
    /** Etiqueta de `CONSTANT_Float_info` (§4.4.4). */
    public static final int TAG_FLOAT = 4;
    /** Etiqueta de `CONSTANT_Long_info` (§4.4.5). */
    public static final int TAG_LONG = 5;
    /** Etiqueta de `CONSTANT_Double_info` (§4.4.5). */
    public static final int TAG_DOUBLE = 6;
    /** Etiqueta de `CONSTANT_Class_info` (§4.4.1). */
    public static final int TAG_CLASS = 7;
    /** Etiqueta de `CONSTANT_String_info` (§4.4.3). */
    public static final int TAG_STRING = 8;
    /** Etiqueta de `CONSTANT_Fieldref_info` (§4.4.2). */
    public static final int TAG_FIELDREF = 9;
    /** Etiqueta de `CONSTANT_Methodref_info` (§4.4.2). */
    public static final int TAG_METHODREF = 10;
    /** Etiqueta de `CONSTANT_InterfaceMethodref_info` (§4.4.2). */
    public static final int TAG_INTERFACE_METHODREF = 11;
    /** Etiqueta de `CONSTANT_NameAndType_info` (§4.4.6). */
    public static final int TAG_NAME_AND_TYPE = 12;
    /** Etiqueta de `CONSTANT_MethodHandle_info` (§4.4.8). */
    public static final int TAG_METHOD_HANDLE = 15;
    /** Etiqueta de `CONSTANT_MethodType_info` (§4.4.9). */
    public static final int TAG_METHOD_TYPE = 16;
    /** Etiqueta de `CONSTANT_Dynamic_info` (§4.4.10). */
    public static final int TAG_DYNAMIC = 17;
    /** Etiqueta de `CONSTANT_InvokeDynamic_info` (§4.4.10). */
    public static final int TAG_INVOKE_DYNAMIC = 18;
    /** Etiqueta de `CONSTANT_Module_info` (§4.4.11). */
    public static final int TAG_MODULE = 19;
    /** Etiqueta de `CONSTANT_Package_info` (§4.4.12). */
    public static final int TAG_PACKAGE = 20;

    /** El pool al que pertenece esta entrada. */
    ConstantPool constantPool();

    /** La etiqueta `tag` de la estructura, uno de los `TAG_*`. */
    int tag();

    /** El índice de esta entrada dentro de su pool; siempre &ge; 1. */
    int index();

    /** Cuántas ranuras del pool ocupa: 2 para `long` y `double`, 1 para el resto. */
    int width();
}
