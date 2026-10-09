package jdk.internal.classfile.impl;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.FieldBuilder;
import java.lang.classfile.FieldElement;
import java.lang.classfile.FieldModel;
import java.lang.classfile.FieldTransform;
import java.lang.classfile.Label;
import java.lang.classfile.MethodBuilder;
import java.lang.classfile.MethodElement;
import java.lang.classfile.MethodModel;
import java.lang.classfile.MethodTransform;
import java.lang.classfile.TypeKind;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The implementations of the factories of the four transformations.
 *
 * <p>The four are together and not one per file because they are the **same** class written four
 * times: chaining, filtering, appending at the end and giving state do not depend on what is being
 * transformed. The repetition is the type system's, not the problem's -- `ClassTransform` and
 * `MethodTransform` have no common supertype that fixes `E` and `B`, so there is no way of writing
 * a single one.
 *
 * <h2>The chaining, which is the only non-obvious thing</h2>
 *
 * <p>`a.andThen(b)` does **not** run both on the original: it runs `b` on what `a` produces. For
 * that, `a` cannot write into the real builder -- it has to write into an intermediate one that
 * hands each element to `b`. That is what the `Chained*Builder` classes further down are: a builder
 * that inside is a transformation.
 */
public final class Transforms {

    private Transforms() {
    }

    // ---- the predicate that accepts everything --------------------------------------------------

    /** The method predicate that says yes to all. */
    public static Predicate<MethodModel> allMethods() {
        return AllMethods.INSTANCE;
    }

    // ---- class ---------------------------------------------------------------------------------

    /** A class transformation followed by another. */
    public static ClassTransform chainClass(ClassTransform first, ClassTransform second) {
        return new ChainedClass(first, second);
    }

    /** The one that drops what meets the predicate. */
    public static ClassTransform droppingClass(Predicate<ClassElement> filter) {
        return new DroppingClass(filter);
    }

    /** The one that lets everything through and runs that at the end. */
    public static ClassTransform endHandlerClass(Consumer<ClassBuilder> finisher) {
        return new EndHandlerClass(finisher);
    }

    /** The one that is made anew for each use. */
    public static ClassTransform statefulClass(Supplier<ClassTransform> supplier) {
        return new StatefulClass(supplier);
    }

    /** The one that transforms each field. */
    public static ClassTransform transformingFields(FieldTransform xform) {
        return new TransformingFields(xform);
    }

    /** The one that transforms the methods that meet the predicate. */
    public static ClassTransform transformingMethods(Predicate<MethodModel> filter,
            MethodTransform xform) {
        return new TransformingMethods(filter, xform);
    }

    // ---- method --------------------------------------------------------------------------------

    /** A method transformation followed by another. */
    public static MethodTransform chainMethod(MethodTransform first, MethodTransform second) {
        return new ChainedMethod(first, second);
    }

    /** The one that drops what meets the predicate. */
    public static MethodTransform droppingMethod(Predicate<MethodElement> filter) {
        return new DroppingMethod(filter);
    }

    /** The one that lets everything through and runs that at the end. */
    public static MethodTransform endHandlerMethod(Consumer<MethodBuilder> finisher) {
        return new EndHandlerMethod(finisher);
    }

    /** The one that is made anew for each use. */
    public static MethodTransform statefulMethod(Supplier<MethodTransform> supplier) {
        return new StatefulMethod(supplier);
    }

    /** The one that transforms the body. */
    public static MethodTransform transformingCode(CodeTransform xform) {
        return new TransformingCode(xform);
    }

    // ---- field ----------------------------------------------------------------------------------

    /** A field transformation followed by another. */
    public static FieldTransform chainField(FieldTransform first, FieldTransform second) {
        return new ChainedField(first, second);
    }

    /** The one that drops what meets the predicate. */
    public static FieldTransform droppingField(Predicate<FieldElement> filter) {
        return new DroppingField(filter);
    }

    /** The one that lets everything through and runs that at the end. */
    public static FieldTransform endHandlerField(Consumer<FieldBuilder> finisher) {
        return new EndHandlerField(finisher);
    }

    /** The one that is made anew for each use. */
    public static FieldTransform statefulField(Supplier<FieldTransform> supplier) {
        return new StatefulField(supplier);
    }

    // ---- code ----------------------------------------------------------------------------------

    /** A code transformation followed by another. */
    public static CodeTransform chainCode(CodeTransform first, CodeTransform second) {
        return new ChainedCode(first, second);
    }

    /** The one that lets everything through and runs that at the end. */
    public static CodeTransform endHandlerCode(Consumer<CodeBuilder> finisher) {
        return new EndHandlerCode(finisher);
    }

    /** The one that is made anew for each use. */
    public static CodeTransform statefulCode(Supplier<CodeTransform> supplier) {
        return new StatefulCode(supplier);
    }

    /**
     * A code builder that writes **through** that transformation.
     *
     * <p>`CodeBuilder.transforming` asks for it. The note said this factory exists because the
     * class is package-private and whoever needs it cannot name it; the caller,
     * `DirectCodeBuilder`, is in this very package and could. What the factory does give is a
     * single place where the chained builder is made.
     */
    public static CodeBuilder chainedCodeBuilder(CodeBuilder downstream, CodeTransform transform) {
        return new ChainedCodeBuilder(downstream, transform);
    }
}

final class AllMethods implements Predicate<MethodModel> {

    static final AllMethods INSTANCE = new AllMethods();

    public boolean test(MethodModel m) {
        return true;
    }
}

// ---- class -------------------------------------------------------------------------------------

final class ChainedClass implements ClassTransform {

    private final ClassTransform first;
    private final ClassTransform second;

    ChainedClass(ClassTransform first, ClassTransform second) {
        this.first = first;
        this.second = second;
    }

    public void accept(ClassBuilder builder, ClassElement element) {
        this.first.accept(new ChainedClassBuilder(builder, this.second), element);
    }

    public void atStart(ClassBuilder builder) {
        ClassBuilder inner = new ChainedClassBuilder(builder, this.second);
        this.first.atStart(inner);
        this.second.atStart(builder);
    }

    // The order is the reverse of `atStart`'s, and it has to be: what the first writes in its
    // closing still has to go through the second, so the second closes afterwards.
    public void atEnd(ClassBuilder builder) {
        this.first.atEnd(new ChainedClassBuilder(builder, this.second));
        this.second.atEnd(builder);
    }
}

// A `ClassBuilder` that inside is a transformation: what is written into it does not go to the
// destination builder but to the transformation's `accept`, with the destination as output.
final class ChainedClassBuilder implements ClassBuilder {

    private final ClassBuilder downstream;
    private final ClassTransform transform;

    ChainedClassBuilder(ClassBuilder downstream, ClassTransform transform) {
        this.downstream = downstream;
        this.transform = transform;
    }

    public ClassBuilder with(ClassElement e) {
        this.transform.accept(this.downstream, e);
        return this;
    }

    public ConstantPoolBuilder constantPool() {
        return this.downstream.constantPool();
    }

    // The four below go straight to the destination and do **not** go through the transformation.
    // It is not an omission: the transformation works on elements, and a field or a method created
    // with `withField`/`withMethod` is not an element somebody emitted -- it is a new structure
    // whose creator already decided how they want it.
    public ClassBuilder withField(Utf8Entry name, Utf8Entry descriptor,
            Consumer<FieldBuilder> handler) {
        this.downstream.withField(name, descriptor, handler);
        return this;
    }

    public ClassBuilder transformField(FieldModel field, FieldTransform xform) {
        this.downstream.transformField(field, xform);
        return this;
    }

    public ClassBuilder withMethod(Utf8Entry name, Utf8Entry descriptor, int flags,
            Consumer<MethodBuilder> handler) {
        this.downstream.withMethod(name, descriptor, flags, handler);
        return this;
    }

    public ClassBuilder transformMethod(MethodModel method, MethodTransform xform) {
        this.downstream.transformMethod(method, xform);
        return this;
    }
}

final class DroppingClass implements ClassTransform {

    private final Predicate<ClassElement> filter;

    DroppingClass(Predicate<ClassElement> filter) {
        this.filter = filter;
    }

    public void accept(ClassBuilder builder, ClassElement element) {
        if (!this.filter.test(element)) {
            builder.with(element);
        }
    }
}

final class EndHandlerClass implements ClassTransform {

    private final Consumer<ClassBuilder> finisher;

    EndHandlerClass(Consumer<ClassBuilder> finisher) {
        this.finisher = finisher;
    }

    public void accept(ClassBuilder builder, ClassElement element) {
        builder.with(element);
    }

    public void atEnd(ClassBuilder builder) {
        this.finisher.accept(builder);
    }
}

// The transformation with state. It asks the supplier for a new one in `atStart` and uses it until
// `atEnd`.
//
// The mutable field is exactly what this class exists to encapsulate: a transformation with state
// cannot be shared, and keeping it here --with a new instance per application-- is what spares
// whoever uses it from having to know.
final class StatefulClass implements ClassTransform {

    private final Supplier<ClassTransform> supplier;
    private ClassTransform current;

    StatefulClass(Supplier<ClassTransform> supplier) {
        this.supplier = supplier;
    }

    public void atStart(ClassBuilder builder) {
        this.current = this.supplier.get();
        this.current.atStart(builder);
    }

    public void accept(ClassBuilder builder, ClassElement element) {
        this.current.accept(builder, element);
    }

    public void atEnd(ClassBuilder builder) {
        this.current.atEnd(builder);
        this.current = null;
    }
}

final class TransformingFields implements ClassTransform {

    private final FieldTransform xform;

    TransformingFields(FieldTransform xform) {
        this.xform = xform;
    }

    public void accept(ClassBuilder builder, ClassElement element) {
        if (element instanceof FieldModel) {
            builder.transformField((FieldModel) element, this.xform);
        } else {
            builder.with(element);
        }
    }
}

final class TransformingMethods implements ClassTransform {

    private final Predicate<MethodModel> filter;
    private final MethodTransform xform;

    TransformingMethods(Predicate<MethodModel> filter, MethodTransform xform) {
        this.filter = filter;
        this.xform = xform;
    }

    public void accept(ClassBuilder builder, ClassElement element) {
        if (element instanceof MethodModel && this.filter.test((MethodModel) element)) {
            builder.transformMethod((MethodModel) element, this.xform);
        } else {
            builder.with(element);
        }
    }
}

// ---- method ----------------------------------------------------------------------------------

final class ChainedMethod implements MethodTransform {

    private final MethodTransform first;
    private final MethodTransform second;

    ChainedMethod(MethodTransform first, MethodTransform second) {
        this.first = first;
        this.second = second;
    }

    public void accept(MethodBuilder builder, MethodElement element) {
        this.first.accept(new ChainedMethodBuilder(builder, this.second), element);
    }

    public void atStart(MethodBuilder builder) {
        this.first.atStart(new ChainedMethodBuilder(builder, this.second));
        this.second.atStart(builder);
    }

    public void atEnd(MethodBuilder builder) {
        this.first.atEnd(new ChainedMethodBuilder(builder, this.second));
        this.second.atEnd(builder);
    }
}

final class ChainedMethodBuilder implements MethodBuilder {

    private final MethodBuilder downstream;
    private final MethodTransform transform;

    ChainedMethodBuilder(MethodBuilder downstream, MethodTransform transform) {
        this.downstream = downstream;
        this.transform = transform;
    }

    public MethodBuilder with(MethodElement e) {
        this.transform.accept(this.downstream, e);
        return this;
    }

    public ConstantPoolBuilder constantPool() {
        return this.downstream.constantPool();
    }

    public MethodBuilder withCode(Consumer<CodeBuilder> code) {
        this.downstream.withCode(code);
        return this;
    }

    public MethodBuilder transformCode(CodeModel code, CodeTransform xform) {
        this.downstream.transformCode(code, xform);
        return this;
    }
}

final class DroppingMethod implements MethodTransform {

    private final Predicate<MethodElement> filter;

    DroppingMethod(Predicate<MethodElement> filter) {
        this.filter = filter;
    }

    public void accept(MethodBuilder builder, MethodElement element) {
        if (!this.filter.test(element)) {
            builder.with(element);
        }
    }
}

final class EndHandlerMethod implements MethodTransform {

    private final Consumer<MethodBuilder> finisher;

    EndHandlerMethod(Consumer<MethodBuilder> finisher) {
        this.finisher = finisher;
    }

    public void accept(MethodBuilder builder, MethodElement element) {
        builder.with(element);
    }

    public void atEnd(MethodBuilder builder) {
        this.finisher.accept(builder);
    }
}

final class StatefulMethod implements MethodTransform {

    private final Supplier<MethodTransform> supplier;
    private MethodTransform current;

    StatefulMethod(Supplier<MethodTransform> supplier) {
        this.supplier = supplier;
    }

    public void atStart(MethodBuilder builder) {
        this.current = this.supplier.get();
        this.current.atStart(builder);
    }

    public void accept(MethodBuilder builder, MethodElement element) {
        this.current.accept(builder, element);
    }

    public void atEnd(MethodBuilder builder) {
        this.current.atEnd(builder);
        this.current = null;
    }
}

final class TransformingCode implements MethodTransform {

    private final CodeTransform xform;

    TransformingCode(CodeTransform xform) {
        this.xform = xform;
    }

    public void accept(MethodBuilder builder, MethodElement element) {
        if (element instanceof CodeModel) {
            builder.transformCode((CodeModel) element, this.xform);
        } else {
            builder.with(element);
        }
    }
}

// ---- field -----------------------------------------------------------------------------------

final class ChainedField implements FieldTransform {

    private final FieldTransform first;
    private final FieldTransform second;

    ChainedField(FieldTransform first, FieldTransform second) {
        this.first = first;
        this.second = second;
    }

    public void accept(FieldBuilder builder, FieldElement element) {
        this.first.accept(new ChainedFieldBuilder(builder, this.second), element);
    }

    public void atStart(FieldBuilder builder) {
        this.first.atStart(new ChainedFieldBuilder(builder, this.second));
        this.second.atStart(builder);
    }

    public void atEnd(FieldBuilder builder) {
        this.first.atEnd(new ChainedFieldBuilder(builder, this.second));
        this.second.atEnd(builder);
    }
}

final class ChainedFieldBuilder implements FieldBuilder {

    private final FieldBuilder downstream;
    private final FieldTransform transform;

    ChainedFieldBuilder(FieldBuilder downstream, FieldTransform transform) {
        this.downstream = downstream;
        this.transform = transform;
    }

    public FieldBuilder with(FieldElement e) {
        this.transform.accept(this.downstream, e);
        return this;
    }

    public ConstantPoolBuilder constantPool() {
        return this.downstream.constantPool();
    }
}

final class DroppingField implements FieldTransform {

    private final Predicate<FieldElement> filter;

    DroppingField(Predicate<FieldElement> filter) {
        this.filter = filter;
    }

    public void accept(FieldBuilder builder, FieldElement element) {
        if (!this.filter.test(element)) {
            builder.with(element);
        }
    }
}

final class EndHandlerField implements FieldTransform {

    private final Consumer<FieldBuilder> finisher;

    EndHandlerField(Consumer<FieldBuilder> finisher) {
        this.finisher = finisher;
    }

    public void accept(FieldBuilder builder, FieldElement element) {
        builder.with(element);
    }

    public void atEnd(FieldBuilder builder) {
        this.finisher.accept(builder);
    }
}

final class StatefulField implements FieldTransform {

    private final Supplier<FieldTransform> supplier;
    private FieldTransform current;

    StatefulField(Supplier<FieldTransform> supplier) {
        this.supplier = supplier;
    }

    public void atStart(FieldBuilder builder) {
        this.current = this.supplier.get();
        this.current.atStart(builder);
    }

    public void accept(FieldBuilder builder, FieldElement element) {
        this.current.accept(builder, element);
    }

    public void atEnd(FieldBuilder builder) {
        this.current.atEnd(builder);
        this.current = null;
    }
}

// ---- code ------------------------------------------------------------------------------------

final class ChainedCode implements CodeTransform {

    private final CodeTransform first;
    private final CodeTransform second;

    ChainedCode(CodeTransform first, CodeTransform second) {
        this.first = first;
        this.second = second;
    }

    public void accept(CodeBuilder builder, CodeElement element) {
        this.first.accept(new ChainedCodeBuilder(builder, this.second), element);
    }

    public void atStart(CodeBuilder builder) {
        this.first.atStart(new ChainedCodeBuilder(builder, this.second));
        this.second.atStart(builder);
    }

    public void atEnd(CodeBuilder builder) {
        this.first.atEnd(new ChainedCodeBuilder(builder, this.second));
        this.second.atEnd(builder);
    }
}

final class ChainedCodeBuilder implements CodeBuilder {

    private final CodeBuilder downstream;
    private final CodeTransform transform;

    ChainedCodeBuilder(CodeBuilder downstream, CodeTransform transform) {
        this.downstream = downstream;
        this.transform = transform;
    }

    public CodeBuilder with(CodeElement e) {
        this.transform.accept(this.downstream, e);
        return this;
    }

    public ConstantPoolBuilder constantPool() {
        return this.downstream.constantPool();
    }

    // The labels and the slots are the destination builder's: a label asked for here has to resolve
    // in the method really being written, not in this intermediary.
    public Label newLabel() {
        return this.downstream.newLabel();
    }

    public Label startLabel() {
        return this.downstream.startLabel();
    }

    public Label endLabel() {
        return this.downstream.endLabel();
    }

    public int receiverSlot() {
        return this.downstream.receiverSlot();
    }

    public int parameterSlot(int paramNo) {
        return this.downstream.parameterSlot(paramNo);
    }

    public int allocateLocal(TypeKind typeKind) {
        return this.downstream.allocateLocal(typeKind);
    }

    public CodeBuilder.CatchBuilder catchBuilder(Label tryStart, Label tryEnd, Label end) {
        return this.downstream.catchBuilder(tryStart, tryEnd, end);
    }

    public CodeBuilder transformingBuilder(CodeTransform xform) {
        return new ChainedCodeBuilder(this, xform);
    }
}

final class EndHandlerCode implements CodeTransform {

    private final Consumer<CodeBuilder> finisher;

    EndHandlerCode(Consumer<CodeBuilder> finisher) {
        this.finisher = finisher;
    }

    public void accept(CodeBuilder builder, CodeElement element) {
        builder.with(element);
    }

    public void atEnd(CodeBuilder builder) {
        this.finisher.accept(builder);
    }
}

final class StatefulCode implements CodeTransform {

    private final Supplier<CodeTransform> supplier;
    private CodeTransform current;

    StatefulCode(Supplier<CodeTransform> supplier) {
        this.supplier = supplier;
    }

    public void atStart(CodeBuilder builder) {
        this.current = this.supplier.get();
        this.current.atStart(builder);
    }

    public void accept(CodeBuilder builder, CodeElement element) {
        this.current.accept(builder, element);
    }

    public void atEnd(CodeBuilder builder) {
        this.current.atEnd(builder);
        this.current = null;
    }
}
