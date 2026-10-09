package java.lang.classfile;

import java.io.IOException;
import java.lang.constant.ClassDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

// The API's factory: `ClassModel`s come out of here, new `.class` files are written here, and the
// format's constants live here.
//
// SCOPE, and it is worth reading before using `build`: what gets written is **exactly what it was
// told**. The `Code` comes out with its `max_stack` computed --by walking the flow graph, not by
// adding up--, its `max_locals`, its exception table and its debug attributes; what does NOT come out
// is a synthesised `StackMapTable`. If the caller adds one, it is written; if not, the method is left
// without it.
//
// The consequence is concrete: a class of version 50 or greater, with jumps and without a
// `StackMapTable`, **does not pass a JVM's verifier**. The JDK computes it by itself. Computing it is
// not a detail missing out of carelessness -- it is a type inference over the whole graph, needing the
// common supertype of each join, which is precisely what `ClassHierarchyResolver` exists for. Until
// that is in place, this says so here instead of handing back bytes that look good.
//
// The options (`ClassFile.Option` and its nested enums) are not here, apart from the marker
// interface: they govern the writer and none of them has a behaviour to turn on or off today.
public interface ClassFile {

    /** An instance with the default options. */
    public static ClassFile of() {
        return new jdk.internal.classfile.impl.ClassFileImpl();
    }

    /** An instance with these options. */
    public static ClassFile of(Option... options) {
        return new jdk.internal.classfile.impl.ClassFileImpl().withOptions(options);
    }

    /** The same instance with these options on top. */
    ClassFile withOptions(Option... options);

    /**
     * It reads a `.class`. It validates the magic, the versions, the whole pool and the structure of
     * fields, methods and attributes; if something does not add up it throws
     * `IllegalArgumentException` (or the `ConstantPoolException` inheriting from it) instead of
     * handing back a half-built model.
     */
    ClassModel parse(byte[] bytes);

    /** It reads the `.class` sitting at `path`. */
    default ClassModel parse(Path path) throws IOException {
        return parse(Files.readAllBytes(path));
    }

    /** The newest major version this implementation knows. */
    public static int latestMajorVersion() {
        return JAVA_25_VERSION;
    }

    /** The newest minor version this implementation knows. */
    public static int latestMinorVersion() {
        return 0;
    }

    // ---- writing --------------------------------------------------------------------------------

    /**
     * It writes a class with that name, that pool and whatever the `handler` tells it.
     *
     * <p>See the scope note in the header: the `StackMapTable` is not synthesised.
     */
    byte[] build(java.lang.classfile.constantpool.ClassEntry thisClassEntry,
            java.lang.classfile.constantpool.ConstantPoolBuilder constantPool,
            Consumer<ClassBuilder> handler);

    /** The same, with a new pool. */
    default byte[] build(ClassDesc thisClassDesc, Consumer<ClassBuilder> handler) {
        java.lang.classfile.constantpool.ConstantPoolBuilder cp =
                java.lang.classfile.constantpool.ConstantPoolBuilder.of();
        return build(cp.classEntry(thisClassDesc), cp, handler);
    }

    /** It writes the class into that file. */
    default void buildTo(Path path, ClassDesc thisClassDesc, Consumer<ClassBuilder> handler)
            throws IOException {
        Files.write(path, build(thisClassDesc, handler));
    }

    /** It writes the class into that file. */
    default void buildTo(Path path, java.lang.classfile.constantpool.ClassEntry thisClassEntry,
            java.lang.classfile.constantpool.ConstantPoolBuilder constantPool,
            Consumer<ClassBuilder> handler) throws IOException {
        Files.write(path, build(thisClassEntry, constantPool, handler));
    }

    /**
     * It writes a `module-info.class` with that `Module` attribute.
     *
     * <p>A module descriptor is a class with a fixed shape: it is called `module-info`, it is
     * `ACC_MODULE` and it has neither a superclass nor members. The only thing of its own is the
     * attribute, and that is why it is the only thing this method asks for.
     */
    default byte[] buildModule(java.lang.classfile.attribute.ModuleAttribute moduleAttribute) {
        return buildModule(moduleAttribute, new NoExtraModuleElements());
    }

    /** The same, plus whatever the `handler` adds (other module attributes). */
    default byte[] buildModule(java.lang.classfile.attribute.ModuleAttribute moduleAttribute,
            Consumer<ClassBuilder> handler) {
        return build(ClassDesc.of("module-info"),
                new ModuleClassHandler(moduleAttribute, handler));
    }

    /** It writes the `module-info.class` into that file. */
    default void buildModuleTo(Path path,
            java.lang.classfile.attribute.ModuleAttribute moduleAttribute) throws IOException {
        Files.write(path, buildModule(moduleAttribute));
    }

    /** It writes the `module-info.class` into that file. */
    default void buildModuleTo(Path path,
            java.lang.classfile.attribute.ModuleAttribute moduleAttribute,
            Consumer<ClassBuilder> handler) throws IOException {
        Files.write(path, buildModule(moduleAttribute, handler));
    }

    /** It copies that class through that transformation, with that name and that pool. */
    byte[] transformClass(ClassModel model,
            java.lang.classfile.constantpool.ClassEntry newClassName, ClassTransform transform);

    /** It copies that class through that transformation, keeping its name. */
    default byte[] transformClass(ClassModel model, ClassTransform transform) {
        return transformClass(model, model.thisClass(), transform);
    }

    /** It copies that class through that transformation, under another name. */
    default byte[] transformClass(ClassModel model, ClassDesc newClassName,
            ClassTransform transform) {
        java.lang.classfile.constantpool.ConstantPoolBuilder cp =
                java.lang.classfile.constantpool.ConstantPoolBuilder.of();
        return transformClass(model, cp.classEntry(newClassName), transform);
    }

    // ---- verification ---------------------------------------------------------------------------

    /**
     * The errors found in that class.
     *
     * <p><strong>It checks the STRUCTURE, not the type flow.</strong> That has to be read straight:
     * an empty list means "no structural error was found", **not** "this class passes the JVM's
     * verifier". The §4.10 verifier --the one checking that the stack holds the type each instruction
     * expects-- is another thing and it is not here.
     *
     * <p>What it does find: a magic or a version that are not right, an inconsistent pool, an index
     * out of range, an attribute length that does not fit in the file, a malformed descriptor. It is
     * what parsing already validates; this method exposes it as a list instead of as an exception.
     */
    List<VerifyError> verify(byte[] bytes);

    /** The same over an already read model. */
    List<VerifyError> verify(ClassModel model);

    /** The same over the file at that path. */
    default List<VerifyError> verify(Path path) throws IOException {
        return verify(Files.readAllBytes(path));
    }

    /**
     * A reading or writing option. It is a marker interface; in the JDK its implementations are
     * `ClassFile`'s nested enums, which are not here because they all govern the writer.
     */
    public interface Option {
    }

    /** `0xCAFEBABE`, the first four bytes of every `.class` (JVMS §4.1). */
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
    /** `ACC_SUPER` on a class. */
    public static final int ACC_SUPER = 0x0020;
    /** `ACC_OPEN` on a module: the same bit as `ACC_SUPER`. */
    public static final int ACC_OPEN = 0x0020;
    /** `ACC_TRANSITIVE` on a `requires`: the same bit again. */
    public static final int ACC_TRANSITIVE = 0x0020;
    /** `ACC_SYNCHRONIZED` on a method: the same bit again. */
    public static final int ACC_SYNCHRONIZED = 0x0020;
    /** `ACC_STATIC_PHASE` on a `requires`. */
    public static final int ACC_STATIC_PHASE = 0x0040;
    /** `ACC_VOLATILE` on a field. */
    public static final int ACC_VOLATILE = 0x0040;
    /** `ACC_BRIDGE` on a method: the same bit as `ACC_VOLATILE`. */
    public static final int ACC_BRIDGE = 0x0040;
    /** `ACC_TRANSIENT` on a field. */
    public static final int ACC_TRANSIENT = 0x0080;
    /** `ACC_VARARGS` on a method: the same bit as `ACC_TRANSIENT`. */
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
    /** `ACC_MODULE`: the same bit as `ACC_MANDATED`, but only valid on a class. */
    public static final int ACC_MODULE = 0x8000;

    /** Java 1.0/1.1's major version. */
    public static final int JAVA_1_VERSION = 45;
    /** Java 1.2's major version. */
    public static final int JAVA_2_VERSION = 46;
    /** Java 1.3's major version. */
    public static final int JAVA_3_VERSION = 47;
    /** Java 1.4's major version. */
    public static final int JAVA_4_VERSION = 48;
    /** Java 5's major version. */
    public static final int JAVA_5_VERSION = 49;
    /** Java 6's major version. */
    public static final int JAVA_6_VERSION = 50;
    /** Java 7's major version. */
    public static final int JAVA_7_VERSION = 51;
    /** Java 8's major version. */
    public static final int JAVA_8_VERSION = 52;
    /** Java 9's major version. */
    public static final int JAVA_9_VERSION = 53;
    /** Java 10's major version. */
    public static final int JAVA_10_VERSION = 54;
    /** Java 11's major version. */
    public static final int JAVA_11_VERSION = 55;
    /** Java 12's major version. */
    public static final int JAVA_12_VERSION = 56;
    /** Java 13's major version. */
    public static final int JAVA_13_VERSION = 57;
    /** Java 14's major version. */
    public static final int JAVA_14_VERSION = 58;
    /** Java 15's major version. */
    public static final int JAVA_15_VERSION = 59;
    /** Java 16's major version. */
    public static final int JAVA_16_VERSION = 60;
    /** Java 17's major version. */
    public static final int JAVA_17_VERSION = 61;
    /** Java 18's major version. */
    public static final int JAVA_18_VERSION = 62;
    /** Java 19's major version. */
    public static final int JAVA_19_VERSION = 63;
    /** Java 20's major version. */
    public static final int JAVA_20_VERSION = 64;
    /** Java 21's major version. */
    public static final int JAVA_21_VERSION = 65;
    /** Java 22's major version. */
    public static final int JAVA_22_VERSION = 66;
    /** Java 23's major version. */
    public static final int JAVA_23_VERSION = 67;
    /** Java 24's major version. */
    public static final int JAVA_24_VERSION = 68;
    /** Java 25's major version. */
    public static final int JAVA_25_VERSION = 69;

    /** The `minor_version` marking a preview class: `0xFFFF`. */
    public static final int PREVIEW_MINOR_VERSION = 0xFFFF;
}

// The `handler` building a `module-info`: the module's attribute, the flag the format demands of it,
// and then whatever the caller wants to add.
//
// Named and not a lambda: see the note on `ClassBuilder` about why these interfaces cannot depend on
// `LambdaMetafactory`.
final class ModuleClassHandler implements Consumer<ClassBuilder> {

    private final java.lang.classfile.attribute.ModuleAttribute attr;
    private final Consumer<ClassBuilder> extra;

    ModuleClassHandler(java.lang.classfile.attribute.ModuleAttribute attr,
            Consumer<ClassBuilder> extra) {
        this.attr = attr;
        this.extra = extra;
    }

    public void accept(ClassBuilder cb) {
        // ACC_MODULE. A `module-info` is not a class anyone can instantiate or extend, and this
        // flag is what tells the JVM so.
        cb.withFlags(0x8000);
        cb.with(this.attr);
        this.extra.accept(cb);
    }
}

// `buildModule(ModuleAttribute)`'s empty `handler`.
final class NoExtraModuleElements implements Consumer<ClassBuilder> {

    public void accept(ClassBuilder cb) {
    }
}
