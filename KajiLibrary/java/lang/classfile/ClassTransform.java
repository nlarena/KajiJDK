package java.lang.classfile;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import jdk.internal.classfile.impl.Transforms;

/**
 * A transformation over a class's elements.
 *
 * <p>See {@link ClassFileTransform} for the general shape. What is this one's own are the factories:
 * nearly everything one wants to do to a class is one of them or two of them chained.
 */
public interface ClassTransform
        extends ClassFileTransform<ClassTransform, ClassElement, ClassBuilder> {

    /** The one letting everything through as it is. Useful as a chain's starting point. */
    public static final ClassTransform ACCEPT_ALL = new AcceptAllClass();

    /** This one and then that other one. See {@link ClassFileTransform#andThen}. */
    default ClassTransform andThen(ClassTransform next) {
        return Transforms.chainClass(this, next);
    }

    /** The one dropping the elements that satisfy the predicate and letting the rest through. */
    public static ClassTransform dropping(Predicate<ClassElement> filter) {
        return Transforms.droppingClass(filter);
    }

    /**
     * The one letting everything through and running that at the end.
     *
     * <p>It is how something is added to a class: the `Consumer` gets the builder once every original
     * element has been copied, and whatever it writes there ends up at the end.
     */
    public static ClassTransform endHandler(Consumer<ClassBuilder> finisher) {
        return Transforms.endHandlerClass(finisher);
    }

    /**
     * A **stateful** transformation, made anew for each use.
     *
     * <p>The `Supplier` is what makes it safe: a transformer that counts or remembers what it saw
     * cannot be shared between two transformations, because one's state would contaminate the other.
     * With the factory, each application starts with its own.
     */
    public static ClassTransform ofStateful(Supplier<ClassTransform> supplier) {
        return Transforms.statefulClass(supplier);
    }

    /** The one transforming each field with that transformation and leaving the rest alone. */
    public static ClassTransform transformingFields(FieldTransform xform) {
        return Transforms.transformingFields(xform);
    }

    /** The one transforming each method. */
    public static ClassTransform transformingMethods(MethodTransform xform) {
        return Transforms.transformingMethods(Transforms.allMethods(), xform);
    }

    /** The one transforming only the methods that satisfy the predicate. */
    public static ClassTransform transformingMethods(Predicate<MethodModel> filter,
            MethodTransform xform) {
        return Transforms.transformingMethods(filter, xform);
    }

    /**
     * The one transforming each method's **body**.
     *
     * <p>It is the most used shortcut of all, and that is why it is here: `transformingMethods` with
     * `MethodTransform.transformingCode` written out by hand is the same thing and reads worse.
     */
    public static ClassTransform transformingMethodBodies(CodeTransform xform) {
        return ClassTransform.transformingMethods(MethodTransform.transformingCode(xform));
    }

    /** The same, only for the methods that satisfy the predicate. */
    public static ClassTransform transformingMethodBodies(Predicate<MethodModel> filter,
            CodeTransform xform) {
        return ClassTransform.transformingMethods(filter,
                MethodTransform.transformingCode(xform));
    }
}

// The implementation of `ClassTransform.ACCEPT_ALL`. Named and not anonymous: our javac emits no
// anonymous class
// in an interface field's initialiser, and the name shows up in stack dumps as a bonus.
final class AcceptAllClass implements ClassTransform {

    public void accept(ClassBuilder builder, ClassElement element) {
        builder.with(element);
    }

    public String toString() {
        return "ClassTransform.ACCEPT_ALL";
    }
}
