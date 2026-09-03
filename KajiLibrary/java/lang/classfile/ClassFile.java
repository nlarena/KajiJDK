package java.lang.classfile;

import java.io.IOException;
import java.lang.constant.ClassDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

// La fábrica de la API: de acá salen los `ClassModel`, acá se escriben los `.class` nuevos, y acá
// viven las constantes del formato.
//
// ALCANCE, y conviene leerlo antes de usar `build`: lo que se escribe es **exactamente lo que se le
// dijo**. El `Code` sale con su `max_stack` calculado --recorriendo el grafo de flujo, no sumando--,
// su `max_locals`, su tabla de excepciones y sus atributos de depuración; lo que NO sale es un
// `StackMapTable` sintetizado. Si el llamador agrega uno, se escribe; si no, el método queda sin él.
//
// La consecuencia es concreta: una clase de versión 50 o mayor, con saltos y sin `StackMapTable`,
// **no pasa el verificador de una JVM**. El JDK lo calcula solo. Calcularlo no es un detalle que
// falte por descuido -- es una inferencia de tipos sobre todo el grafo, que necesita el supertipo
// común de cada unión, que es justamente para lo que existe `ClassHierarchyResolver`. Mientras eso
// no esté, esto lo dice acá en vez de devolver bytes que parecen buenos.
//
// Las opciones (`ClassFile.Option` y sus enums anidados) no están, salvo la interfaz marcadora:
// gobiernan al escritor y ninguna tiene hoy un comportamiento que prender o apagar.
public interface ClassFile {

    /** Una instancia con las opciones por omisión. */
    public static ClassFile of() {
        return new jdk.internal.classfile.impl.ClassFileImpl();
    }

    /** Una instancia con estas opciones. */
    public static ClassFile of(Option... options) {
        return new jdk.internal.classfile.impl.ClassFileImpl().withOptions(options);
    }

    /** La misma instancia con estas opciones encima. */
    ClassFile withOptions(Option... options);

    /**
     * Lee un `.class`. Valida el magic, las versiones, el pool entero y la estructura de campos,
     * métodos y atributos; si algo no cierra tira `IllegalArgumentException` (o la
     * `ConstantPoolException` que hereda de ella) en vez de devolver un modelo a medio armar.
     */
    ClassModel parse(byte[] bytes);

    /** Lee el `.class` que está en `path`. */
    default ClassModel parse(Path path) throws IOException {
        return parse(Files.readAllBytes(path));
    }

    /** La versión mayor más nueva que esta implementación conoce. */
    public static int latestMajorVersion() {
        return JAVA_25_VERSION;
    }

    /** La versión menor más nueva que esta implementación conoce. */
    public static int latestMinorVersion() {
        return 0;
    }

    // ---- escritura ------------------------------------------------------------------------------

    /**
     * Escribe una clase con ese nombre, ese pool y lo que el `handler` le diga.
     *
     * <p>Ver la nota de alcance del encabezado: el `StackMapTable` no se sintetiza.
     */
    byte[] build(java.lang.classfile.constantpool.ClassEntry thisClassEntry,
            java.lang.classfile.constantpool.ConstantPoolBuilder constantPool,
            Consumer<ClassBuilder> handler);

    /** Lo mismo, con un pool nuevo. */
    default byte[] build(ClassDesc thisClassDesc, Consumer<ClassBuilder> handler) {
        java.lang.classfile.constantpool.ConstantPoolBuilder cp =
                java.lang.classfile.constantpool.ConstantPoolBuilder.of();
        return build(cp.classEntry(thisClassDesc), cp, handler);
    }

    /** Escribe la clase en ese archivo. */
    default void buildTo(Path path, ClassDesc thisClassDesc, Consumer<ClassBuilder> handler)
            throws IOException {
        Files.write(path, build(thisClassDesc, handler));
    }

    /** Escribe la clase en ese archivo. */
    default void buildTo(Path path, java.lang.classfile.constantpool.ClassEntry thisClassEntry,
            java.lang.classfile.constantpool.ConstantPoolBuilder constantPool,
            Consumer<ClassBuilder> handler) throws IOException {
        Files.write(path, build(thisClassEntry, constantPool, handler));
    }

    /**
     * Escribe un `module-info.class` con ese atributo `Module`.
     *
     * <p>Un descriptor de módulo es una clase con una forma fija: se llama `module-info`, es
     * `ACC_MODULE` y no tiene ni superclase ni miembros. Lo único propio es el atributo, y por eso
     * es lo único que este método pide.
     */
    default byte[] buildModule(java.lang.classfile.attribute.ModuleAttribute moduleAttribute) {
        return buildModule(moduleAttribute, new NoExtraModuleElements());
    }

    /** Lo mismo, más lo que el `handler` agregue (otros atributos del módulo). */
    default byte[] buildModule(java.lang.classfile.attribute.ModuleAttribute moduleAttribute,
            Consumer<ClassBuilder> handler) {
        return build(ClassDesc.of("module-info"),
                new ModuleClassHandler(moduleAttribute, handler));
    }

    /** Escribe el `module-info.class` en ese archivo. */
    default void buildModuleTo(Path path,
            java.lang.classfile.attribute.ModuleAttribute moduleAttribute) throws IOException {
        Files.write(path, buildModule(moduleAttribute));
    }

    /** Escribe el `module-info.class` en ese archivo. */
    default void buildModuleTo(Path path,
            java.lang.classfile.attribute.ModuleAttribute moduleAttribute,
            Consumer<ClassBuilder> handler) throws IOException {
        Files.write(path, buildModule(moduleAttribute, handler));
    }

    /** Copia esa clase a través de esa transformación, con ese nombre y ese pool. */
    byte[] transformClass(ClassModel model,
            java.lang.classfile.constantpool.ClassEntry newClassName, ClassTransform transform);

    /** Copia esa clase a través de esa transformación, conservando su nombre. */
    default byte[] transformClass(ClassModel model, ClassTransform transform) {
        return transformClass(model, model.thisClass(), transform);
    }

    /** Copia esa clase a través de esa transformación, con otro nombre. */
    default byte[] transformClass(ClassModel model, ClassDesc newClassName,
            ClassTransform transform) {
        java.lang.classfile.constantpool.ConstantPoolBuilder cp =
                java.lang.classfile.constantpool.ConstantPoolBuilder.of();
        return transformClass(model, cp.classEntry(newClassName), transform);
    }

    // ---- verificación ---------------------------------------------------------------------------

    /**
     * Los errores que se le encuentran a esa clase.
     *
     * <p><strong>Comprueba la ESTRUCTURA, no el flujo de tipos.</strong> Eso hay que leerlo al
     * derecho: una lista vacía significa "no se encontró ningún error estructural", **no** "esta
     * clase pasa el verificador de la JVM". El verificador de §4.10 --el que comprueba que la pila
     * tenga el tipo que cada instrucción espera-- es otra cosa y no está acá.
     *
     * <p>Lo que sí encuentra: un magic o una versión que no son, un pool inconsistente, un índice
     * fuera de rango, un largo de atributo que no entra en el archivo, un descriptor mal formado.
     * Es lo que el parseo ya valida; este método lo expone como lista en vez de como excepción.
     */
    List<VerifyError> verify(byte[] bytes);

    /** Lo mismo sobre un modelo ya leído. */
    List<VerifyError> verify(ClassModel model);

    /** Lo mismo sobre el archivo de esa ruta. */
    default List<VerifyError> verify(Path path) throws IOException {
        return verify(Files.readAllBytes(path));
    }

    /**
     * Una opción de lectura o de escritura. Es una interfaz marcadora; en el JDK sus
     * implementaciones son los enums anidados de `ClassFile`, que acá no están porque todos
     * gobiernan al escritor.
     */
    public interface Option {
    }

    /** `0xCAFEBABE`, los primeros cuatro bytes de todo `.class` (JVMS §4.1). */
    public static final int MAGIC_NUMBER = 0xCAFEBABE;

    /** `ACC_PUBLIC`. */
    public static final int ACC_PUBLIC = 0x0001;
    /** `ACC_PRIVATE`. */
    public static final int ACC_PRIVATE = 0x0002;
    /** `ACC_PROTECTED`. */
    public static final int ACC_PROTECTED = 0x0004;
    /** `ACC_STATIC`. */
    public static final int ACC_STATIC = 0x0008;
    /** `ACC_FINAL`. */
    public static final int ACC_FINAL = 0x0010;
    /** `ACC_SUPER` en una clase. */
    public static final int ACC_SUPER = 0x0020;
    /** `ACC_OPEN` en un módulo: el mismo bit que `ACC_SUPER`. */
    public static final int ACC_OPEN = 0x0020;
    /** `ACC_TRANSITIVE` en un `requires`: el mismo bit otra vez. */
    public static final int ACC_TRANSITIVE = 0x0020;
    /** `ACC_SYNCHRONIZED` en un método: el mismo bit otra vez. */
    public static final int ACC_SYNCHRONIZED = 0x0020;
    /** `ACC_STATIC_PHASE` en un `requires`. */
    public static final int ACC_STATIC_PHASE = 0x0040;
    /** `ACC_VOLATILE` en un campo. */
    public static final int ACC_VOLATILE = 0x0040;
    /** `ACC_BRIDGE` en un método: el mismo bit que `ACC_VOLATILE`. */
    public static final int ACC_BRIDGE = 0x0040;
    /** `ACC_TRANSIENT` en un campo. */
    public static final int ACC_TRANSIENT = 0x0080;
    /** `ACC_VARARGS` en un método: el mismo bit que `ACC_TRANSIENT`. */
    public static final int ACC_VARARGS = 0x0080;
    /** `ACC_NATIVE`. */
    public static final int ACC_NATIVE = 0x0100;
    /** `ACC_INTERFACE`. */
    public static final int ACC_INTERFACE = 0x0200;
    /** `ACC_ABSTRACT`. */
    public static final int ACC_ABSTRACT = 0x0400;
    /** `ACC_STRICT`. */
    public static final int ACC_STRICT = 0x0800;
    /** `ACC_SYNTHETIC`. */
    public static final int ACC_SYNTHETIC = 0x1000;
    /** `ACC_ANNOTATION`. */
    public static final int ACC_ANNOTATION = 0x2000;
    /** `ACC_ENUM`. */
    public static final int ACC_ENUM = 0x4000;
    /** `ACC_MANDATED`. */
    public static final int ACC_MANDATED = 0x8000;
    /** `ACC_MODULE`: el mismo bit que `ACC_MANDATED`, pero sólo válido en una clase. */
    public static final int ACC_MODULE = 0x8000;

    /** Versión mayor de Java 1.0/1.1. */
    public static final int JAVA_1_VERSION = 45;
    /** Versión mayor de Java 1.2. */
    public static final int JAVA_2_VERSION = 46;
    /** Versión mayor de Java 1.3. */
    public static final int JAVA_3_VERSION = 47;
    /** Versión mayor de Java 1.4. */
    public static final int JAVA_4_VERSION = 48;
    /** Versión mayor de Java 5. */
    public static final int JAVA_5_VERSION = 49;
    /** Versión mayor de Java 6. */
    public static final int JAVA_6_VERSION = 50;
    /** Versión mayor de Java 7. */
    public static final int JAVA_7_VERSION = 51;
    /** Versión mayor de Java 8. */
    public static final int JAVA_8_VERSION = 52;
    /** Versión mayor de Java 9. */
    public static final int JAVA_9_VERSION = 53;
    /** Versión mayor de Java 10. */
    public static final int JAVA_10_VERSION = 54;
    /** Versión mayor de Java 11. */
    public static final int JAVA_11_VERSION = 55;
    /** Versión mayor de Java 12. */
    public static final int JAVA_12_VERSION = 56;
    /** Versión mayor de Java 13. */
    public static final int JAVA_13_VERSION = 57;
    /** Versión mayor de Java 14. */
    public static final int JAVA_14_VERSION = 58;
    /** Versión mayor de Java 15. */
    public static final int JAVA_15_VERSION = 59;
    /** Versión mayor de Java 16. */
    public static final int JAVA_16_VERSION = 60;
    /** Versión mayor de Java 17. */
    public static final int JAVA_17_VERSION = 61;
    /** Versión mayor de Java 18. */
    public static final int JAVA_18_VERSION = 62;
    /** Versión mayor de Java 19. */
    public static final int JAVA_19_VERSION = 63;
    /** Versión mayor de Java 20. */
    public static final int JAVA_20_VERSION = 64;
    /** Versión mayor de Java 21. */
    public static final int JAVA_21_VERSION = 65;
    /** Versión mayor de Java 22. */
    public static final int JAVA_22_VERSION = 66;
    /** Versión mayor de Java 23. */
    public static final int JAVA_23_VERSION = 67;
    /** Versión mayor de Java 24. */
    public static final int JAVA_24_VERSION = 68;
    /** Versión mayor de Java 25. */
    public static final int JAVA_25_VERSION = 69;

    /** El `minor_version` que marca una clase de vista previa: `0xFFFF`. */
    public static final int PREVIEW_MINOR_VERSION = 0xFFFF;
}

// El `handler` que arma un `module-info`: el atributo del modulo, la bandera que el formato le
// exige, y despues lo que el llamador quiera agregar.
//
// Con nombre y no lambda: ver la nota de `ClassBuilder` sobre por que estas interfaces no pueden
// depender de `LambdaMetafactory`.
final class ModuleClassHandler implements Consumer<ClassBuilder> {

    private final java.lang.classfile.attribute.ModuleAttribute attr;
    private final Consumer<ClassBuilder> extra;

    ModuleClassHandler(java.lang.classfile.attribute.ModuleAttribute attr,
            Consumer<ClassBuilder> extra) {
        this.attr = attr;
        this.extra = extra;
    }

    public void accept(ClassBuilder cb) {
        // ACC_MODULE. Un `module-info` no es una clase que alguien pueda instanciar ni extender, y
        // esta bandera es lo que se lo dice a la JVM.
        cb.withFlags(0x8000);
        cb.with(this.attr);
        this.extra.accept(cb);
    }
}

// El `handler` vacio de `buildModule(ModuleAttribute)`.
final class NoExtraModuleElements implements Consumer<ClassBuilder> {

    public void accept(ClassBuilder cb) {
    }
}
