package java.lang.classfile;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import jdk.internal.classfile.impl.Transforms;

/** A transformation over a method's elements. See {@link ClassFileTransform}. */
public interface MethodTransform
        extends ClassFileTransform<MethodTransform, MethodElement, MethodBuilder> {

    /** The one letting everything through as it is. */
    public static final MethodTransform ACCEPT_ALL = new AcceptAllMethod();

    /** This one and then that other one. */
    default MethodTransform andThen(MethodTransform next) {
        return Transforms.chainMethod(this, next);
    }

    /** The one dropping the elements that satisfy the predicate. */
    public static MethodTransform dropping(Predicate<MethodElement> filter) {
        return Transforms.droppingMethod(filter);
    }

    /** The one letting everything through and running that at the end. */
    public static MethodTransform endHandler(Consumer<MethodBuilder> finisher) {
        return Transforms.endHandlerMethod(finisher);
    }

    /** A stateful transformation, made anew for each use. */
    public static MethodTransform ofStateful(Supplier<MethodTransform> supplier) {
        return Transforms.statefulMethod(supplier);
    }

    /**
     * The one transforming the method's `Code` and leaving the other elements alone.
     *
     * <p>It tells the body from the rest because they are different things: the method's flags, its
     * declared exceptions and its annotations pass through as they are, and only the code enters the
     * code transformation.
     */
    public static MethodTransform transformingCode(CodeTransform xform) {
        return Transforms.transformingCode(xform);
    }
}

// The implementation of `MethodTransform.ACCEPT_ALL`. Named and not anonymous: our javac emits no
// anonymous class
// in an interface field's initialiser, and the name shows up in stack dumps as a bonus.
final class AcceptAllMethod implements MethodTransform {

    public void accept(MethodBuilder builder, MethodElement element) {
        builder.with(element);
    }

    public String toString() {
        return "MethodTransform.ACCEPT_ALL";
    }
}
