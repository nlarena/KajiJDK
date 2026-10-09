package java.lang.classfile;

import java.util.function.Consumer;
import java.util.function.Supplier;
import jdk.internal.classfile.impl.Transforms;

/**
 * A transformation over a method's instructions.
 *
 * <p>It is the one doing the interesting work --instrumenting, rewriting calls, counting-- and the
 * only one of the four that does **not** have `dropping`. That is no oversight of the JDK's: dropping
 * a lone instruction nearly always leaves the method inconsistent, because instructions depend on
 * what the earlier ones left on the stack. Filtering code is rewriting it, and {@link #accept} is
 * there for that.
 */
public interface CodeTransform extends ClassFileTransform<CodeTransform, CodeElement, CodeBuilder> {

    /** The one letting everything through as it is. */
    public static final CodeTransform ACCEPT_ALL = new AcceptAllCode();

    /** This one and then that other one. */
    default CodeTransform andThen(CodeTransform next) {
        return Transforms.chainCode(this, next);
    }

    /** The one letting everything through and running that at the end. */
    public static CodeTransform endHandler(Consumer<CodeBuilder> finisher) {
        return Transforms.endHandlerCode(finisher);
    }

    /** A stateful transformation, made anew for each use. */
    public static CodeTransform ofStateful(Supplier<CodeTransform> supplier) {
        return Transforms.statefulCode(supplier);
    }
}

// The implementation of `CodeTransform.ACCEPT_ALL`. Named and not anonymous: our javac emits no
// anonymous class
// in an interface field's initialiser, and the name shows up in stack dumps as a bonus.
final class AcceptAllCode implements CodeTransform {

    public void accept(CodeBuilder builder, CodeElement element) {
        builder.with(element);
    }

    public String toString() {
        return "CodeTransform.ACCEPT_ALL";
    }
}
