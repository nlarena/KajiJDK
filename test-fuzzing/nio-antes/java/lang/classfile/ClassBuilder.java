package java.lang.classfile;

import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.AccessFlag.Location;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import jdk.internal.classfile.impl.AccessFlagsImpl;
import jdk.internal.classfile.impl.ClassFileVersionImpl;
import jdk.internal.classfile.impl.InterfacesImpl;
import jdk.internal.classfile.impl.SuperclassImpl;

/**
 * Where a class gets written.
 *
 * <p>Everything a class has --version, flags, superclass, interfaces, fields, methods, attributes--
 * goes in through {@link #with}, and these methods are shortcuts building the element that
 * corresponds. One can write a whole class without using any of them; they exist because
 * `withSuperclass(CD_Object)` reads better than `with(SuperclassImpl.of(...))`.
 *
 * <p><strong>Two things do not go in as elements and that is why they are not here</strong>: the
 * class's name and its constant pool. Both are fixed at the start --they are arguments of
 * {@link ClassFile#build}-- because a class cannot change its name halfway through writing without
 * invalidating every reference to itself that has already been written.
 */
public interface ClassBuilder extends ClassFileBuilder<ClassElement, ClassBuilder> {

    /** The format's version. */
    default ClassBuilder withVersion(int major, int minor) {
        return this.with(new ClassFileVersionImpl(major, minor));
    }

    /** The class's flags, as a bit mask. */
    default ClassBuilder withFlags(int flags) {
        return this.with(new AccessFlagsImpl(flags, Location.CLASS));
    }

    /** The class's flags. */
    default ClassBuilder withFlags(AccessFlag... flags) {
        int m = 0;
        for (int i = 0; i < flags.length; i++) {
            m = m | flags[i].mask();
        }
        return this.with(new AccessFlagsImpl(m, Location.CLASS));
    }

    /** The superclass. */
    default ClassBuilder withSuperclass(ClassEntry superclassEntry) {
        return this.with(new SuperclassImpl(superclassEntry));
    }

    /** The superclass, by its descriptor. */
    default ClassBuilder withSuperclass(ClassDesc desc) {
        return this.withSuperclass(this.constantPool().classEntry(desc));
    }

    /** The interfaces it implements. */
    default ClassBuilder withInterfaces(List<ClassEntry> interfaces) {
        return this.with(new InterfacesImpl(interfaces));
    }

    /** The interfaces it implements. */
    default ClassBuilder withInterfaces(ClassEntry... interfaces) {
        List<ClassEntry> list = new ArrayList<ClassEntry>();
        for (int i = 0; i < interfaces.length; i++) {
            list.add(interfaces[i]);
        }
        return this.withInterfaces(list);
    }

    /** The interfaces it implements, by their descriptors. */
    default ClassBuilder withInterfaceSymbols(List<ClassDesc> interfaces) {
        List<ClassEntry> list = new ArrayList<ClassEntry>();
        for (int i = 0; i < interfaces.size(); i++) {
            list.add(this.constantPool().classEntry(interfaces.get(i)));
        }
        return this.withInterfaces(list);
    }

    /** The interfaces it implements, by their descriptors. */
    default ClassBuilder withInterfaceSymbols(ClassDesc... interfaces) {
        List<ClassDesc> list = new ArrayList<ClassDesc>();
        for (int i = 0; i < interfaces.length; i++) {
            list.add(interfaces[i]);
        }
        return this.withInterfaceSymbols(list);
    }

    /**
     * A field, with its body written by that `Consumer`.
     *
     * <p>The name and the descriptor are arguments and not elements for the same reason as the
     * class's name: they identify the field, and changing them halfway would be writing another one.
     */
    ClassBuilder withField(Utf8Entry name, Utf8Entry descriptor, Consumer<FieldBuilder> handler);

    /** A field with those flags and nothing else. */
    default ClassBuilder withField(Utf8Entry name, Utf8Entry descriptor, int flags) {
        return this.withField(name, descriptor, new FlagsOnlyField(flags));
    }

    /** A field, naming it with text and descriptor. */
    default ClassBuilder withField(String name, ClassDesc descriptor,
            Consumer<FieldBuilder> handler) {
        return this.withField(this.constantPool().utf8Entry(name),
                this.constantPool().utf8Entry(descriptor.descriptorString()), handler);
    }

    /** A field with those flags, naming it with text and descriptor. */
    default ClassBuilder withField(String name, ClassDesc descriptor, int flags) {
        return this.withField(this.constantPool().utf8Entry(name),
                this.constantPool().utf8Entry(descriptor.descriptorString()), flags);
    }

    /** It copies that field through that transformation. */
    ClassBuilder transformField(FieldModel field, FieldTransform transform);

    /** A method, with its body written by that `Consumer`. */
    ClassBuilder withMethod(Utf8Entry name, Utf8Entry descriptor, int flags,
            Consumer<MethodBuilder> handler);

    /** A method, naming it with text and descriptor. */
    default ClassBuilder withMethod(String name, MethodTypeDesc descriptor, int flags,
            Consumer<MethodBuilder> handler) {
        return this.withMethod(this.constantPool().utf8Entry(name),
                this.constantPool().utf8Entry(descriptor.descriptorString()), flags, handler);
    }

    /**
     * A method whose `Consumer` writes the **body** directly.
     *
     * <p>The difference from {@link #withMethod} is one layer: there the `Consumer` gets a
     * {@link MethodBuilder} and has to call `withCode`; here it gets the {@link CodeBuilder} already
     * open. It is the common case --a method with a body and nothing else-- and it saves the middle
     * step.
     */
    default ClassBuilder withMethodBody(Utf8Entry name, Utf8Entry descriptor, int flags,
            Consumer<CodeBuilder> handler) {
        return this.withMethod(name, descriptor, flags, new BodyOnlyMethod(handler));
    }

    /** The same, naming the method with text and descriptor. */
    default ClassBuilder withMethodBody(String name, MethodTypeDesc descriptor, int flags,
            Consumer<CodeBuilder> handler) {
        return this.withMethodBody(this.constantPool().utf8Entry(name),
                this.constantPool().utf8Entry(descriptor.descriptorString()), flags, handler);
    }

    /** It copies that method through that transformation. */
    ClassBuilder transformMethod(MethodModel method, MethodTransform transform);
}

// The two `Consumer`s the `default` methods above need.
//
// They are classes and not lambdas for a concrete reason and not out of style: a lambda inside a
// `default` of a public interface from the library's start-up forces `LambdaMetafactory` to be
// available at that moment, and these interfaces are loaded by the compiler itself. With named
// classes there is no `invokedynamic` to resolve.

final class FlagsOnlyField implements Consumer<FieldBuilder> {

    private final int flags;

    FlagsOnlyField(int flags) {
        this.flags = flags;
    }

    public void accept(FieldBuilder fb) {
        fb.withFlags(this.flags);
    }
}

final class BodyOnlyMethod implements Consumer<MethodBuilder> {

    private final Consumer<CodeBuilder> body;

    BodyOnlyMethod(Consumer<CodeBuilder> body) {
        this.body = body;
    }

    public void accept(MethodBuilder mb) {
        mb.withCode(this.body);
    }
}
