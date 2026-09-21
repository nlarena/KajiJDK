package java.lang.classfile;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import jdk.internal.classfile.impl.Transforms;

/** A transformation over a field's elements. See {@link ClassFileTransform}. */
public interface FieldTransform
        extends ClassFileTransform<FieldTransform, FieldElement, FieldBuilder> {

    /** The one letting everything through as it is. */
    public static final FieldTransform ACCEPT_ALL = new AcceptAllField();

    /** This one and then that other one. */
    default FieldTransform andThen(FieldTransform next) {
        return Transforms.chainField(this, next);
    }

    /** The one dropping the elements that satisfy the predicate. */
    public static FieldTransform dropping(Predicate<FieldElement> filter) {
        return Transforms.droppingField(filter);
    }

    /** The one letting everything through and running that at the end. */
    public static FieldTransform endHandler(Consumer<FieldBuilder> finisher) {
        return Transforms.endHandlerField(finisher);
    }

    /** A stateful transformation, made anew for each use. */
    public static FieldTransform ofStateful(Supplier<FieldTransform> supplier) {
        return Transforms.statefulField(supplier);
    }
}

// The implementation of `FieldTransform.ACCEPT_ALL`. Named and not anonymous: our javac emits no
// anonymous class
// in an interface field's initialiser, and the name shows up in stack dumps as a bonus.
final class AcceptAllField implements FieldTransform {

    public void accept(FieldBuilder builder, FieldElement element) {
        builder.with(element);
    }

    public String toString() {
        return "FieldTransform.ACCEPT_ALL";
    }
}
