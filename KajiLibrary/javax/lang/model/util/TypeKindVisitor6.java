package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;

/**
 * KajiLibrary's javax.lang.model.util.TypeKindVisitor6 — the visitor that dispatches by
 * {@link TypeKind} and not only by interface.
 *
 * <p>Same idea as {@link ElementKindVisitor6}, on the side of types, and with two dispatches
 * instead of three. They are the two places where one model interface covers several different
 * forms:
 *
 * <ul>
 * <li>{@link PrimitiveType} covers the eight primitives. `visitPrimitive` opens them into
 *     `visitPrimitiveAsInt`, `visitPrimitiveAsBoolean` and so on — which is what is needed almost
 *     always, because a visitor that treats `int` and `double` alike is rare.</li>
 * <li>{@link NoType} covers the pseudo-types: `void`, a package's, a module's and `NONE`. Four
 *     things that are not types, for four different reasons, under a single interface.</li>
 * </ul>
 *
 * <p>The rest of the forms of type — array, declared, wildcard — have their own interface and need
 * no dispatch, so they are inherited as they are from {@link SimpleTypeVisitor6}.
 *
 * <p>The `AssertionError` at the end of each dispatch means the same as in `ElementKindVisitor6`: a
 * `PrimitiveType` whose kind is not primitive is a broken model, not a new language construct.
 *
 * <p>`visitNoTypeAsModule` is the only case that falls into `visitUnknown`: the `MODULE`
 * pseudo-type is from Java 9 and this visitor is from 6. {@link TypeKindVisitor9} passes it to the
 * funnel.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class TypeKindVisitor6<R, P> extends SimpleTypeVisitor6<R, P> {

    @Deprecated(since = "9")
    protected TypeKindVisitor6() {
        super(null);
    }

    @Deprecated(since = "9")
    protected TypeKindVisitor6(R defaultValue) {
        super(defaultValue);
    }

    // Chained `if`s and not a `switch`, for the same reason as in ElementKindVisitor6 (the frozen
    // javac does not lower a switch on an enum from the class path; #401/#538 are closed in the
    // source).

    public R visitPrimitive(PrimitiveType t, P p) {
        TypeKind k = t.getKind();
        if (k == TypeKind.BOOLEAN) {
            return this.visitPrimitiveAsBoolean(t, p);
        }
        if (k == TypeKind.BYTE) {
            return this.visitPrimitiveAsByte(t, p);
        }
        if (k == TypeKind.SHORT) {
            return this.visitPrimitiveAsShort(t, p);
        }
        if (k == TypeKind.INT) {
            return this.visitPrimitiveAsInt(t, p);
        }
        if (k == TypeKind.LONG) {
            return this.visitPrimitiveAsLong(t, p);
        }
        if (k == TypeKind.CHAR) {
            return this.visitPrimitiveAsChar(t, p);
        }
        if (k == TypeKind.FLOAT) {
            return this.visitPrimitiveAsFloat(t, p);
        }
        if (k == TypeKind.DOUBLE) {
            return this.visitPrimitiveAsDouble(t, p);
        }
        throw new AssertionError("Bad kind " + k + " for PrimitiveType" + t);
    }

    public R visitPrimitiveAsBoolean(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitPrimitiveAsByte(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitPrimitiveAsShort(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitPrimitiveAsInt(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitPrimitiveAsLong(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitPrimitiveAsChar(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitPrimitiveAsFloat(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitPrimitiveAsDouble(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitNoType(NoType t, P p) {
        TypeKind k = t.getKind();
        if (k == TypeKind.VOID) {
            return this.visitNoTypeAsVoid(t, p);
        }
        if (k == TypeKind.PACKAGE) {
            return this.visitNoTypeAsPackage(t, p);
        }
        if (k == TypeKind.MODULE) {
            return this.visitNoTypeAsModule(t, p);
        }
        if (k == TypeKind.NONE) {
            return this.visitNoTypeAsNone(t, p);
        }
        throw new AssertionError("Bad kind " + k + " for NoType" + t);
    }

    public R visitNoTypeAsVoid(NoType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitNoTypeAsPackage(NoType t, P p) {
        return this.defaultAction(t, p);
    }

    /** A module's pseudo-type is from Java 9: see the header. */
    public R visitNoTypeAsModule(NoType t, P p) {
        return this.visitUnknown(t, p);
    }

    public R visitNoTypeAsNone(NoType t, P p) {
        return this.defaultAction(t, p);
    }
}
